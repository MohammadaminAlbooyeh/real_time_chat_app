# Deployment Guide

## Production Architecture

The application is deployed using Docker Compose with production-optimized settings. All services run in containers on a single host or across a cluster via orchestrator (Kubernetes, Docker Swarm).

## Prerequisites

- Docker Engine 24+
- Docker Compose v2.20+
- Domain name with DNS pointing to server
- SSL certificate (Let's Encrypt or commercial)
- 8 GB+ RAM, 4+ CPU cores recommended

## Environment Configuration

### 1. Clone and Prepare

```bash
git clone https://github.com/yourorg/real-time-chat-app.git
cd real-time-chat-app
```

### 2. Configure Environment Variables

Each service requires a `.env` file. Copy the production template:

```bash
cp .env.production services/api-service/.env
cp .env.production services/websocket-service/.env
cp .env.production services/worker-service/.env
cp .env.production services/search-service/.env
```

### 3. Set Production Values

Edit each `.env` file with secure values:

**`.env.production` variables:**
```
APP_NAME=Real-Time Chat
APP_VERSION=1.0.0
DEBUG=false

# Database
DB_USER=chat_user
DB_PASSWORD=<generate-strong-password>
DB_HOST=postgres
DB_PORT=5432
DB_NAME=chat_db

DATABASE_URL=postgresql+asyncpg://${DB_USER}:${DB_PASSWORD}@${DB_HOST}:${DB_PORT}/${DB_NAME}
DATABASE_URL_SYNC=postgresql+psycopg2://${DB_USER}:${DB_PASSWORD}@${DB_HOST}:${DB_PORT}/${DB_NAME}

# Redis
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_URL=redis://${REDIS_HOST}:${REDIS_PORT}/0

# JWT
JWT_SECRET_KEY=<generate-64-char-random-hex>
JWT_ALGORITHM=HS256
JWT_ACCESS_TOKEN_EXPIRE_MINUTES=30
JWT_REFRESH_TOKEN_EXPIRE_DAYS=7

# CORS
ALLOWED_ORIGINS=["https://yourdomain.com"]

# Celery (worker service only)
CELERY_BROKER_URL=redis://redis:6379/1
CELERY_RESULT_BACKEND=redis://redis:6379/2

# Elasticsearch (search service only)
ELASTICSEARCH_URL=http://elasticsearch:9200

# Search Service URL (worker service only)
SEARCH_SERVICE_URL=http://search-service:8002
```

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

**Let's Encrypt with Certbot:**
```bash
sudo certbot certonly --standalone -d yourdomain.com
sudo cp /etc/letsencrypt/live/yourdomain.com/fullchain.pem nginx/ssl/cert.pem
sudo cp /etc/letsencrypt/live/yourdomain.com/privkey.pem nginx/ssl/key.pem
```

### 5. Nginx Configuration

Edit `nginx/nginx.conf` to set your domain name:

```nginx
server {
    listen 443 ssl;
    server_name yourdomain.com;
    # ...
}
```

## Deployment Steps

### Standard Deployment

```bash
# Build production images
docker compose build

# Start all services in detached mode
docker compose up -d

# Run database migrations
docker compose exec api-service alembic upgrade head

# Verify services are running
docker compose ps

# Check logs
docker compose logs -f
```

### Zero-Downtime Deployment

For production with zero downtime consideration:
1. Build new images
2. Scale up new containers alongside existing ones
3. Switch traffic via load balancer
4. Scale down old containers

## Database Migrations

Migrations are managed with Alembic:

```bash
# Apply all pending migrations
docker compose exec api-service alembic upgrade head

# Check current migration version
docker compose exec api-service alembic current

# View migration history
docker compose exec api-service alembic history

# Rollback one migration
docker compose exec api-service alembic downgrade -1

# Create a new migration (auto-detect changes)
docker compose exec api-service alembic revision --autogenerate -m "description"
```

## Health Checks

The Docker Compose configuration includes health checks for critical services:

| Service | Check | Interval |
|---------|-------|----------|
| PostgreSQL | `pg_isready -U postgres` | 5s |
| API Service | `GET /health` | 15s (via Prometheus) |
| WebSocket | Connection successful | At runtime |

**Manual health verification:**
```bash
# API health
curl https://yourdomain.com/health
curl https://yourdomain.com/health/db

# Search service health
curl https://yourdomain.com/api/v1/search/health

# Service discovery (Prometheus targets)
curl localhost:9090/api/v1/targets
```

## Scaling

### Horizontal Scaling

All stateless services can be scaled:

```bash
# Scale API service to 3 instances
docker compose up -d --scale api-service=3

# Scale WebSocket service
docker compose up -d --scale websocket-service=2

# Scale worker service
docker compose up -d --scale worker-service=4
```

**IMPORTANT:** WebSocket service maintains in-memory connection state. When scaling:
- Use sticky sessions (IP hash) at the load balancer
- Or move connection state to Redis (requires code changes)

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

The API service uses connection pooling via SQLAlchemy async engine. In production, configure:

```python
# In config.py (production overrides)
engine = create_async_engine(
    settings.database_url,
    pool_size=20,
    max_overflow=10,
    pool_pre_ping=True,
    pool_recycle=3600,
)
```

## Production Checklist

- [ ] `DEBUG=false` in all environment files
- [ ] Strong `JWT_SECRET_KEY` generated (64+ random chars)
- [ ] Strong database password generated
- [ ] SSL certificates configured and auto-renewal set up
- [ ] CORS `ALLOWED_ORIGINS` set to production domain only
- [ ] Database migrations applied
- [ ] Elasticsearch security enabled for production (xpack)
- [ ] Monitoring stack deployed (Prometheus, Grafana)
- [ ] Logging configured with appropriate retention
- [ ] Backups configured for PostgreSQL, Redis, Elasticsearch
- [ ] Health check endpoints monitored
- [ ] Rate limiting configured
- [ ] Firewall rules applied (only 80, 443, 22 open)
- [ ] Docker containers run as non-root user
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

Redis data is persisted to `redis_data` volume. For manual backup:

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
3. Apply any new migrations: `docker compose exec api-service alembic upgrade head`
4. Restart services: `docker compose up -d`
