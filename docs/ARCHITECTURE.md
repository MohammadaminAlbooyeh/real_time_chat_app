# Architecture

## System Overview

The Real-Time Chat Application follows a microservices architecture with four independent Java/Spring Boot backend services, three frontend clients, and a monitoring stack. The design emphasizes separation of concerns, horizontal scalability, and real-time communication.

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
                                    │ (React,    │
                                    │  nginx-    │
                                    │  served)   │
                                    └────────────┘
```

Per `docker-compose.yml`: `nginx` depends on `api-service` and `websocket-service` only (those are the two proxied upstreams in `nginx/nginx.conf`). `api-service`, `websocket-service`, and `worker-service` all depend on `postgres` and `redis`. `search-service` depends only on `elasticsearch`. `frontend-web` is a separate container (served by its own Nginx, port 3000) that depends on `api-service` and `websocket-service` being reachable at runtime, not at build/startup order.

## Microservices Detail

### 1. API Service (`services/api-service/`)

**Framework:** Spring Boot 3.5 (Spring MVC, Spring Data JPA, Spring Security), Java 21
**Port:** 8000

The primary HTTP API gateway handling all synchronous CRUD operations.

**Key packages (`com.chatapp.api`):**
- `controller/` - `AuthController`, `UserController`, `RoomController`, `MessageController`, `RelationshipController`, `SearchController`, `UploadController`, `NotificationController`, `HealthController`
- `model/` - JPA entities: `User`, `Room`, `RoomMember`, `Message`, `MessageReaction`, `MessageAttachment`, `Relationship`, `UserNotification`
- `repository/` - Spring Data JPA repositories, one per entity
- `service/` - `UserService`, `RoomService`, `MessageService`, `RedisPublisherService`
- `security/` - `JwtService` (token issue/verify), `JwtAuthFilter` (request-level auth)
- `middleware/` - `RateLimitingFilter`, `GlobalExceptionHandler`
- `config/` - `SecurityConfig` (stateless JWT filter chain, BCrypt), `WebConfig` (CORS + `/uploads/**` static resource mapping), `RedisConfig`, `PropertiesConfig`

**Key architecture decisions:**
- Stateless JWT auth: `SecurityConfig` disables CSRF and sessions, permits `/health*` and `/api/v1/auth/{register,login,refresh}`, and requires authentication everywhere else via `JwtAuthFilter`.
- Message mutations (send/edit/delete/reactions) are written via `MessageService` and published to Redis so `websocket-service` can fan them out.
- File uploads are written straight to a local `uploads/` directory on the container filesystem (see `UploadController`), not to S3 or a CDN.

### 2. WebSocket Service (`services/websocket-service/`)

**Framework:** Spring Boot (plain `org.springframework.web.socket.handler.TextWebSocketHandler`, not STOMP/SockJS)
**Port:** 8001

Handles all real-time bidirectional communication. Maintains in-memory state of active connections and room subscriptions.

**Key packages (`com.chatapp.websocket`):**
- `ws/ChatWebSocketHandler` - the single WebSocket endpoint, registered at `/ws` by `config/WebSocketConfig` (`@EnableWebSocket`)
- `message/MessageHandler` - dispatches inbound event types: `message:send`, `typing:start`, `typing:stop`, `room:join`, `room:leave`
- `manager/ConnectionManager` - tracks sessions per user and room membership, performs broadcasts
- `presence/PresenceTracker` - sets `presence:{userId}` in Redis and publishes `presence:online`/`presence:offline` events
- `redis/RedisPublisher` - publishes events to Redis pub/sub
- `redis/RoomEventSubscriber` - subscribes to the `room:*` channel pattern so events from other instances (or from api-service) reach locally-connected clients
- `repository/MessageRepository` - writes new messages directly (hand-written SQL via Spring's JDBC support, not a JPA entity)
- `security/JwtService` - verifies the JWT passed as the `token` query parameter on connect

**Connection lifecycle:**
1. Client connects to `ws://host/ws?token=<jwt>`.
2. `ChatWebSocketHandler.afterConnectionEstablished` verifies the JWT; on failure it closes the session with code 4001.
3. On success, the connection is registered in `ConnectionManager` and `PresenceTracker.setOnline(...)` is called.
4. Client sends `room:join` to subscribe to room events.
5. Server broadcasts events to all room subscribers via `ConnectionManager`.
6. On disconnect, the user is removed from `ConnectionManager` and marked offline via `PresenceTracker.setOffline(...)`.

**Event flow patterns:**
- **Client-initiated messages:** client sends `message:send` → `MessageHandler` inserts the row via `MessageRepository` → broadcasts `message:new` to the room (excluding the sender) → publishes the same event on Redis so other websocket-service instances can forward it.
- **Typing indicators:** client sends `typing:start`/`typing:stop` → broadcast as `typing:indicator` to room subscribers (excluding sender); not persisted.
- **Cross-instance fan-out:** any event published to a `room:{roomId}` Redis channel is picked up by `RoomEventSubscriber` and rebroadcast to locally-connected sessions in that room.

### 3. Search Service (`services/search-service/`)

**Framework:** Spring Boot wrapping the Elasticsearch Java client
**Port:** 8002

Dedicated search service providing a simple internal API for indexing, searching, and deleting messages in Elasticsearch.

**Key packages (`com.chatapp.search`):**
- `controller/SearchController` - REST endpoints below
- `client/SearchService` - Elasticsearch connection and query construction
- `dto/` - `IndexMessageRequest`, `SearchResult`

**Endpoints:**
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/search/index` | Index a message |
| GET | `/api/v1/search/messages` | Search messages (`q`, optional `roomId`, `limit` capped at 200, `offset`) |
| DELETE | `/api/v1/search/messages/{messageId}` | Remove a message from the index |
| GET | `/api/v1/search/health` | Health check |

### 4. Worker Service (`services/worker-service/`)

**Framework:** Spring Boot using Spring's `@Scheduled` cron annotations — **not** Celery, and there is no separate message broker for task dispatch (Redis is still used, but only by the services it reads/writes, not as a task queue).
**Port:** 8003 (exposes Spring Boot Actuator/health endpoints; it is otherwise a background process, not a user-facing API)

**Scheduled tasks (`task/ScheduledTasks`):**
| Method | Cron | Delegates to |
|--------|------|--------------|
| `cleanupExpiredSessions` | `0 3 * * *` (daily 3:00 AM) | `MaintenanceService` |
| `archiveOldMessages` | `0 4 * * *` (daily 4:00 AM) | `MaintenanceService` |
| `updateStaleStatuses` | `0 */5 * * * *` (every 5 min) | `UserStatusService` |
| `cleanupInactiveUsers` | `0 5 * * *` (daily 5:00 AM) | `UserStatusService` |
| `cleanupAbandonedRooms` | `0 6 * * *` (daily 6:00 AM) | `RoomCleanupService` |

**Supporting services:**
- `MaintenanceService` - session/message cleanup and archival
- `UserStatusService` - presence staleness and inactive-user cleanup
- `RoomCleanupService` - deletes abandoned rooms
- `NotificationService` - notification persistence/dispatch
- `SearchIndexService` - calls `search-service` over HTTP (`RestTemplate`) at `${app.search-service.url}/api/v1/search/index` (and the delete equivalent) to keep the Elasticsearch index in sync

## Component Interaction

### API Service ↔ PostgreSQL
- Spring Data JPA over the `postgresql` JDBC driver.
- All CRUD operations go through repository interfaces backed by JPA entities.
- No migration framework — schema changes are applied by editing `database/schema.sql`/`indexes.sql` and re-initializing the database (see below).

### API Service ↔ Redis
- `RedisPublisherService` publishes room events for message mutations.

### WebSocket ↔ Redis
- Subscribes to the `room:*` pattern for cross-instance/API-initiated events (`RoomEventSubscriber`).
- Maintains presence data with `presence:{userId}` keys and room membership with `room:{roomId}:members` sets.
- Publishes presence events to the `presence` channel.

### Worker ↔ Postgres/Redis
- Scheduled jobs operate directly against Postgres via the services listed above; Redis is used for whatever caching/presence state those services need, not as a task broker.

### Worker ↔ Search Service
- `SearchIndexService` makes plain HTTP calls (`RestTemplate`) to `search-service`'s `/api/v1/search/index` and delete endpoints.

## Deployment Architecture

See DEPLOYMENT.md for the complete deployment guide.

**Development:** `docker-compose.yml` + `docker-compose.dev.yml` overrides, started via `make dev`.
**Production:** `docker-compose.yml` with `.env.production` values. This repository does not include Kubernetes manifests, Helm charts, or a configured CI/CD pipeline — deployment today is Docker Compose only.

### Network Topology

All services join the `chat-network` bridge network defined in `docker-compose.yml`. Services communicate by service name (Docker DNS).
- External: Nginx (80/443), Frontend Web (3000), Postgres (5432, exposed for local tooling), Redis (6379), Elasticsearch (9200)
- Monitoring: Prometheus (9090), Grafana (3001), Jaeger (16686, OTLP 4318)

(Note: in the current `docker-compose.yml`, Postgres, Redis, and Elasticsearch ports are published to the host for local development convenience; tighten this for a real production deployment.)

## Data Flow Examples

### User sends a message (WebSocket path):
```
Client → WebSocket Service → PostgreSQL (write, via MessageRepository)
                           → Redis pub/sub (event)
                           → In-memory broadcast to room members
Worker (scheduled, not triggered per-message) → SearchIndexService → Search Service → Elasticsearch
```
Note: unlike a task-queue design, there is no immediate "index this message now" background job fired on send — indexing is driven by whatever the worker's scheduled/service logic does, backed by direct HTTP calls into search-service.

### User edits a message (REST path):
```
Client → API Service → PostgreSQL (update)
                     → Redis pub/sub (room:{id})
WebSocket Service ← Redis subscription (RoomEventSubscriber)
                 → In-memory broadcast to room members
```

### User searches messages:
```
Client → API Service (SearchController, /api/v1/search/messages) → MessageService
       → (delegates to) Search Service → Elasticsearch
```

## Database Schema

Schema is defined in `database/schema.sql` and `database/indexes.sql`, applied automatically on first container start via Postgres's `docker-entrypoint-initdb.d` mechanism (see `docker-compose.yml`). There is no migration tool wired into this project.

### Core Tables
- **users** - User accounts with profile info, auth fields, presence status
- **rooms** - Chat rooms with metadata
- **room_members** - Many-to-many user-room with role (admin/member)
- **messages** - Chat messages with soft-delete and edit tracking

### Supporting Tables
- **message_reactions** - Emoji reactions on messages (unique per user+message+emoji)
- **message_attachments** - File attachments linked to messages
- **relationships** - Friend connections with status (pending/accepted)
- **user_status_history** - Audit log of status changes
- **user_notifications** - Notifications stored per user

### Key Indexes
```sql
idx_messages_room_created ON messages(room_id, created_at DESC)
idx_users_online ON users(is_online) WHERE is_online = TRUE
idx_user_notifications_unread ON user_notifications(user_id) WHERE is_read = FALSE
```

See `database/indexes.sql` for the complete list.

## Security Architecture

See SECURITY.md for the detailed security overview.

- **Authentication:** JWT (via the `jjwt` library) issued/verified by `JwtService`; enforced per-request by `JwtAuthFilter` in `SecurityConfig`'s stateless filter chain.
- **Password hashing:** BCrypt (`BCryptPasswordEncoder`).
- **Authorization:** Ownership and room-membership checks performed in service/controller code.
- **CORS:** Configured allowed origins via `WebConfig` + `ApiProperties`.
- **Rate limiting:** `RateLimitingFilter` servlet filter in api-service.
- **File upload:** Content-type allowlist and 50MB size limit enforced in `UploadController`; files are written to local disk, not scanned or sent to a CDN (see SECURITY.md for what is and isn't implemented).
- **Secrets:** Environment variables (`.env` files per service), never hardcoded.
