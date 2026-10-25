# Scaling Guide

## Overview

The application is split into four independently deployable Spring Boot services (`api-service`, `websocket-service`, `worker-service`, `search-service`), each with its own `Dockerfile` and `docker-compose.yml` entry. This guide covers scaling strategies for each, grounded in what's actually configured in the repo today — there is no Kubernetes manifest or autoscaler in this repo; scaling here means Docker Compose replica counts and manual capacity planning.

## Stateless Services (Horizontally Scalable)

### 1. API Service

**Scaling strategy:** Stateless, scale horizontally behind a load balancer.

```bash
docker compose up -d --scale api-service=5
```

**Considerations:**
- No session affinity required — auth is JWT-based (`SecurityConfig.java` sets `SessionCreationPolicy.STATELESS`), so any instance can handle any request
- Database access is via Spring Data JPA/Hibernate, which by default pools connections with HikariCP; pool size is not explicitly overridden in `application.yml` today, so Hikari's defaults apply — tune `spring.datasource.hikari.maximum-pool-size` per instance if you scale out
- `RateLimitingFilter`'s request-count state is in-memory per instance (see SECURITY.md) — scaling out multiplies the effective per-IP limit, since there's no shared Redis-backed counter
- Each instance maintains its own Redis connection (`spring.data.redis.url`) for publishing WebSocket fan-out events (`RedisPublisherService`)

### 2. WebSocket Service

**Scaling strategy:** Horizontally scalable, but connections are stateful.

**Reality check:** `ConnectionManager` holds live `WebSocketSession` objects in memory per instance — a given user's connection lives on exactly one instance. To keep that user reachable from events published elsewhere, `websocket-service` relies on Redis pub/sub fan-out that is **already implemented and working today**:

- `RedisPublisher` publishes room/presence events to channels like `room:{roomId}` and `presence`
- `RoomEventSubscriber` subscribes to the `room:*` pattern on every instance and rebroadcasts matching events to that instance's own local sessions (`services/websocket-service/src/main/java/com/chatapp/websocket/redis/RoomEventSubscriber.java`)

This means you can run multiple `websocket-service` replicas today without sticky sessions for the *event propagation* problem — any instance that publishes an event to Redis will have it fan out to clients connected on every other instance. What you still need, if you put a load balancer in front of multiple replicas, is a way to route a given client's WebSocket connection consistently to *some* instance for the lifetime of that connection (standard WebSocket/L4 proxying handles this already since each TCP connection sticks to one backend for its duration — no special "sticky session" cookie logic is required, since there's no HTTP session state, just the live socket itself).

```bash
docker compose up -d --scale websocket-service=3
```

If fronting with Nginx, a plain `upstream` block with the default load-balancing algorithm is sufficient for distributing new connections; each already-established WebSocket connection naturally stays pinned to whichever backend accepted it.

### 3. Worker Service

**Scaling strategy:** `worker-service` runs Spring's `@Scheduled` cron jobs (`ScheduledTasks.java`), not a task queue — there is no Celery, no broker, and no per-task concurrency knob to tune here.

```bash
docker compose up -d --scale worker-service=2
```

**Important:** because the scheduled jobs (`cleanupExpiredSessions`, `archiveOldMessages`, `updateStaleStatuses`, `cleanupInactiveUsers`, `cleanupAbandonedRooms`) are plain `@Scheduled` methods with no distributed lock, running more than one `worker-service` replica means **every replica runs every cron job on its own schedule**, duplicating the work (each job should be idempotent, so duplicate runs are mostly harmless, but they're wasted work and can race on cleanup queries). If you need multiple replicas for availability, add a distributed lock (e.g. ShedLock with the Postgres or Redis backend) before scaling this service past 1 instance in production. The `task.scheduling.pool.size: 5` setting in `application.yml` controls how many scheduled jobs can run *concurrently within one instance*, not across instances.

### 4. Search Service

**Scaling strategy:** Horizontally scalable, stateless — it only talks to Elasticsearch.

```bash
docker compose up -d --scale search-service=2
```

All instances connect to the same Elasticsearch URL (`ELASTICSEARCH_URL`). If Elasticsearch becomes the bottleneck, scale/tune Elasticsearch itself before adding more `search-service` replicas.

## Stateful Services

### 5. PostgreSQL

Single instance today (`docker-compose.yml`'s `postgres` service, `postgres:16-alpine`), with schema loaded once via `docker-entrypoint-initdb.d` from `database/schema.sql` and `database/indexes.sql` (see INSTALLATION.md — these scripts only run on first container start against an empty data volume).

**Vertical scaling:** increase CPU/RAM/disk IOPS for the container.

**Horizontal scaling:** read replicas are not configured in this repo. If you need them, you'd add a replica service to `docker-compose.yml` with `wal_level=replica` and point read-heavy queries at it — this is a change you'd have to make, not something already wired up.

**Connection pooling:** each JPA-based service (`api-service`, `websocket-service`, `worker-service`) uses Hikari by default; if you run many instances against one Postgres, explicitly cap each instance's pool (`spring.datasource.hikari.maximum-pool-size`) so the sum across instances stays under Postgres's `max_connections`.

**Indexing:** `database/indexes.sql` defines the indexes actually applied; use `pg_stat_user_indexes` to check usage and `REINDEX INDEX CONCURRENTLY` to rebuild bloated ones.

### 6. Redis

**Current usage (real, grep-verified in the codebase):**
- Pub/sub for real-time events — `room:*` channels and `presence` (websocket-service's `RedisPublisher`/`RoomEventSubscriber`, api-service's `RedisPublisherService`)
- Presence tracking: `presence:{userId}` keys set by `PresenceTracker`
- Room membership sets: `room:{roomId}:members`

There is no Celery broker/result-backend usage of Redis — that was a Python-era concept and no longer applies.

**Vertical scaling:** increase `maxmemory`, use faster storage.

**Horizontal scaling (Redis Cluster):** not configured in this repo; a single `redis:7-alpine` container is used. Clustering would be an infrastructure addition if pub/sub or presence-key volume outgrows one node — most installs won't need this, since pub/sub messages aren't retained and presence keys are tiny (~50 bytes each).

### 7. Elasticsearch

**Current setup:** single node, `discovery.type=single-node`, security disabled (`xpack.security.enabled=false`) — fine for development, not for production as-is.

**Scaling for production:** move to a proper multi-node cluster with security enabled and real `number_of_shards`/`number_of_replicas` settings on the `messages` index, sized to your message volume. This repo does not ship a multi-node Elasticsearch config — treat any such example as a target to build, not something already running.

## Load Balancing

### Nginx

`nginx/nginx.conf` currently proxies to the single `api-service` and `websocket-service` containers defined in `docker-compose.yml`. To front multiple replicas, add an `upstream` block per service and point `proxy_pass` at it, e.g.:

```nginx
upstream api {
    least_conn;
    server api-service:8000;
}

upstream ws {
    server websocket-service:8001;
}
```

Because WebSocket events already fan out through Redis (see above), the `ws` upstream does not need `ip_hash`/sticky-session configuration for correctness — ordinary load distribution works, since each connection is pinned to whichever backend Nginx picked for it, for its own lifetime.

### Resource Limits

Docker Compose supports per-service resource limits if you need predictable scaling behavior:

```yaml
services:
  api-service:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 1G
```

## Performance Testing

`scripts/load-test.sh` exists in this repo for basic load testing — check it for the actual tool/invocation it uses before reaching for `ab`/`pgbench` ad hoc. Before scaling in production, establish baselines for:
- Request latency (p50/p95/p99) — note that without Micrometer/Actuator instrumented (see MONITORING.md), you'll need to measure this externally (e.g. with your load-testing tool's own timing) rather than from in-app metrics today
- Error rate
- Database connection count (`pg_stat_activity`)
- Redis memory usage (`redis-cli INFO memory`)
- CPU/memory per container (`docker stats`)
