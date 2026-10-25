# Documentation Index

## Getting Started

| Document | Description |
|----------|-------------|
| [INSTALLATION.md](INSTALLATION.md) | Step-by-step local development setup |
| [DEPLOYMENT.md](DEPLOYMENT.md) | Production deployment with Docker Compose |

## API Reference

| Document | Description |
|----------|-------------|
| [API_REFERENCE.md](API_REFERENCE.md) | Complete REST API reference with all endpoints, request/response schemas |
| [WEBSOCKET_EVENTS.md](WEBSOCKET_EVENTS.md) | WebSocket protocol: events, payloads, auth flow, reconnection |

## Architecture & Design

| Document | Description |
|----------|-------------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | System architecture, microservices, data flow, communication patterns |
| [SCALING.md](SCALING.md) | Horizontal scaling, database pooling, Redis, load balancing |

## Operations

| Document | Description |
|----------|-------------|
| [MONITORING.md](MONITORING.md) | Prometheus, Grafana, Jaeger setup and usage |
| [TROUBLESHOOTING.md](TROUBLESHOOTING.md) | Common issues and solutions |

## Security & Development

| Document | Description |
|----------|-------------|
| [SECURITY.md](SECURITY.md) | Authentication, authorization, data protection, security best practices |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Code style, PR workflow, testing, development guidelines |

---

## Quick Links

- **Root README:** [`README.md`](../README.md) - Project overview and quick start

## Service Summary

| Service | Tech | Port | Description |
|---------|------|------|-------------|
| API Service | Spring Boot (Spring MVC + Spring Data JPA) | 8000 | REST API for all CRUD operations |
| WebSocket Service | Spring Boot (`TextWebSocketHandler`) + Redis | 8001 | Real-time messaging and presence |
| Search Service | Spring Boot + Elasticsearch client | 8002 | Full-text message search |
| Worker Service | Spring Boot (`@Scheduled` cron jobs) | 8003 | Scheduled maintenance, cleanup, notifications |
| PostgreSQL 16 | SQL | 5432 | Primary data store |
| Redis 7 | In-memory | 6379 | Cache, pub/sub |
| Elasticsearch 8.12 | Search engine | 9200 | Full-text search index |
| Nginx | Reverse proxy | 80/443 | Routes to api-service and websocket-service, SSL termination |

All four backend services are independent Java 21 / Spring Boot 3.5 Maven projects under `services/`; there is no shared Python package or FastAPI/Celery component anymore.

## Frontend Clients

| Client | Tech | Directory |
|--------|------|-----------|
| Web | React 18 + Vite + Redux Toolkit | `frontend/web/` |
| Desktop | Electron + React + Vite | `frontend/desktop/` |
| Mobile | React Native + Expo | `frontend/mobile/` |
