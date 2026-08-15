# Installation Guide

## Prerequisites

Ensure the following are installed on your system:

- **Python** 3.12+ (`python3 --version`)
- **Node.js** 20+ (`node --version`)
- **npm** 10+ (`npm --version`)
- **Docker** 24+ (`docker --version`)
- **Docker Compose** v2.20+ (`docker compose version`)
- **Git** (`git --version`)

## Quick Start

The fastest way to get the entire application running is with Docker Compose:

```bash
# 1. Clone the repository
git clone https://github.com/MohammadaminAlbooyeh/real_time_chat_app.git
cd real_time_chat_app

# 2. Copy environment files
cp .env.example services/api-service/.env
cp .env.example services/websocket-service/.env
cp .env.example services/worker-service/.env
cp .env.example services/search-service/.env

# 3. Start all services (development mode with hot reload)
make dev

# 4. In another terminal, run database migrations
make migrate

# 5. Seed demo data (if available)
make seed
```

The application will be available at:
- **Web App:** http://localhost:3000
- **API:** http://localhost:8000
- **API Docs (Swagger):** http://localhost:8000/docs
- **API Docs (ReDoc):** http://localhost:8000/redoc
- **WebSocket:** ws://localhost:8001/ws

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

Create the database:

```bash
psql -U postgres -c "CREATE DATABASE chat_db;"
```

### 2. Elasticsearch Setup (Optional)

Required for full-text search functionality.

```bash
# macOS
brew install elasticsearch
brew services start elasticsearch

# Docker (easiest)
docker run -d --name elasticsearch \
  -p 9200:9200 -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  elasticsearch:8.12.0
```

### 3. Backend Services Setup

Each backend service has its own Python virtual environment.

**API Service:**
```bash
cd services/api-service
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt

# Edit .env if needed (DATABASE_URL, REDIS_URL, etc.)
# If using Docker for DB, keep defaults
```

**WebSocket Service:**
```bash
cd services/websocket-service
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

**Worker Service:**
```bash
cd services/worker-service
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

**Search Service:**
```bash
cd services/search-service
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

### 4. Frontend Setup

**Web Frontend:**
```bash
cd frontend/web
npm install
```

**Desktop (Electron):**
```bash
cd frontend/desktop
npm install
```

**Mobile (React Native):**
```bash
cd frontend/mobile
npm install
```

### 5. Database Migrations

```bash
cd services/api-service
source venv/bin/activate
alembic upgrade head
```

### 6. Run Services

Open separate terminals for each service:

**Terminal 1 - API Service:**
```bash
cd services/api-service
source venv/bin/activate
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

**Terminal 2 - WebSocket Service:**
```bash
cd services/websocket-service
source venv/bin/activate
uvicorn main:app --host 0.0.0.0 --port 8001 --reload
```

**Terminal 3 - Search Service:**
```bash
cd services/search-service
source venv/bin/activate
uvicorn main:app --host 0.0.0.0 --port 8002 --reload
```

**Terminal 4 - Worker Service:**
```bash
cd services/worker-service
source venv/bin/activate
celery -A main.celery_app worker --loglevel=info
```

**Terminal 5 - Celery Beat (scheduled tasks):**
```bash
cd services/worker-service
source venv/bin/activate
celery -A main.celery_app beat --loglevel=info
```

**Terminal 6 - Frontend:**
```bash
cd frontend/web
npm run dev
```

## Environment Files

### `.env.example` (base template)

```bash
APP_NAME=Real-Time Chat API
APP_VERSION=1.0.0
DEBUG=true

DATABASE_URL=postgresql+asyncpg://postgres:postgres@localhost:5432/chat_db
DATABASE_URL_SYNC=postgresql+psycopg2://postgres:postgres@localhost:5432/chat_db

REDIS_URL=redis://localhost:6379/0

JWT_SECRET_KEY=change-this-to-a-secure-random-secret
JWT_ALGORITHM=HS256
JWT_ACCESS_TOKEN_EXPIRE_MINUTES=30
JWT_REFRESH_TOKEN_EXPIRE_DAYS=7

ALLOWED_ORIGINS=["http://localhost:3000","http://localhost:5173"]
```

Copy to each service directory:
```bash
for dir in services/*/; do cp .env.example "$dir.env"; done
```

## Makefile Commands

| Command | Description |
|---------|-------------|
| `make dev` | Start all services with hot reload |
| `make build` | Build Docker images |
| `make up` | Start services in detached mode |
| `make down` | Stop all services |
| `make logs` | Follow service logs |
| `make migrate` | Run database migrations |
| `make test` | Run tests |
| `make shell` | Open bash in API service container |
| `make clean` | Remove containers and volumes |

## Verify Installation

Run the following checks to confirm everything is working:

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
# Expected: TokenResponse with user data

# 4. Search service (if Elasticsearch is running)
curl http://localhost:8002/api/v1/search/health
# Expected: {"status":"ok","service":"search-service"}

# 5. Web frontend
open http://localhost:3000
```

## Troubleshooting

See [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for common issues.

**Quick fixes:**
- Port conflicts: Stop other services on ports 8000-8002, 5432, 6379, 9200
- Docker permission denied: Add user to `docker` group: `sudo usermod -aG docker $USER`
- Database connection refused: Ensure PostgreSQL is running and the DATABASE_URL is correct
- Redis connection error: Ensure Redis is running and REDIS_URL is correct
