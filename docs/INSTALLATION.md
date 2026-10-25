# Installation Guide

## Prerequisites

Ensure the following are installed on your system:

- **Java** 21 (`java --version`) — all four backend services are Spring Boot apps on Java 21
- **Maven** 3.9+ (`mvn --version`) — or just use the Maven Wrapper if a service ships one
- **Node.js** 20+ (`node --version`)
- **npm** 10+ (`npm --version`)
- **Docker** 24+ (`docker --version`)
- **Docker Compose** v2.20+ (`docker compose version`)
- **Git** (`git --version`)

## Quick Start

The fastest way to get the entire application running is with Docker Compose — it builds all four Java services, starts Postgres/Redis/Elasticsearch, and loads the schema automatically:

```bash
# 1. Clone the repository
git clone https://github.com/MohammadaminAlbooyeh/real_time_chat_app.git
cd real_time_chat_app

# 2. Copy environment files
cp .env.example services/api-service/.env
cp .env.example services/websocket-service/.env
cp .env.example services/worker-service/.env
cp .env.example services/search-service/.env

# 3. Start all services (development mode, hot-reload via mounted sources)
make dev
```

There is **no `make migrate` or `make seed` step** — the Postgres container loads `database/schema.sql` and `database/indexes.sql` automatically on first startup (see "Database Setup" below), and there is no seed data script wired into the Makefile.

The application will be available at:
- **Web App:** http://localhost:3000
- **API:** http://localhost:8000
- **WebSocket:** ws://localhost:8001/ws
- **Search Service:** http://localhost:8002
- **Nginx (reverse proxy for API + WS):** http://localhost (80) / https://localhost (443, self-signed cert)

There is no Swagger/OpenAPI UI wired into api-service — see `docs/API_REFERENCE.md` for the documented endpoints instead.

## Manual Setup (Without Docker)

### 1. Database Setup

Install and start PostgreSQL 16 and Redis 7.

```bash
# macOS (Homebrew)
brew install postgresql@16 redis
brew services start postgresql@16
brew services start redis

# Ubuntu/Debian
sudo apt install postgresql-16 redis-server
sudo systemctl start postgresql
sudo systemctl start redis
```

Create the database and load the schema by hand (there is no migration tool — see "Schema Changes" below):

```bash
psql -U postgres -c "CREATE DATABASE chat_db;"
psql -U postgres -d chat_db -f database/schema.sql
psql -U postgres -d chat_db -f database/indexes.sql
```

### 2. Elasticsearch Setup (Optional)

Required for `search-service`'s full-text search.

```bash
# Docker (easiest)
docker run -d --name elasticsearch \
  -p 9200:9200 -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  elasticsearch:8.12.0
```

### 3. Backend Services Setup

Each backend service is an independent Maven project (Java 21, Spring Boot 3.5). Build each one from its own directory:

```bash
cd services/api-service
mvn clean install
```

```bash
cd services/websocket-service
mvn clean install
```

```bash
cd services/worker-service
mvn clean install
```

```bash
cd services/search-service
mvn clean install
```

Each `mvn clean install` compiles the service, runs its JUnit test suite, and produces an executable jar under `target/`.

### 4. Frontend Setup

**Web Frontend (React + Vite + Redux Toolkit):**
```bash
cd frontend/web
npm install
```

**Desktop (Electron + Vite, same React codebase shell):**
```bash
cd frontend/desktop
npm install
```

**Mobile (React Native via Expo):**
```bash
cd frontend/mobile
npm install
```

### 5. Schema Changes

There is no Alembic/Flyway/Liquibase migration framework in this project. The schema is a plain pair of SQL files:

- `database/schema.sql` — tables
- `database/indexes.sql` — indexes

In Docker, these are mounted into the Postgres container as `docker-entrypoint-initdb.d/01-schema.sql` and `02-indexes.sql` and only run automatically **the first time** the `postgres_data` volume is created (empty). If you change these files after the volume already exists, Postgres will not re-run them — either apply the SQL by hand against the running database, or drop the volume (`docker compose down -v`, dev environments only) to let it re-initialize from scratch. The Makefile's `migrate` target (`docker compose exec api-service java -jar app.jar -- migrate`) is a leftover from the old Python/Alembic setup and does nothing — the Spring Boot jars have no such subcommand. Each service's JPA layer is configured with `ddl-auto: validate`, meaning Hibernate checks the schema matches the entities but never auto-generates or alters tables.

### 6. Run Services

Open separate terminals for each service and run the Spring Boot app directly from the built jar (or `mvn spring-boot:run` for hot classpath use):

**Terminal 1 - API Service:**
```bash
cd services/api-service
mvn spring-boot:run
# or: java -jar target/api-service-1.0.0.jar
```

**Terminal 2 - WebSocket Service:**
```bash
cd services/websocket-service
mvn spring-boot:run
```

**Terminal 3 - Search Service:**
```bash
cd services/search-service
mvn spring-boot:run
```

**Terminal 4 - Worker Service:**
```bash
cd services/worker-service
mvn spring-boot:run
```

The worker service has no separate "beat" process — scheduled maintenance jobs (session cleanup, message archiving, stale-presence sweep, inactive-user cleanup, abandoned-room cleanup) run in-process via Spring's `@Scheduled` annotations in `ScheduledTasks.java`; there is no Celery broker/worker/beat split.

**Terminal 5 - Frontend:**
```bash
cd frontend/web
npm run dev
```

## Environment Variables

The real configuration keys (see each service's `src/main/resources/application.yml`) are:

```bash
APP_NAME=Real-Time Chat
APP_VERSION=1.0.0
PORT=8000                      # 8000 api, 8001 websocket, 8002 search, 8003 worker

DATABASE_URL=jdbc:postgresql://localhost:5432/chat_db
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres

REDIS_URL=redis://localhost:6379/0

JWT_SECRET_KEY=change-this-to-a-secure-random-secret
JWT_ALGORITHM=HS256
JWT_ACCESS_TOKEN_EXPIRE_MINUTES=30
JWT_REFRESH_TOKEN_EXPIRE_DAYS=7

ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

ELASTICSEARCH_URL=http://localhost:9200      # search-service
SEARCH_SERVICE_URL=http://search-service:8002 # worker-service, to call search-service
```

> Note: `services/api-service/.env` in this repo still contains stale Python-era keys (`DATABASE_URL=postgresql+asyncpg://...`, `DATABASE_URL_SYNC=...`) left over from before the Java migration. These are **not** read by Spring Boot — ignore them and use the JDBC-style `DATABASE_URL` shown above, matching `application.yml`.

Copy to each service directory:
```bash
for dir in services/*/; do cp .env.example "$dir.env"; done
```

## Makefile Commands

| Command | Description |
|---------|-------------|
| `make dev` | Start all services with `docker-compose.dev.yml` overrides (mounted source, hot rebuild) |
| `make build` | Build Docker images |
| `make up` | Start services in detached mode |
| `make down` | Stop all services |
| `make logs` | Follow service logs |
| `make test-api` / `test-search` / `test-worker` / `test-ws` | Run one service's `mvn test` |
| `make test-all` | Run `mvn test` for all four services |
| `make shell` | Open a shell in the api-service container |
| `make clean` | `docker compose down -v` + `docker system prune -f` |

(The Makefile also defines a `migrate` target; as noted above it is non-functional and should be ignored.)

## Verify Installation

```bash
# 1. API health check
curl http://localhost:8000/health
# Expected: {"status":"ok","service":"api-service"}

# 2. Database health
curl http://localhost:8000/health/db
# Expected: {"status":"ok","database":"connected"}

# 3. Register a test user
curl -X POST http://localhost:8000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"testpass123"}'
# Expected: TokenResponse with access/refresh tokens

# 4. Search service (if Elasticsearch is running)
curl http://localhost:8002/api/v1/search/health
# Expected: {"status":"ok","service":"search-service"}

# 5. Web frontend
open http://localhost:3000
```

## Troubleshooting

See [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for common issues.

**Quick fixes:**
- Port conflicts: Stop other services on ports 8000-8003, 5432, 6379, 9200
- Docker permission denied: Add user to `docker` group: `sudo usermod -aG docker $USER`
- Database connection refused: Ensure PostgreSQL is running and `DATABASE_URL` is a JDBC URL (`jdbc:postgresql://...`)
- Redis connection error: Ensure Redis is running and `REDIS_URL` is correct
