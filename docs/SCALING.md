# Scaling Guide

## Overview

The application is designed for horizontal scalability with stateless microservices. This guide covers scaling strategies for each component.

## Architecture Scalability

```
                    ┌──────────┐
                    │  Nginx   │ (Load Balancer)
                    │  HAProxy │
                    └────┬─────┘
                         │
          ┌──────────────┼──────────────┐
          │              │              │
    ┌─────▼─────┐  ┌────▼─────┐  ┌────▼─────┐
    │  API       │  │   API    │  │   API    │  ◄── Horizontally scalable
    │  Instance 1│  │Instance 2│  │Instance N│
    └─────┬─────┘  └────┬─────┘  └────┬─────┘
          │              │              │
          └──────────────┼──────────────┘
                         │
                    ┌────▼─────┐
                    │PostgreSQL│
                    │ Pool     │  ◄── Connection pooling / Read replicas
                    └──────────┘
```

## Stateless Services (Horizontally Scalable)

### 1. API Service

**Scaling strategy:** Stateless, scale horizontally behind a load balancer.

```bash
# Docker Compose scaling
docker compose up -d --scale api-service=5
```

**Considerations:**
- No session affinity required
- Connection pooling to PostgreSQL: `pool_size=20` per instance
- Each instance maintains its own Redis connection for pub/sub
- Recommended: 2-10 instances based on load

**Performance targets:**
- 1000+ concurrent requests per instance
- <100ms p95 response time
- 200+ requests/second per instance

**Configuration for high load:**

```python
# config.py overrides
database_url: with connection pooling
    pool_size=20
    max_overflow=10
    pool_pre_ping=True

# Increase pagination limits if needed
pagination_max_limit: 500
```

### 2. WebSocket Service

**Scaling strategy:** Horizontally scalable with sticky sessions.

**Challenge:** WebSocket connections are stateful (in-memory `ConnectionManager`). A user's connection exists on one instance only.

**Solution:** Use sticky sessions at the load balancer:

```nginx
# Nginx sticky sessions (ip_hash)
upstream ws {
    ip_hash;
    server websocket-service:8001;
    server websocket-service-2:8001;
}

# HAProxy sticky sessions
backend websocket
    balance source
    server ws1 websocket-service:8001 check
    server ws2 websocket-service-2:8001 check
```

**Future improvement:** Move connection state to Redis to allow truly stateless WebSocket servers (pure pub/sub model). This would require:
1. Store user→instance mapping in Redis
2. Route messages through Redis to the correct instance
3. Use Redis Streams for reliable message delivery

**Performance targets:**
- 10,000+ concurrent connections per instance
- <50ms message delivery latency
- 500+ messages/second per instance

### 3. Worker Service

**Scaling strategy:** Horizontally scalable by increasing concurrency and instances.

```bash
# Scale workers
docker compose up -d --scale worker-service=4

# Or increase concurrency per worker (in Dockerfile or command)
celery -A main.celery_app worker --concurrency=8 --loglevel=info
```

**Considerations:**
- Each worker can process multiple tasks concurrently (`--concurrency=N`)
- Tasks should be idempotent (duplicate execution is safe)
- Set `task_acks_late=True` for at-least-once delivery
- Set `worker_prefetch_multiplier=1` for fair task distribution

**Recommended:**
- 2-6 worker instances
- 4-8 concurrency per instance
- Separate queues for high/low priority tasks (future)

### 4. Search Service

**Scaling strategy:** Horizontally scalable, stateless.

```bash
docker compose up -d --scale search-service=2
```

**Considerations:**
- All instances connect to the same Elasticsearch cluster
- No inter-instance coordination needed
- If Elasticsearch is the bottleneck, scale ES before the service

## Stateful Services (Vertical or Clustered Scaling)

### 5. PostgreSQL

**Vertical scaling:** Increase CPU, RAM, and disk IOPS.

**Horizontal scaling:** Read replicas for read-heavy workloads.

```bash
# Read replica in docker-compose.yml
postgres-replica:
    image: postgres:16-alpine
    environment:
        POSTGRES_DB: chat_db
        POSTGRES_USER: postgres
        POSTGRES_PASSWORD: postgres
    command: |
        postgres -c wal_level=replica \
                 -c max_wal_senders=5 \
                 -c hot_standby=on
```

**Connection pooling:** Use PgBouncer or configure SQLAlchemy pool:

```python
# API service database.py (production)
engine = create_async_engine(
    settings.database_url,
    pool_size=20,          # Connections per instance
    max_overflow=10,       # Additional burst connections
    pool_pre_ping=True,    # Verify connections before use
    pool_recycle=3600,     # Recycle connections hourly
)
```

**Connection pool sizing formula:**
```
pool_size = (max_connections * 0.8) / (number_of_app_instances)
```

**Indexing strategy:**
- `database/indexes.sql` defines all indexes
- Monitor index usage with `pg_stat_user_indexes`
- Rebuild bloated indexes periodically: `REINDEX INDEX CONCURRENTLY`

### 6. Redis

**Current usage:** Single instance for:
- Pub/sub for real-time events (channel `room:*`, `presence`)
- Task broker (Celery, DB 1) and result backend (DB 2)
- Presence tracking (`presence:{user_id}` keys)
- Room member sets (`room:{room_id}:members`)

**Vertical scaling:** Increase `maxmemory` and use faster storage (SSD).

**Redis Cluster (for horizontal scaling):**

```yaml
# docker-compose.yml with Redis Cluster
redis-cluster:
    image: redis:7-alpine
    command: redis-cli --cluster create \
        127.0.0.1:7000 127.0.0.1:7001 127.0.0.1:7002 \
        127.0.0.1:7003 127.0.0.1:7004 127.0.0.1:7005 \
        --cluster-replicas 1
```

**Memory considerations:**
- Pub/sub messages are not stored and don't consume memory
- Presence keys are small (~50 bytes each), 1M users = ~50MB
- Celery result backend stores task results temporarily (enable TTL)
- Set `maxmemory-policy allkeys-lru` for cache-only usage

### 7. Elasticsearch

**Current setup:** Single node with 1 shard, 0 replicas.

**Scaling:** Add nodes and increase shard count.

```yaml
# For production, use multi-node cluster
elasticsearch-node1:
    image: elasticsearch:8.12.0
    environment:
        - cluster.name=chat-search
        - node.name=node-1
        - discovery.seed_hosts=es-node2,es-node3
        - cluster.initial_master_nodes=es-node1,es-node2,es-node3

elasticsearch-node2: ...
elasticsearch-node3: ...
```

**Index settings for production:**
```json
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 2,
    "refresh_interval": "30s",
    "translog.durability": "async"
  }
}
```

**Shard sizing guide:**
- Target 20-40 GB per shard
- For `messages` index: 1 shard per ~20M messages
- 3 shards with 2 replicas = 9 total shards for 3-node cluster

**Performance tuning:**
- Increase `indices.memory.index_buffer_size` for faster indexing
- Use `keyword` type for exact match fields (room_id, sender_id)
- Use `text` with custom analyzer for content search
- Monitor with Elasticsearch monitoring APIs

## Load Balancing

### Nginx

Current configuration in `nginx/nginx.conf` routes traffic to single instances. For multiple instances:

```nginx
upstream api {
    least_conn;
    server api-service:8000;
    server api-service-2:8000;
    server api-service-3:8000;
}

upstream ws {
    ip_hash;  # Sticky sessions for WebSocket
    server websocket-service:8001;
    server websocket-service-2:8001;
}
```

**Load balancing algorithms:**
- `least_conn` - Best for API service (distributes to least loaded)
- `ip_hash` - Required for WebSocket service (sticky sessions)
- `round_robin` - Default, adequate for stateless services

### Resource Limits

Set resource limits per container for predictable scaling:

```yaml
services:
  api-service:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 256M
```

## Caching Strategy

### Redis Cache

Current Redis usage is primarily for pub/sub and task queues. For API caching:

```
CACHEABLE ENDPOINTS:
  GET /users/me              - Short TTL (30s)
  GET /users/{id}            - Medium TTL (60s)
  GET /rooms                 - Medium TTL (30s)
  GET /rooms/{id}/members    - Medium TTL (60s)
  GET /search/messages       - Short TTL (15s)

NOT CACHEABLE (real-time data):
  GET /rooms/{id}/messages   - Real-time, not cached
  WebSocket events           - Real-time, not cached
  Auth endpoints             - Never cached
```

### Cache Invalidation

Publish cache invalidation events via Redis when data changes:

```python
# Example: invalidate user cache on profile update
await redis.publish("cache:invalidate", {
    "keys": ["users:me", f"users:{user_id}"],
})
```

## Auto-Scaling (Orchestrator)

For Kubernetes or Docker Swarm:

```yaml
# Docker Compose with Swarm mode
deploy:
  mode: replicated
  replicas: 3
  resources:
    limits:
      cpus: '1'
      memory: 512M
  update_config:
    parallelism: 1
    delay: 10s
  restart_policy:
    condition: on-failure
  placement:
    constraints:
      - node.role == worker
```

## Performance Testing

Before scaling in production, establish baselines:

```bash
# Load testing API service
ab -n 10000 -c 100 http://localhost:8000/health

# WebSocket connection test (using wsbench or custom script)
# Database connection test
pgbench -c 50 -t 1000 -U postgres chat_db
```

**Key metrics to monitor:**
- Request latency (p50, p95, p99)
- Error rate
- Database connection count
- Redis memory usage
- CPU/Memory per service
- Disk I/O
- Network throughput
