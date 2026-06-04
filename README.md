# Real-Time Chat Application

A production-grade real-time chat platform with microservices architecture, supporting web, desktop (Electron), and mobile (React Native) clients.

## Architecture

- **API Service** (FastAPI) - REST API for auth, rooms, messages, users, search, file upload
- **WebSocket Service** (FastAPI) - Real-time messaging, typing indicators, presence
- **Search Service** (FastAPI + Elasticsearch) - Full-text search across messages
- **Worker Service** (Celery) - Async task processing (notifications, maintenance, indexing)
- **PostgreSQL** - Primary database
- **Redis** - Pub/sub, caching, task broker
- **Nginx** - Reverse proxy with SSL

## Quick Start

```bash
make setup    # Copy env, build, start services
make migrate  # Run database migrations
make seed     # Seed demo data
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| API | 8000 | REST API endpoints |
| WebSocket | 8001 | Real-time WebSocket connection |
| Search | 8002 | Elasticsearch-backed search |
| PostgreSQL | 5432 | Database |
| Redis | 6379 | Cache & pub/sub |
| Nginx | 80/443 | Reverse proxy |

## Frontend Clients

- **Web**: React 18 + Vite + Redux Toolkit (`frontend/web/`)
- **Desktop**: Electron wrapper (`frontend/desktop/`)
- **Mobile**: React Native / Expo (`frontend/mobile/`)

## Tech Stack

- Python 3.12 / FastAPI / SQLAlchemy async
- React 18 / Redux Toolkit / Vite
- PostgreSQL 16 / Redis 7 / Elasticsearch 8
- Docker / Docker Compose
- Prometheus / Grafana / Jaeger / Sentry
- GitHub Actions CI/CD
