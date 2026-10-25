# Architecture

> For the full, up-to-date architecture writeup (component detail, data flow, security architecture, deployment topology) see **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**. This file is a shorter top-level summary.

## System Overview

The Real-Time Chat Application is built on a microservices architecture with four independent Java/Spring Boot backend services, three frontend clients, and supporting infrastructure. There is no shared Python library anymore — the old `shared/` package was removed as part of the migration to Java.

```
                                    ┌─────────────┐
                                    │   Clients    │
                                    │ (Web/Desktop │
                                    │  /Mobile)    │
                                    └──────┬──────┘
                                           │
                                    ┌──────▼──────┐
                                    │    Nginx     │
                                    │   :80/443    │
                                    │  (Reverse    │
                                    │   Proxy)     │
                                    └──────┬──────┘
                                           │
                        ┌──────────────────┼──────────────────┐
                        │                                     │
                  ┌─────▼─────┐                        ┌──────▼──────┐
                  │   API      │                        │  WebSocket   │
                  │  Service   │                        │  Service     │
                  │  :8000     │                        │  :8001       │
                  │ (Spring    │                        │ (Spring Boot,│
                  │  Boot)     │                        │  WebSocket)  │
                  └─────┬─────┘                        └──────┬──────┘
                        │                                     │
                  ┌─────▼──────────────────▼──────┐
                  │           Redis                │
                  │   :6379 - Pub/Sub & Cache      │
                  └─────┬──────────────────┬──────┘
                        │                  │
                  ┌─────▼───────┐   ┌─────▼──────┐
                  │  PostgreSQL  │   │  Worker     │
                  │  :5432       │   │  Service    │
                  │  (Primary    │   │  :8003      │
                  │   Store)     │   │ (Spring     │
                  └─────────────┘    │  @Scheduled)│
                                     └─────┬──────┘
                                           │ HTTP (indexing calls)
                                     ┌─────▼──────┐
                                     │   Search    │
                                     │   Service   │
                                     │   :8002     │
                                     │ (Spring Boot│
                                     │  + ES)      │
                                     └─────┬──────┘
                                           │
                                     ┌─────▼──────┐
                                     │Elasticsearch│
                                     │   :9200     │
                                     └────────────┘

                                    ┌────────────┐
                                    │ Frontend   │
                                    │ Web :3000  │
                                    │ (React)    │
                                    └────────────┘
```

## Microservices Breakdown

### 1. API Service (Spring Boot)

**Port:** 8000

The primary REST API gateway. Handles all CRUD operations.

**Responsibilities:**
- User authentication and registration (JWT)
- User profile management
- Room creation, membership, and administration
- Message sending, editing, deletion
- Message reactions management
- Friend relationships and friend requests
- Message search over the primary database (delegated from `SearchController`)
- File uploads with content-type/size validation
- Notification listing and management
- Health checks

**Key Technologies:** Spring Boot 3.5, Spring MVC, Spring Data JPA, Spring Security, `jjwt`, BCrypt (`spring-security-crypto`)

**Endpoints:** See [docs/API_REFERENCE.md](docs/API_REFERENCE.md) for the complete reference.

### 2. WebSocket Service (Spring Boot)

**Port:** 8001

Handles all real-time bidirectional communication.

**Responsibilities:**
- WebSocket connection management (connect/disconnect) via a plain `TextWebSocketHandler`
- Real-time message delivery (writes directly to Postgres via its own repository, independent of api-service)
- Typing indicators broadcast
- Room join/leave tracking
- Presence tracking (online/offline status) via Redis
- Redis pub/sub event forwarding between service instances

**Key Technologies:** `spring-boot-starter-websocket`, Spring Data Redis, `jjwt`

**Connection Flow:**
1. Client connects to `ws://host:8001/ws?token=<jwt_token>`
2. Server verifies the JWT
3. Connection registered in `ConnectionManager`
4. Client sends `room:join` to subscribe to room events
5. Messages are broadcast to all room members currently connected to that instance
6. Redis pub/sub relays room/presence events to other websocket-service instances

### 3. Search Service (Spring Boot)

**Port:** 8002

Dedicated full-text search service backed by Elasticsearch.

**Responsibilities:**
- Index messages into Elasticsearch
- Full-text search across indexed messages
- Delete messages from the index
- Health monitoring

**Key Technologies:** Spring Boot, Elasticsearch Java client

### 4. Worker Service (Spring Boot)

**Port:** 8003 (background process; no end-user HTTP API)

Runs scheduled maintenance jobs in-process via Spring's `@Scheduled` cron annotations. There is no Celery, no message broker for task dispatch, and no separate "beat" process — everything is one Spring Boot app.

**Responsibilities:**
- **Maintenance:** Clean up expired sessions, archive old messages
- **User Maintenance:** Update stale online statuses, clean up inactive users
- **Room Maintenance:** Clean up abandoned rooms
- **Search Indexing:** Call `search-service` over HTTP (`RestTemplate`) to keep the Elasticsearch index in sync

**Scheduled Tasks (`@Scheduled` cron, `task/ScheduledTasks`):**
| Task | Schedule |
|------|----------|
| `cleanupExpiredSessions` | Daily 3:00 AM |
| `archiveOldMessages` | Daily 4:00 AM |
| `updateStaleStatuses` | Every 5 minutes |
| `cleanupInactiveUsers` | Daily 5:00 AM |
| `cleanupAbandonedRooms` | Daily 6:00 AM |

**Key Technologies:** Spring Boot, Spring `@Scheduled`, Spring Data JPA, `RestTemplate`

## Data Flow Patterns

### REST Flow (API Service)
```
Client → Nginx → API Service → PostgreSQL
                            ↘ Redis (events)
```

### WebSocket Flow
```
Client WebSocket → Nginx → WebSocket Service → Redis (pub/sub)
                                              → PostgreSQL (direct writes)
```

### Scheduled Job Flow (no task queue)
```
Worker Service (@Scheduled timer fires) → PostgreSQL / Search Service HTTP call
```
There is no broker-based task dispatch — jobs run on a timer inside the worker process itself.

### Search Flow
```
Client → API Service (delegates to MessageService against PostgreSQL)
       → Search Service → Elasticsearch (separate internal index, kept in sync by Worker Service)
```

## Communication Patterns

### REST API
- Synchronous HTTP/HTTPS via Spring MVC
- JSON request/response bodies (camelCase)
- JWT Bearer token authentication
- CORS configured for web clients (`WebConfig`)

### WebSocket
- Persistent bidirectional TCP connections
- JSON-encoded message frames
- Token-based authentication on connection (`?token=` query param)
- Room-scoped message broadcasting
- Typing indicator events
- Presence events (online/offline)

### Redis Pub/Sub
- WebSocket service publishes room events to `room:{room_id}` channels for cross-instance fan-out
- WebSocket service subscribes to the `room:*` pattern
- Also used for a `presence` channel for global status updates

## Database Schema

**Core tables:** `users`, `rooms`, `room_members`, `messages`
**Supporting tables:** `message_reactions`, `message_attachments`, `relationships`, `user_status_history`, `user_notifications`

See `database/schema.sql` for full DDL. There is **no migration framework** — the schema is applied once, automatically, the first time Postgres initializes its data volume (via `docker-entrypoint-initdb.d`).

Key indexes are defined in `database/indexes.sql`, including:
- Composite index on `messages(room_id, created_at DESC)` for efficient message pagination
- Partial index on `users(is_online)` for presence queries
- Partial index on `user_notifications(user_id) WHERE is_read = FALSE` for unread counts

## Deployment Topology

### Docker Compose (Development & Production)

All services run in Docker containers connected via a `chat-network` bridge network. There are no Kubernetes manifests or CI/CD pipeline configuration in this repository today.

**Infrastructure services:**
- PostgreSQL 16 (primary database)
- Redis 7 (cache, pub/sub)
- Elasticsearch 8.12 (full-text search)
- Nginx (reverse proxy, SSL termination)

**Monitoring stack:**
- Prometheus (metrics collection)
- Grafana (visualization)
- Jaeger (distributed tracing)

**Application services:**
- API Service (Spring Boot, Java 21)
- WebSocket Service (Spring Boot, Java 21)
- Worker Service (Spring Boot, Java 21)
- Search Service (Spring Boot, Java 21)
- Frontend Web (React/Vite, served via its own Nginx-based image)

## Scaling Considerations

See [docs/SCALING.md](docs/SCALING.md) for the detailed scaling guide.

**Stateless services** (API, Search) can be horizontally scaled behind a load balancer.
**Partially-stateful service** (WebSocket) keeps live connections in memory per instance, but room/presence events already fan out across instances via Redis pub/sub — scaling it still requires sticky sessions at the load balancer so a given client's connection and requests land on the same instance.
**Worker Service** should stay at a single replica unless distributed-scheduler coordination is added — its `@Scheduled` cron jobs have no leader election, so multiple replicas would duplicate maintenance runs.
**Stateful infrastructure** (PostgreSQL, Redis, Elasticsearch) requires its own clustering strategy for scale; none is configured in this repo's `docker-compose.yml`, which runs each as a single node.
