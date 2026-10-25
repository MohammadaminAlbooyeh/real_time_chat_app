# Troubleshooting Guide

## Build / Startup Issues

### `mvn package` fails or hangs

```bash
# Build one service directly to see the real error
cd services/api-service
mvn clean package -DskipTests

# Common causes:
# - No internet access for the first `mvn dependency:go-offline` (Docker build step)
# - Wrong Java version — this project targets Java 21 (check `java -version`)
# - Stale ~/.m2 cache after a pom.xml change: `mvn clean install -U`
```

### Service container exits immediately after `docker compose up`

```bash
docker compose logs <service-name>

# Common causes:
# - Missing or misconfigured .env file for that service
#   (services/api-service/.env, services/websocket-service/.env, services/worker-service/.env, services/search-service/.env)
# - Spring Boot failed to start because it couldn't reach Postgres/Redis/Elasticsearch
#   (check depends_on / healthcheck ordering in docker-compose.yml)
# - A Bean Validation / @ConfigurationProperties binding error at startup — read the
#   full Spring Boot stack trace, it names the exact property that failed to bind
```

### "Port is already allocated"

```bash
lsof -i :8000   # macOS/Linux (api-service)
lsof -i :8001   # websocket-service
lsof -i :8002   # search-service
lsof -i :8003   # worker-service
netstat -ano | findstr :8000  # Windows

# Stop the conflicting process or change the port mapping in docker-compose.yml
```

## Database Connection Issues

### "Connection refused" connecting to PostgreSQL

```bash
docker compose ps postgres
docker compose exec postgres pg_isready -U postgres

# Start it if needed
docker compose up -d postgres

# Verify DATABASE_URL in each service's .env, e.g.:
# DATABASE_URL=jdbc:postgresql://postgres:5432/chat_db
```
Note the JDBC URL scheme (`jdbc:postgresql://...`) — this is a Java/Hibernate connection string, not the old `postgresql+asyncpg://` SQLAlchemy form.

### "FATAL: database 'chat_db' does not exist"

```bash
docker compose exec postgres psql -U postgres -c "CREATE DATABASE chat_db;"
```

### Schema is missing tables / "relation does not exist"

Schema is applied **once**, on first container start, via `docker-entrypoint-initdb.d`:

```yaml
# docker-compose.yml, postgres service
volumes:
  - ./database/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql
  - ./database/indexes.sql:/docker-entrypoint-initdb.d/02-indexes.sql
  - ./database/seeds:/docker-entrypoint-initdb.d/seeds
```

PostgreSQL's entrypoint only runs these scripts when the data directory is empty. If you change `database/schema.sql` later, it will **not** be re-applied to an existing volume. To pick up schema changes:

```bash
docker compose down -v        # drops the postgres_data volume — destroys all data
docker compose up -d postgres # re-runs the initdb scripts against a fresh volume
```

**There is no migration tool wired into this project** (no Flyway, no Liquibase). `services/api-service` sets `spring.jpa.hibernate.ddl-auto: validate`, meaning Hibernate will refuse to start if the schema doesn't match the JPA entities — it will never auto-generate or alter the schema for you. Any schema change must be made by hand in `database/schema.sql`/`database/indexes.sql` and re-applied via the volume-reset process above (or manually with `psql`/a migration tool you add yourself).

`scripts/migrate.sh` and the Makefile's `migrate` target are **stale leftovers from the Python/Alembic era and do not work** — `scripts/migrate.sh` still shells out to `alembic`, which no longer exists anywhere in this codebase, and `make migrate` runs `docker compose exec api-service java -jar app.jar -- migrate`, an argument the Spring Boot jar does not understand. Don't use either; use the volume-reset approach above.

### Connection Pool Exhaustion

**Symptoms:** slow queries, "too many clients", timeouts.

```bash
docker compose exec postgres psql -U postgres -c "SELECT count(*) FROM pg_stat_activity;"
docker compose exec postgres psql -U postgres -c "SHOW max_connections;"
```

Each JPA-backed service (`api-service`, `websocket-service`, `worker-service`) uses HikariCP by default. If you're running multiple replicas of a service, explicitly cap `spring.datasource.hikari.maximum-pool-size` per instance so the total across all instances stays under Postgres's `max_connections`.

## WebSocket Issues

### "WebSocket closed with code 4001"

**Causes (from `ChatWebSocketHandler.java`):**
- No `token` query parameter on the `/ws` connection
- Token invalid, expired, or not an access-type token
- `JWT_SECRET_KEY` differs between `api-service` (which issued the token) and `websocket-service` (which verifies it) — **this is the most common cause after a config change**; the two services each read their own `.env`, and they must match exactly

```bash
# Connect with a valid access token as a query param
# ws://host:8001/ws?token=<access_token>

docker compose logs websocket-service
```

### Frequent WebSocket disconnections

**Causes:** network instability, reverse-proxy idle timeout, server restart.

**Client-side reconnection:**
```javascript
function connectWebSocket(token) {
    const ws = new WebSocket(`ws://${host}/ws?token=${token}`);
    ws.onclose = (event) => {
        if (event.code === 4001) {
            refreshToken().then((newToken) => connectWebSocket(newToken));
        } else {
            setTimeout(() => connectWebSocket(token), 1000);
        }
    };
}
```

**Server-side checks:**
```bash
docker compose logs websocket-service

# Confirm Redis pub/sub fan-out is working
docker compose exec redis redis-cli PUBLISH "room:test-room-id" '{"type":"test"}'
docker compose logs websocket-service   # RoomEventSubscriber should log/forward it

# If fronting with Nginx, raise the proxied read timeout for long-lived connections
# (nginx/nginx.conf)
proxy_read_timeout 86400;
```

### "Unknown event type" error

The client sent a `type` the server doesn't recognize. `ChatWebSocketHandler`/`MessageHandler` only understand:
`message:send`, `typing:start`, `typing:stop`, `room:join`, `room:leave`. Anything else gets echoed back as an `error` event (see WEBSOCKET_EVENTS.md).

## Redis Issues

### "Connection refused" to Redis

```bash
docker compose ps redis
docker compose exec redis redis-cli ping   # should return PONG
docker compose up -d redis
```

### Events published by api-service never reach WebSocket clients

```bash
# Verify the channel names match on both ends:
# - api-service's RedisPublisherService publishes to "room:{roomId}"
# - websocket-service's RoomEventSubscriber subscribes to the pattern "room:*"

docker compose exec redis redis-cli SUBSCRIBE "room:*"      # terminal 1
docker compose exec redis redis-cli PUBLISH "room:<id>" '{"type":"test"}'  # terminal 2
```

### Redis memory full

```bash
docker compose exec redis redis-cli INFO memory
docker compose exec redis redis-cli CONFIG SET maxmemory-policy allkeys-lru
```

## Docker Issues

### "No such service" error

```bash
ls docker-compose.yml
docker compose build
```

### Volumes not mounting correctly

```bash
ls -la ./services/api-service   # should exist and have code
# Check Docker Desktop file-sharing settings (macOS/Windows) include the project dir
```

### "Cannot connect to the Docker daemon"

```bash
open -a Docker              # macOS
sudo systemctl start docker # Linux
sudo usermod -aG docker $USER && newgrp docker
```

## Permission Problems

### "Permission denied" accessing uploaded files

```bash
ls -la services/api-service/uploads
chmod 755 services/api-service/uploads
chown -R $USER:$USER services/api-service/uploads
```

### SSL certificate permission issues

```bash
chmod 600 nginx/ssl/key.pem
chmod 644 nginx/ssl/cert.pem
```

## Application Errors

### "Invalid or expired token"

**Causes:**
- Token expired (default 30 min access token lifetime)
- A refresh token was sent to an endpoint expecting an access token, or vice versa
- `JWT_SECRET_KEY` changed since the token was issued, or differs between `api-service` and `websocket-service`

```bash
# Get a new access token
curl -X POST http://localhost:8000/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "<your_refresh_token>"}'
```

### File upload failures

**Causes (verified against `UploadController.java`):**
- `Content-Type` not in the allowlist (`image/jpeg`, `image/png`, `image/gif`, `image/webp`, `application/pdf`, `text/plain`, `application/zip`, `application/x-rar-compressed`, `application/x-7z-compressed`, `application/msword`, `application/vnd.openxmlformats-officedocument.wordprocessingml.document`) → `400`
- File exceeds 50 MB (`52428800` bytes) → `400`
- Caller is not a member of the target room → `403`
- `messageId` supplied but that message doesn't belong to the room → `404`

### Search not returning results

**Causes:**
- Elasticsearch isn't running or isn't healthy
- The message hasn't been indexed yet (indexing to `search-service` happens out of the synchronous request path)
- Query too narrow

```bash
curl http://localhost:9200
curl http://localhost:9200/messages
curl -X GET "localhost:9200/messages/_search" -H 'Content-Type: application/json' -d'
{"query": {"match": {"content": "test"}}}'

docker compose logs worker-service | grep -i index
docker compose logs search-service
```

## Environment Specific

### Development: hot reload not working

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up
# Confirm docker-compose.dev.yml actually mounts source/volumes for your workflow —
# a plain `java -jar app.jar` container does not hot-reload Java changes the way
# a Python dev server would; you need either Spring Boot DevTools wired in, or to
# rebuild/restart the container after a code change.
```

### Production: services can't communicate

```bash
docker compose ps
docker network ls | grep chat-network
docker compose exec api-service ping websocket-service
```

## Getting Additional Help

- Check service logs: `docker compose logs -f <service-name>`
- Raise log verbosity: set `LOG_LEVEL=DEBUG` in the relevant service's `.env`
- Check monitoring dashboards (see MONITORING.md — note the real instrumentation gaps described there before assuming Grafana/Prometheus data exists)
- Open a GitHub issue with: steps to reproduce, relevant logs, environment details (OS, Docker version, Java version)
