# Troubleshooting Guide

## Database Connection Issues

### "psql: could not connect to server: Connection refused"

**Causes:**
- PostgreSQL service is not running
- Wrong host/port in connection string
- Docker container not started

**Solutions:**

```bash
# Check if PostgreSQL is running locally
pg_isready -U postgres

# If using Docker, check container status
docker compose ps postgres

# Start PostgreSQL if needed
brew services start postgresql@16  # macOS
sudo systemctl start postgresql    # Linux
docker compose up -d postgres      # Docker

# Verify connection string in .env
# DATABASE_URL=postgresql+asyncpg://postgres:postgres@localhost:5432/chat_db
```

### "FATAL: database 'chat_db' does not exist"

```bash
# Create the database
psql -U postgres -c "CREATE DATABASE chat_db;"

# Or via Docker
docker compose exec postgres psql -U postgres -c "CREATE DATABASE chat_db;"
```

### "FATAL: password authentication failed"

```bash
# Verify credentials in .env match PostgreSQL configuration
# Default: postgres / postgres

# Reset password if needed
psql -U postgres -c "ALTER USER postgres PASSWORD 'postgres';"

# Check pg_hba.conf for authentication method
# For local dev, use: local all all trust
```

### "SSL connection required" error

If PostgreSQL is configured to require SSL but the app doesn't use it:

```python
# Add ?sslmode=disable to connection string
DATABASE_URL=postgresql+asyncpg://user:pass@host:5432/db?sslmode=disable
```

### Connection Pool Exhaustion

**Symptoms:** Slow queries, "too many clients" errors, timeouts.

**Solutions:**
```bash
# Check current connections
docker compose exec postgres psql -U postgres -c "SELECT count(*) FROM pg_stat_activity;"

# Check max connections
docker compose exec postgres psql -U postgres -c "SHOW max_connections;"

# Reduce pool size in database.py (for each API instance)
engine = create_async_engine(
    settings.database_url,
    pool_size=10,  # Reduced from default
    max_overflow=5,
)
```

## WebSocket Disconnections

### "WebSocket closed with code 4001"

**Causes:**
- Missing or invalid JWT token
- Expired access token

**Solutions:**
```python
# Verify the token is passed as a query parameter
# ws://host:8001/ws?token=<valid_access_token>

# Tokens expire after 30 minutes - refresh before connecting
# Check WebSocket service logs
docker compose logs websocket-service
```

### Frequent WebSocket Disconnections

**Causes:**
- Network instability
- Load balancer timeout
- Server restarting

**Solutions:**

**Client-side reconnection (recommended):**
```javascript
function connectWebSocket() {
    const ws = new WebSocket(`ws://${host}/ws?token=${token}`);

    ws.onclose = (event) => {
        if (event.code === 4001) {
            // Token issue - need to re-authenticate
            refreshToken().then(() => connectWebSocket());
        } else {
            // Transient error - reconnect with backoff
            setTimeout(connectWebSocket, 1000);
        }
    };

    ws.onerror = () => ws.close();
}
```

**Server-side checks:**
```bash
# Check WebSocket service logs for errors
docker compose logs websocket-service

# Verify Redis pub/sub is working
docker compose exec redis redis-cli PUBLISH test "hello"
docker compose logs websocket-service  # Should see forwarded message

# Increase Nginx proxy timeout (nginx/nginx.conf)
proxy_read_timeout 86400;  # 24 hours
```

### "Unknown event type" Error

**Cause:** Client sent an event type that the server doesn't recognize.

**Solution:** Check the client is sending valid event types:
- `message:send`, `typing:start`, `typing:stop`, `room:join`, `room:leave`

## Redis Connection Errors

### "Connection refused" to Redis

```bash
# Check Redis is running
docker compose ps redis
redis-cli ping  # Should return PONG

# Start Redis if needed
docker compose up -d redis
```

### "MOVED" redirection errors (Redis Cluster)

If using Redis Cluster mode but connecting to a single node:

```python
# Use RedisCluster client instead of Redis
from redis.cluster import RedisCluster
redis = RedisCluster.from_url(settings.redis_url)
```

### Redis Pub/Sub Not Working

**Symptoms:** Messages sent via API not appearing in WebSocket.

**Debugging:**
```bash
# Test Redis pub/sub manually
# Terminal 1: Subscribe
docker compose exec redis redis-cli SUBSCRIBE "room:*"

# Terminal 2: Publish
docker compose exec redis redis-cli PUBLISH "room:test-room-id" '{"type":"test"}'

# Check WebSocket service is subscribed
docker compose logs websocket-service | grep "psubscribe"

# Verify channel pattern matching
# WebSocket uses: psubscribe("room:*")
# API publishes to: f"room:{room_id}"
```

### Redis Memory Full

```bash
# Check memory usage
docker compose exec redis redis-cli INFO memory

# Set maxmemory policy (default is noeviction - change to allkeys-lru)
docker compose exec redis redis-cli CONFIG SET maxmemory-policy allkeys-lru

# Persist to config file if using custom redis.conf
```

## Docker Issues

### "Port is already allocated"

```bash
# Find what's using the port
lsof -i :8000   # macOS/Linux
netstat -ano | findstr :8000  # Windows

# Stop the conflicting service or change the port mapping
```

### "No such service" error

```bash
# Ensure docker-compose.yml is in the current directory
ls docker-compose.yml

# Rebuild if services were added
docker compose build
```

### Container exits immediately

```bash
# Check container logs
docker compose logs <service-name>

# Common causes:
# - Missing .env file
# - Wrong working directory
# - Import error in Python code
# - Database connection failure at startup
```

### Volumes not mounting correctly

```bash
# Verify volume paths exist
ls -la ./services/api-service  # Should exist and have code

# Check Docker Desktop file sharing settings (macOS/Windows)
# Ensure the project directory is shared
```

### "Cannot connect to the Docker daemon"

```bash
# Start Docker Desktop (macOS)
open -a Docker

# Start Docker service (Linux)
sudo systemctl start docker

# Add user to docker group (Linux)
sudo usermod -aG docker $USER
# Log out and back in
```

## Permission Problems

### "Permission denied" when accessing files

```bash
# Check file permissions
ls -la services/api-service/uploads

# Fix permissions
chmod 755 services/api-service/uploads
chown -R $USER:$USER services/api-service/uploads
```

### Cannot execute docker commands without sudo

```bash
# Add user to docker group (Linux)
sudo usermod -aG docker $USER
newgrp docker  # Apply without logout
```

### SSL Certificate Permission Issues

```bash
# SSL key must have restricted permissions
chmod 600 nginx/ssl/key.pem
chmod 644 nginx/ssl/cert.pem
```

## Application Errors

### "Maximum rooms limit reached"

**Cause:** User has reached the 100-room limit.

**Solution:** Leave unused rooms or increase `max_rooms_per_user` in config.

### "Email or username already taken"

**Cause:** Duplicate registration attempt.

**Solution:** Use a different email/username or log in if you already have an account.

### "Invalid or expired token"

**Causes:**
- Token has expired (30 min lifetime)
- Wrong token type (used refresh token instead of access token)
- Token signed with different secret

**Solutions:**
```python
# Refresh the token
POST /api/v1/auth/refresh
Body: {"refresh_token": "<your_refresh_token>"}

# Ensure using access_token (not refresh_token) for API calls
# Check JWT_SECRET_KEY hasn't changed between token issuance and verification
```

### File Upload Failures

**Causes:**
- File type not in allowed list
- File exceeds 50 MB limit
- User is not a member of the target room

**Solutions:**
```python
# Check allowed types in upload.py
ALLOWED_CONTENT_TYPES = {
    "image/jpeg", "image/png", "image/gif", "image/webp",
    "application/pdf", "text/plain",
    "application/zip", "application/x-rar-compressed",
    "application/x-7z-compressed", "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
}

# Verify room membership
# Check file size: must be < 50 MB (52428800 bytes)
```

### Search Not Returning Results

**Causes:**
- Elasticsearch not running
- Messages not indexed yet (async indexing)
- Query too specific

**Debugging:**
```bash
# Check Elasticsearch is running
curl http://localhost:9200

# Check index exists
curl http://localhost:9200/messages

# Search directly in Elasticsearch
curl -X GET "localhost:9200/messages/_search" -H 'Content-Type: application/json' -d'
{
  "query": {"match": {"content": "test"}}
}'

# Verify worker is processing indexing tasks
docker compose logs worker-service | grep index
```

## Environment Specific

### Development: Hot Reload Not Working

```bash
# Ensure running with docker-compose.dev.yml
docker compose -f docker-compose.yml -f docker-compose.dev.yml up

# Check volume mounts are correct
# The dev compose mounts local source into containers
```

### Production: Services Can't Communicate

```bash
# Verify all services are on the same network
docker compose ps

# Check network exists
docker network ls | grep chat-network

# Test connectivity between services
docker compose exec api-service ping websocket-service
```

### Migration Errors

```bash
# Check current migration state
docker compose exec api-service alembic current

# View migration history
docker compose exec api-service alembic history

# If migration fails, check for:
# - Schema conflicts (table already exists)
# - Data type mismatches
# - Foreign key constraint violations

# Rollback if needed
docker compose exec api-service alembic downgrade -1
```

## Getting Additional Help

- Check service logs: `docker compose logs -f <service-name>`
- Enable debug mode: Set `DEBUG=true` in `.env`
- Check monitoring dashboards (see MONITORING.md)
- Open a GitHub issue with:
  - Steps to reproduce
  - Relevant logs
  - Environment details (OS, Docker version, etc.)
