# Architecture

## System Overview

The Real-Time Chat Application is built on a microservices architecture with four backend services, a shared library, three frontend clients, and supporting infrastructure.

```
                                ┌─────────────┐
                                │   Nginx      │
                                │  (Reverse    │
                                │   Proxy)     │
                                └──────┬──────┘
                                       │
                    ┌──────────────────┼──────────────────┐
                    │                  │                  │
              ┌─────▼─────┐    ┌──────▼──────┐    ┌─────▼─────┐
              │   API      │    │  WebSocket   │    │  Frontend │
              │  Service   │    │  Service     │    │  (Web)    │
              │  :8000     │    │  :8001       │    │  :3000    │
              └─────┬─────┘    └──────┬──────┘    └───────────┘
                    │                  │
                    │    ┌─────────────┤
                    │    │             │
              ┌─────▼────▼──┐   ┌─────▼──────┐
              │  PostgreSQL │   │    Redis    │
              │  :5432      │   │   :6379     │
              └─────┬───────┘   └─────┬──────┘
                    │                 │
              ┌─────▼───────┐   ┌─────▼──────┐
              │   Worker    │   │  Search    │
              │   Service   │──▶│  Service   │
              │   (Celery)  │   │  :8002     │
              └─────────────┘   └─────┬──────┘
                                      │
                                ┌─────▼──────┐
                                │Elasticsearch│
                                │   :9200     │
                                └────────────┘
```

## Microservices Breakdown

### 1. API Service (FastAPI)

**Port:** 8000

The primary REST API gateway. Handles all CRUD operations.

**Responsibilities:**
- User authentication and registration
- User profile management
- Room creation, membership, and administration
- Message sending, editing, deletion
- Message reactions management
- Friend relationships and friend requests
- Full-text search across messages (via database ILIKE)
- File uploads with validation
- Notification listing and management
- Health checks

**Key Technologies:** FastAPI, SQLAlchemy async, Pydantic, python-jose, passlib (bcrypt)

**Endpoints:** See API_DOCS.md for complete reference.

### 2. WebSocket Service (FastAPI)

**Port:** 8001

Handles all real-time bidirectional communication.

**Responsibilities:**
- WebSocket connection management (connect/disconnect)
- Real-time message delivery
- Typing indicators broadcast
- Room join/leave tracking
- Presence tracking (online/offline status)
- Redis pub/sub event forwarding to connected clients

**Key Technologies:** FastAPI WebSockets, Redis pub/sub, asyncio

**Connection Flow:**
1. Client connects to `ws://host:8001/ws?token=<jwt_token>`
2. Server verifies JWT token
3. Connection registered in `ConnectionManager`
4. Client sends `room:join` to subscribe to room events
5. Messages are broadcast to all room members via WebSocket
6. Redis pub/sub forwards events from API service to WebSocket service

### 3. Search Service (FastAPI)

**Port:** 8002

Dedicated full-text search service backed by Elasticsearch.

**Responsibilities:**
- Index messages into Elasticsearch
- Full-text search across indexed messages
- Delete messages from index
- Health monitoring

**Key Technologies:** FastAPI, Elasticsearch async client

**Index Mapping:**
```
messages index:
  - id (keyword)
  - content (text, analyzed with custom analyzer)
  - room_id (keyword)
  - sender_id (keyword)
  - sender_username (keyword)
  - message_type (keyword)
  - created_at (date)
```

### 4. Worker Service (Celery)

Async task processor for background jobs.

**Responsibilities:**
- **Notifications:** Send push and email notifications, notify offline users
- **Search Indexing:** Index/delete messages in Elasticsearch via Search Service
- **Maintenance:** Clean up expired sessions, archive old messages
- **User Maintenance:** Update stale online statuses, clean up inactive users
- **Room Maintenance:** Clean up abandoned rooms

**Task Schedules (Celery Beat):**
| Task | Schedule | Description |
|------|----------|-------------|
| `cleanup_expired_sessions` | Daily 3:00 AM | Delete old status history |
| `archive_old_messages` | Daily 4:00 AM | Soft-delete messages > 90 days |
| `update_stale_statuses` | Every 5 minutes | Mark idle users offline |

**Key Technologies:** Celery, Redis (broker/backend), SQLAlchemy sync

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

### Async Task Flow
```
API Service → Redis (Celery broker) → Worker Service → Search Service → Elasticsearch
                                                     → PostgreSQL
                                                     → Redis (pub/sub)
```

### Search Flow
```
Client → API Service → PostgreSQL (ILIKE search)
       → Search Service → Elasticsearch (full-text)
```

## Communication Patterns

### REST API
- Synchronous HTTP/HTTPS via FastAPI
- JSON request/response bodies
- JWT Bearer token authentication
- CORS configured for web clients

### WebSocket
- Persistent bidirectional TCP connections
- JSON-encoded message frames
- Token-based authentication on connection
- Room-scoped message broadcasting
- Typing indicator events
- Presence events (online/offline)

### Redis Pub/Sub
- API service publishes room events to `room:{room_id}` channels
- WebSocket service subscribes to `room:*` pattern
- Used for: message updates, deletions, reactions
- Also used for `presence` channel for global status updates

### Celery Task Queue
- Redis as message broker (DB 1) and result backend (DB 2)
- Tasks are serialized as JSON
- `task_acks_late=True` for at-least-once delivery
- `worker_prefetch_multiplier=1` for fair task distribution

## Shared Library (`shared/`)

The `shared/` package contains code used across multiple services:

- **models/** - SQLAlchemy model definitions (User, Room, Message)
- **schemas/** - Pydantic validation schemas
- **constants/** - Configuration constants, error codes
- **utils/** - Formatters, helpers, validators

## Database Schema

**Core tables:** `users`, `rooms`, `room_members`, `messages`
**Supporting tables:** `message_reactions`, `message_attachments`, `relationships`, `user_status_history`, `user_notifications`

See `database/schema.sql` for full DDL.

Key indexes are defined in `database/indexes.sql`, including:
- Composite index on `messages(room_id, created_at DESC)` for efficient message pagination
- Partial index on `users(is_online)` for presence queries
- Partial index on `user_notifications(user_id) WHERE is_read = FALSE` for unread counts

## Deployment Topology

### Docker Compose (Development & Production)

All services run in Docker containers connected via a `chat-network` bridge network.

**Infrastructure services:**
- PostgreSQL 16 (primary database)
- Redis 7 (cache, pub/sub, task broker)
- Elasticsearch 8 (full-text search)
- Nginx (reverse proxy, SSL termination)

**Monitoring stack:**
- Prometheus (metrics collection)
- Grafana (visualization, pre-provisioned dashboards)
- Jaeger (distributed tracing)
- Sentry (error tracking)

**Application services:**
- API Service (FastAPI, 1+ instances)
- WebSocket Service (FastAPI, 1+ instances)
- Worker Service (Celery, 1+ instances)
- Search Service (FastAPI, 1 instance)
- Frontend Web (React/Vite, served via Nginx)

## Scaling Considerations

See SCALING.md for detailed scaling guide.

**Stateless services** (API, WebSocket, Search, Worker) can be horizontally scaled.
**Stateful services** (PostgreSQL, Redis, Elasticsearch) require clustering.

- API Service: Stateless, scale with load balancer
- WebSocket Service: Stateful (in-memory connections), requires sticky sessions or moving connection state to Redis
- Worker Service: Stateless, scale with more workers for concurrent task processing
- Search Service: Stateless, scale behind load balancer
