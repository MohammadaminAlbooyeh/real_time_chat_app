# Deployment Guide

## Production Architecture

The application is deployed using Docker Compose; `docker-compose.yml` defines nginx, the four Java services (`api-service`, `websocket-service`, `worker-service`, `search-service`), Postgres, Redis, Elasticsearch, the web frontend, and the monitoring stack (Prometheus, Grafana, Jaeger). All services run as containers on a single host (or hosts that share the compose network). There are no Kubernetes manifests or CI/CD pipelines in this repository — treat any claim of Kubernetes/orchestrator deployment as aspirational, not implemented, unless you add that tooling yourself.

## Prerequisites

- Docker Engine 24+
- Docker Compose v2.20+
- Domain name with DNS pointing to server (if deploying publicly)
- SSL certificate (Let's Encrypt or commercial) for a real deployment — the bundled `nginx/Dockerfile` generates a self-signed cert at build time, which is fine for local HTTPS testing only
- 8 GB+ RAM, 4+ CPU cores recommended (JVMs for four services + Postgres + Redis + Elasticsearch)

## Environment Configuration

### 1. Clone and Prepare

```bash
git clone https://github.com/MohammadaminAlbooyeh/real_time_chat_app.git
cd real_time_chat_app
```

### 2. Configure Environment Variables

Each service has its own `.env` file, referenced by `docker-compose.yml`'s `env_file:`. Start from the production template:

```bash
cp .env.production services/api-service/.env
cp .env.production services/websocket-service/.env
cp .env.production services/worker-service/.env
cp .env.production services/search-service/.env
```

### 3. Set Production Values

The real keys consumed by each service's `application.yml` are:

```
APP_NAME=Real-Time Chat
APP_VERSION=1.0.0

# Database (JDBC — api-service, websocket-service, worker-service)
DATABASE_URL=jdbc:postgresql://postgres:5432/chat_db
DATABASE_USERNAME=<db user>
DATABASE_PASSWORD=<generate-strong-password>

# Redis
REDIS_URL=redis://redis:6379/0

# JWT (api-service, websocket-service)
JWT_SECRET_KEY=<generate-64-char-random-hex>
JWT_ALGORITHM=HS256
JWT_ACCESS_TOKEN_EXPIRE_MINUTES=30
JWT_REFRESH_TOKEN_EXPIRE_DAYS=7

# CORS (api-service)
ALLOWED_ORIGINS=https://yourdomain.com

# Elasticsearch (search-service)
ELASTICSEARCH_URL=http://elasticsearch:9200
ELASTICSEARCH_INDEX=messages

# Search service URL (worker-service calls it; api-service does its own search via MessageService)
SEARCH_SERVICE_URL=http://search-service:8002
```

There is no Celery broker/result backend anymore — `worker-service` runs its scheduled jobs in-process with Spring's `@Scheduled`, so `CELERY_BROKER_URL`/`CELERY_RESULT_BACKEND` variables from the old Python setup are obsolete and unused.

**Generate secure secrets:**
```bash
# JWT secret key
openssl rand -hex 32

# Database password
openssl rand -base64 24
```

### 4. SSL Configuration

Place your SSL certificate and key files in `nginx/ssl/`:

```
nginx/ssl/cert.pem   # Full certificate chain
nginx/ssl/key.pem    # Private key (secure permissions: 600)
```

By default, `nginx/Dockerfile` generates a self-signed cert at image build time via `openssl req`. For a real deployment, replace it with a trusted certificate before (or instead of) that step.

**Let's Encrypt with Certbot:**
```bash
sudo certbot certonly --standalone -d yourdomain.com
sudo cp /etc/letsencrypt/live/yourdomain.com/fullchain.pem nginx/ssl/cert.pem
sudo cp /etc/letsencrypt/live/yourdomain.com/privkey.pem nginx/ssl/key.pem
```

### 5. Nginx Configuration

`nginx/nginx.conf` currently proxies all traffic on `/` to `api-service:8000` and everything under `/ws` to `websocket-service:8001`, both on port 80 and on 443 (TLS). Edit `server_name localhost;` in both `server` blocks to your real domain before deploying publicly.

## Deployment Steps

### Standard Deployment

```bash
# Build production images
docker compose build

# Start all services in detached mode
docker compose up -d

# Verify services are running
docker compose ps

# Check logs
docker compose logs -f
```

There is no migration step here — the schema is already applied the first time the `postgres_data` volume is created (see "Schema Changes" below). If you are deploying against an existing database, you do not need, and there is no command, to "run migrations."

### Zero-Downtime Deployment

For production with zero downtime consideration:
1. Build new images
2. Scale up new containers alongside existing ones
3. Switch traffic via load balancer
4. Scale down old containers

There is no automated blue/green or rolling-deploy tooling in this repo (no Kubernetes, no Swarm config) — this is a manual process you'd script yourself on top of plain `docker compose`.

## Schema Changes (no migration tool)

This project has **no migration framework** (no Alembic, no Flyway, no Liquibase). The schema lives in two plain SQL files:

- `database/schema.sql`
- `database/indexes.sql`

`docker-compose.yml` mounts them into the Postgres container as `docker-entrypoint-initdb.d/01-schema.sql` and `02-indexes.sql`, alongside `database/seeds/`. Postgres only executes files under `docker-entrypoint-initdb.d` **once**, when the data directory (the `postgres_data` volume) is first initialized. Consequences for deployment:

- A brand-new environment: just `docker compose up -d` — the schema loads automatically.
- An existing environment with schema changes: you must apply the new SQL manually, e.g.:
  ```bash
  docker compose exec -T postgres psql -U postgres -d chat_db < database/schema.sql
  ```
  (idempotent `ALTER`/`CREATE IF NOT EXISTS` statements recommended), or accept downtime and recreate the volume in a disposable environment.
- Each service's JPA layer runs with `hibernate.ddl-auto: validate` — it will refuse to start if the live schema doesn't match the entity mappings, but it will never create or alter tables for you.

Do not run `docker compose exec api-service alembic ...` — that tool was removed with the Python stack and the command no longer exists in these containers.

## Health Checks

The Docker Compose configuration includes a health check for Postgres only (`pg_isready`); the other services have no declared Docker healthcheck, so `depends_on` only waits for container start, not application readiness.

| Service | Check |
|---------|-------|
| PostgreSQL | `pg_isready -U postgres` (Compose healthcheck, 5s interval) |
| API Service | `GET /health`, `GET /health/db` |
| Search Service | `GET /api/v1/search/health` |
| WebSocket | Successful `ws://.../ws?token=...` handshake |

**Manual health verification:**
```bash
curl https://yourdomain.com/health
curl https://yourdomain.com/health/db
curl https://yourdomain.com/api/v1/search/health

# Service discovery (Prometheus targets)
curl localhost:9090/api/v1/targets
```

## Scaling

### Horizontal Scaling

All backend services are stateless HTTP/JPA apps except `websocket-service`, which keeps live connections in memory, so they can in principle be scaled:

```bash
docker compose up -d --scale api-service=3
docker compose up -d --scale websocket-service=2
docker compose up -d --scale worker-service=4
```

**IMPORTANT:** `websocket-service` keeps each client's `WebSocketSession` in an in-memory `ConnectionManager`. Cross-instance fan-out for room/presence events already works via Redis pub/sub (`RedisPublisher` / `RoomEventSubscriber`), so a message sent on one instance reaches clients connected to another instance. However, a specific client's connection still only exists on the instance it's attached to — you need sticky sessions (IP hash or similar) at the load balancer so reconnects/requests for that session land back on the right instance; there's no shared session store.

Also be aware: scaling `worker-service` to more than one instance will run the same `@Scheduled` cron jobs on every replica (there's no leader election or distributed scheduler lock), which can cause duplicate cleanup/archiving runs. Keep `worker-service` at a single replica unless you add that coordination yourself.

### Vertical Scaling

Adjust resource limits in `docker-compose.yml`:

```yaml
services:
  api-service:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
```

### Database Connection Pooling

Connection pooling is handled by Spring Boot's default HikariCP pool (configured implicitly via `spring.datasource`). Tune it per service in `application.yml` if needed, e.g.:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```

## Production Checklist

- [ ] Strong `JWT_SECRET_KEY` generated (64+ random chars)
- [ ] Strong database password generated
- [ ] SSL certificates configured and auto-renewal set up
- [ ] CORS `ALLOWED_ORIGINS` set to production domain only
- [ ] Schema applied (`database/schema.sql` + `indexes.sql`) on a fresh volume, or manually re-applied after a change
- [ ] Elasticsearch security enabled for production (xpack is disabled in the default compose file)
- [ ] Monitoring stack deployed (Prometheus, Grafana, Jaeger)
- [ ] Logging configured with appropriate retention
- [ ] Backups configured for PostgreSQL, Redis, Elasticsearch
- [ ] Health check endpoints monitored
- [ ] Firewall rules applied (only 80, 443, 22 open)
- [ ] Secrets stored in environment variables (not in code)

## Backup and Restore

### PostgreSQL Backup

```bash
# Backup
docker compose exec -T postgres pg_dump -U postgres chat_db > backup_$(date +%Y%m%d).sql

# Restore
cat backup.sql | docker compose exec -T postgres psql -U postgres chat_db
```

### Redis Backup

Redis data is persisted to the `redis_data` volume. For manual backup:

```bash
docker compose exec redis redis-cli SAVE
# The dump.rdb file is in the redis_data volume
```

### Elasticsearch Backup

```bash
# Snapshot to a registered repository
curl -X PUT "localhost:9200/_snapshot/backup-repo"
curl -X PUT "localhost:9200/_snapshot/backup-repo/snapshot-1"
```

## Monitoring

See [MONITORING.md](MONITORING.md) for detailed monitoring setup.

After deployment, verify monitoring is operational:
- Grafana: `https://yourdomain.com:3001` (default credentials: admin/admin)
- Prometheus: `https://yourdomain.com:9090`
- Jaeger: `https://yourdomain.com:16686`

## Upgrading

1. Pull latest changes: `git pull`
2. Rebuild images: `docker compose build`
3. If `database/schema.sql`/`indexes.sql` changed, apply the new SQL by hand against the running database (see "Schema Changes" above) — there is no automatic migration step
4. Restart services: `docker compose up -d`
