# Architecture

## System Overview

The Real-Time Chat Application follows a microservices architecture with four distinct backend services, a shared code library, multiple frontend clients, and a comprehensive monitoring stack. The design emphasizes separation of concerns, horizontal scalability, and real-time communication.

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
                        │                  │                  │
                  ┌─────▼─────┐    ┌──────▼──────┐    ┌─────▼──────┐
                  │   API      │    │  WebSocket   │    │  Frontend  │
                  │  Service   │    │  Service     │    │   Web      │
                  │  :8000     │    │  :8001       │    │  :3000     │
                  │  (FastAPI) │    │  (FastAPI)   │    │  (React)   │
                  └─────┬─────┘    └──────┬──────┘    └────────────┘
                        │                  │
                  ┌─────▼──────────────────▼──────┐
                  │           Redis                │
                  │   :6379 - Pub/Sub & Cache      │
                  │   :6379/1 - Celery Broker      │
                  │   :6379/2 - Celery Backend     │
                  └─────┬──────────────────┬──────┘
                        │                  │
                  ┌─────▼───────┐   ┌─────▼──────┐
                  │  PostgreSQL  │   │  Worker     │
                  │  :5432       │   │  Service    │
                  │  (Primary    │   │  (Celery)   │
                  │   Store)     │   └─────┬──────┘
                  └─────────────┘         │
                                    ┌─────▼──────┐
                                    │   Search    │
                                    │   Service   │
                                    │   :8002     │
                                    │  (FastAPI)  │
                                    └─────┬──────┘
                                          │
                                    ┌─────▼──────┐
                                    │Elasticsearch│
                                    │   :9200     │
                                    └────────────┘
```

## Microservices Detail

### 1. API Service (`services/api-service/`)

**Framework:** FastAPI with async SQLAlchemy
**Port:** 8000

The primary HTTP API gateway handling all synchronous CRUD operations. It is the most heavily loaded service and the first to be scaled horizontally.

**Modules:**
- `routes/` - Endpoint definitions organized by domain (auth, users, rooms, messages, relationships, search, upload, notifications, health)
- `models/` - SQLAlchemy ORM models (User, Room, RoomMember, Message, MessageReaction, MessageAttachment, Relationship, UserNotification)
- `schemas/` - Pydantic request/response validation schemas
- `auth.py` - JWT creation, decoding, password hashing with bcrypt
- `middleware.py` - CORS, request timing
- `dependencies.py` - Dependency injection (get_current_user, get_optional_user)
- `database.py` - Async SQLAlchemy engine and session factory
- `redis_client.py` - Async Redis client for publishing events

**Key architecture decisions:**
- Uses `NullPool` to avoid connection pooling issues in async context
- Sessions are created per-request via dependency injection
- Message mutations (edit, delete, reaction) publish events to Redis pub/sub

### 2. WebSocket Service (`services/websocket-service/`)

**Framework:** FastAPI with WebSocket support
**Port:** 8001

Handles all real-time bidirectional communication. Maintains in-memory state of active connections and room subscriptions.

**Modules:**
- `routes.py` - Single WebSocket endpoint at `/ws`
- `connection_manager.py` - Manages active connections and room subscriptions
- `message_handler.py` - Processes incoming WebSocket events
- `presence_tracker.py` - Manages online/offline status publishing
- `middleware.py` - JWT verification for WebSocket connections
- `redis_client.py` - Redis pub/sub subscriber and presence store
- `database.py` - Direct database access for WebSocket message writes

**Connection lifecycle:**
1. Client connects to `ws://host/ws?token=<jwt>`
2. Server verifies JWT and registers connection
3. Client sends `room:join` to subscribe to room events
4. Server broadcasts events to all room subscribers
5. On disconnect, user is removed from rooms and marked offline

**Event flow patterns:**
- **Client-initiated messages:** Client sends `message:send` → WebSocket writes to DB → broadcasts to room (excluding sender) → publishes to Redis for API service
- **API-initiated updates:** API service edits/deletes message → publishes to `room:{id}` → WebSocket service receives via Redis → broadcasts to room
- **Typing indicators:** Client sends `typing:start/stop` → WebSocket broadcasts to room subscribers (excluding sender)

### 3. Search Service (`services/search-service/`)

**Framework:** FastAPI
**Port:** 8002

Dedicated search service that wraps Elasticsearch operations. Provides a simple internal API for indexing, searching, and deleting messages.

**Modules:**
- `routes.py` - Internal REST endpoints for index/search/delete
- `elasticsearch_client.py` - ES connection management and query construction
- `models.py` - Pydantic models for request/response

**Endpoints (internal):**
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/search/index` | Index a message |
| GET | `/api/v1/search/messages` | Search messages |
| DELETE | `/api/v1/search/messages/{id}` | Remove from index |
| GET | `/api/v1/search/health` | Health check |

**Search analysis:** Custom `message_analyzer` using standard tokenizer with lowercase and stop filter.

### 4. Worker Service (`services/worker-service/`)

**Framework:** Celery with Redis broker
**Port:** N/A (background worker)

Handles asynchronous and scheduled tasks.

**Task categories:**
- **Notification tasks** (`tasks/notification_tasks.py`):
  - `send_push_notification` - Store push notification in DB
  - `send_email_notification` - Store email notification (email integration placeholder)
  - `notify_offline_users` - Send push notifications to offline room members

- **Message tasks** (`tasks/message_tasks.py`):
  - `index_message` - Index message in Elasticsearch (via Search Service API)
  - `process_message_attachments` - Verify attachment file sizes
  - `delete_message_from_index` - Remove message from search index

- **User tasks** (`tasks/user_tasks.py`):
  - `update_stale_statuses` - Mark users offline after 5 min inactivity
  - `cleanup_inactive_users` - Deactivate users inactive for 90 days

- **Room tasks** (`tasks/room_tasks.py`):
  - `cleanup_abandoned_rooms` - Delete rooms with no members older than 30 days
  - `update_room_last_activity` - Update room's updated_at timestamp

- **Maintenance tasks** (`tasks/maintenance_tasks.py`):
  - `cleanup_expired_sessions` - Delete old user_status_history records
  - `archive_old_messages` - Soft-delete messages older than 90 days

**Scheduled tasks (Celery Beat):**
```python
cleanup_expired_sessions: daily 3:00 AM
archive_old_messages: daily 4:00 AM
update_stale_statuses: every 5 minutes
```

## Component Interaction

### API Service ↔ PostgreSQL
- SQLAlchemy async ORM with connection pooling via `async_sessionmaker`
- All CRUD operations go through the database session
- Migrations via Alembic

### API Service ↔ Redis
- Publishes room events (message:updated, message:deleted, message:reaction)
- Events published to channel `room:{room_id}`

### WebSocket ↔ Redis
- Subscribes to `room:*` pattern for API-initiated events
- Maintains presence data with `presence:{user_id}` keys
- Tracks room members with `room:{room_id}:members` sets
- Publishes presence events to `presence` channel

### Worker ↔ Redis
- Uses Redis as Celery broker (DB 1) and result backend (DB 2)
- `task_acks_late=True` ensures at-least-once delivery
- `worker_prefetch_multiplier=1` for fair distribution

### Worker ↔ Search Service
- HTTP calls to Search Service API for indexing/deleting
- Retries with timeouts for resilience

### API Service ↔ Search (direct)
- Database-level ILIKE search for basic text search
- Elasticsearch-backed search delegated to Search Service

## Shared Library (`shared/`)

The `shared/` package provides reusable code:
- `models/` - SQLAlchemy model definitions shared between services
- `schemas/` - Common Pydantic schemas
- `constants/` - App-wide constants, error codes, configuration
- `utils/` - Helper functions, validators, formatters

## Deployment Architecture

See DEPLOYMENT.md for complete deployment guide.

**Development:** Single Docker Compose file with hot-reload volumes
**Production:** Docker Compose with separate `.env` files per service, SSL termination at Nginx, optional orchestrator (Kubernetes/Swarm)

### Network Topology

All services join `chat-network` bridge network. Services communicate by service name (Docker DNS). Ports are exposed only as needed:
- External: Nginx (80/443), Frontend Web (3000)
- Internal: PostgreSQL (5432), Redis (6379), Elasticsearch (9200)
- Monitoring: Prometheus (9090), Grafana (3001), Jaeger (16686)

## Data Flow Examples

### User sends a message (WebSocket path):
```
Client → WebSocket Service → PostgreSQL (write)
                           → Redis pub/sub (event)
                           → In-memory broadcast to room members
Worker → Redis (Celery) → index_message → Search Service → Elasticsearch
                       → notify_offline_users
```

### User edits a message (REST path):
```
Client → API Service → PostgreSQL (update)
                     → Redis pub/sub (room:{id})
WebSocket Service ← Redis subscription
                 → In-memory broadcast to room members
```

### User searches messages:
```
Client → API Service → PostgreSQL (ILIKE search)
       → Search Service → Elasticsearch (if delegated)
```

## Database Schema

Schema is defined in `database/schema.sql` and managed via Alembic migrations in `database/migrations/`.

### Core Tables
- **users** - User accounts with profile info, auth fields, presence status
- **rooms** - Chat rooms with metadata
- **room_members** - Many-to-many user-room with role (admin/member)
- **messages** - Chat messages with soft-delete and edit tracking

### Supporting Tables
- **message_reactions** - Emoji reactions on messages (unique per user+message+emoji)
- **message_attachments** - File attachments linked to messages
- **relationships** - Friend connections with status (pending/accepted/blocked)
- **user_status_history** - Audit log of status changes
- **user_notifications** - Push/email notifications stored per user

### Key Indexes
```sql
idx_messages_room_created ON messages(room_id, created_at DESC)
idx_users_online ON users(is_online) WHERE is_online = TRUE
idx_user_notifications_unread ON user_notifications(user_id) WHERE is_read = FALSE
```

See `database/indexes.sql` for the complete list.

## Security Architecture

See SECURITY.md for detailed security overview.

- **Authentication:** JWT with HS256, access tokens (30min) + refresh tokens (7 days)
- **Password hashing:** bcrypt via passlib
- **Authorization:** Role-based (room admin/member), ownership checks
- **CORS:** Configured allowed origins only
- **File upload:** Content type whitelist, size limit (50MB)
- **Secrets:** Environment variables, never hardcoded
