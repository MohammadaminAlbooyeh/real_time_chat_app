# Real-Time Chat Application

A **production-grade, scalable real-time chat platform** built with a Java/Spring Boot microservices architecture. Supports web, desktop (Electron), and mobile (React Native/Expo) clients with file uploads, full-text search, message reactions, and presence tracking.

---

## 🎯 Key Features

- ✅ **Real-Time Messaging** - WebSocket-based instant message delivery
- ✅ **Multi-Platform Clients** - Web (React), Desktop (Electron), Mobile (React Native/Expo)
- ✅ **User Authentication** - JWT-based auth with refresh tokens and session management
- ✅ **Message Features** - Reactions, attachments, edit/delete, typing indicators
- ✅ **Advanced Search** - Full-text search powered by Elasticsearch
- ✅ **User Relationships** - Friend requests, relationships, user discovery
- ✅ **File Uploads** - Type/size-validated file handling (stored on local disk)
- ✅ **Presence Tracking** - Online/offline status via Redis pub/sub
- ✅ **Scheduled Maintenance** - Background jobs via Spring `@Scheduled` (notifications, cleanup, indexing)
- ✅ **Monitoring & Observability** - Prometheus, Grafana, Jaeger tracing
- ✅ **Containerized** - Docker Compose, Nginx reverse proxy, security middleware

---

## 📐 Architecture Overview

```
┌──────────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                               │
├──────────────────┬──────────────────┬──────────────────────────────┤
│   Web Client     │  Desktop Client  │   Mobile Client              │
│  (React 18)      │   (Electron)     │  (React Native/Expo)        │
│  Port: 3000      │   Standalone     │   iOS/Android               │
└────────┬─────────┴──────────┬───────┴──────────────┬───────────────┘
         │                    │                      │
         └────────────────────┼──────────────────────┘
                              │
         ┌────────────────────▼───────────────────────┐
         │         NGINX REVERSE PROXY                │
         │        (SSL/TLS Termination)              │
         │     Ports: 80/443                         │
         └────────┬──────────────────────────────────┘
                  │
        ┌─────────┼──────────────┐
        │                        │
┌───────▼──────┐        ┌────────▼──────┐
│  API Service │        │  WebSocket     │
│ (Spring Boot)│        │  Service       │
│  Port: 8000  │        │ (Spring Boot)  │
│              │        │  Port: 8001    │
└──────┬───────┘        └──────┬─────────┘
       │                       │
   ┌───▼────────────────────▼──┐  ┌────────────────┐
   │      PostgreSQL (DB)      │  │  Redis          │
   │   Primary Data Store      │  │ Cache/Pub-Sub   │
   │      Port: 5432           │  │ Port: 6379      │
   └────────────────────────────┘  └───────┬─────────┘
                                            │
                                      ┌─────▼──────────┐
                                      │ Worker Service │
                                      │ (Spring Boot,  │
                                      │  @Scheduled)   │
                                      └────────────────┘

   ┌──────────────┐        ┌───────────────┐
   │Search Service│───────▶│ Elasticsearch │
   │(Spring Boot) │        │    :9200      │
   │  Port: 8002  │        └───────────────┘
   └──────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                  MONITORING & OBSERVABILITY                      │
├──────────────────┬──────────────────┬──────────────────────────────┤
│   Prometheus     │     Grafana      │      Jaeger                  │
│   :9090          │     :3001        │      :16686                  │
└──────────────────┴──────────────────┴──────────────────────────────┘
```

Note: in `docker-compose.yml`, the `api-service`, `websocket-service`, and `worker-service` all depend on Postgres and Redis directly; `search-service` depends only on Elasticsearch. `nginx` proxies to `api-service` and `websocket-service`; the search service is called by the worker (for indexing) and by clients that call it directly, not fronted by Nginx.

---

## 📁 Project Structure

```
real-time-chat-app/
│
├── 📂 services/                    # Backend microservices (Java 21, Spring Boot 3.5, Maven)
│   ├── api-service/               # REST API gateway (Spring MVC)
│   │   ├── src/main/java/com/chatapp/api/
│   │   │   ├── controller/       # REST controllers (auth, users, rooms, messages, upload, etc.)
│   │   │   ├── model/            # JPA entities (User, Room, Message, ...)
│   │   │   ├── repository/       # Spring Data JPA repositories
│   │   │   ├── service/          # Business logic (MessageService, RoomService, UserService)
│   │   │   ├── security/         # JwtService, JwtAuthFilter
│   │   │   ├── middleware/       # RateLimitingFilter, GlobalExceptionHandler
│   │   │   ├── config/           # SecurityConfig, WebConfig (CORS), RedisConfig
│   │   │   └── dto/              # Request/response records
│   │   └── pom.xml
│   │
│   ├── websocket-service/        # Real-time WebSocket service (Spring `TextWebSocketHandler`)
│   │   ├── src/main/java/com/chatapp/websocket/
│   │   │   ├── ws/               # ChatWebSocketHandler (the /ws endpoint)
│   │   │   ├── message/          # MessageHandler - inbound event processing
│   │   │   ├── manager/          # ConnectionManager - session/room tracking
│   │   │   ├── presence/         # PresenceTracker - online/offline via Redis
│   │   │   ├── redis/            # RedisPublisher, RoomEventSubscriber
│   │   │   └── repository/       # Direct JDBC-style message writes
│   │   └── pom.xml
│   │
│   ├── search-service/           # Full-text search (Elasticsearch)
│   │   ├── src/main/java/com/chatapp/search/
│   │   │   ├── controller/       # SearchController (index/search/delete)
│   │   │   └── client/           # SearchService (Elasticsearch client wrapper)
│   │   └── pom.xml
│   │
│   └── worker-service/           # Scheduled maintenance jobs (Spring `@Scheduled`)
│       ├── src/main/java/com/chatapp/worker/
│       │   ├── task/             # ScheduledTasks (cron-based jobs)
│       │   └── service/          # MaintenanceService, UserStatusService, RoomCleanupService,
│       │                         # NotificationService, SearchIndexService
│       └── pom.xml
│
├── 📂 frontend/                    # Client applications
│   ├── web/                       # React 18 + Vite + Redux Toolkit web app
│   ├── desktop/                   # Electron + React + Vite desktop app
│   └── mobile/                    # Expo / React Native mobile app
│
├── 📂 database/                    # Database schema (no migration framework)
│   ├── schema.sql               # Full schema, applied via docker-entrypoint-initdb.d
│   ├── indexes.sql              # Indexes, applied via docker-entrypoint-initdb.d
│   └── seeds/                   # Seed data for development
│
├── 📂 monitoring/                  # Monitoring & observability
│   ├── prometheus.yml
│   ├── grafana/
│   └── jaeger-config.yml
│
├── 📂 nginx/                       # Reverse proxy configuration
│   ├── nginx.conf
│   └── ssl/
│
├── 📂 scripts/                     # Utility shell scripts (backup, deploy, health-check, etc.)
│
├── 📂 tests/                       # Test suites (per-service tests live under each service's src/test/java)
│
├── 📂 docs/                        # Documentation (see docs/README.md for the index)
│
├── docker-compose.yml           # Base compose file
├── docker-compose.dev.yml       # Dev overrides
├── Makefile                     # Build & run commands
├── .env.example / .env.production
└── README.md                    # This file
```

---

## 🚀 Quick Start

### Prerequisites

- **Docker** 24+ & **Docker Compose** v2.20+
- **Node.js** 20+ (for local frontend development)
- **Java 21** & **Maven 3.9+** (for local backend development outside Docker)

### 1️⃣ Clone & Setup

```bash
git clone https://github.com/MohammadaminAlbooyeh/real_time_chat_app.git
cd real_time_chat_app

# Copy environment files
cp .env.example .env
```

### 2️⃣ Start Services

```bash
# Start all services in development mode
make dev

# OR using Docker Compose directly
docker compose -f docker-compose.yml -f docker-compose.dev.yml up
```

Each Java service builds its own image from its `Dockerfile` (Maven build inside the image). To build/test a single service locally without Docker:

```bash
cd services/api-service
mvn clean install   # build
mvn test            # run tests
```

### 3️⃣ Database Schema

There is **no migration tool** (no Flyway/Liquibase/Alembic). The schema is applied automatically the first time the Postgres container starts, via `docker-entrypoint-initdb.d`:

- `database/schema.sql` - tables
- `database/indexes.sql` - indexes
- `database/seeds/` - optional seed data

To change the schema, edit `database/schema.sql`/`indexes.sql` directly and recreate the Postgres volume (`docker compose down -v && docker compose up`), or apply the SQL manually against a running database with `psql`. The `make migrate` Makefile target is a leftover from the old stack and does not do anything useful — the Spring Boot `app.jar` has no `migrate` subcommand.

### 4️⃣ Access Application

| Service | URL |
|---------|-----|
| 🌐 Web UI | http://localhost:3000 |
| 🔌 API Service | http://localhost:8000 |
| 🔌 WebSocket Service | ws://localhost:8001/ws |
| 🔍 Search Service | http://localhost:8002 |
| 📊 Grafana | http://localhost:3001 |
| 🔍 Jaeger | http://localhost:16686 |
| 📈 Prometheus | http://localhost:9090 |

---

## 🛠 Tech Stack

### Backend
| Component | Technology | Purpose |
|-----------|-----------|---------|
| Language/Runtime | Java 21 | All backend services |
| Framework | Spring Boot 3.5 (Spring MVC, Spring WebSocket) | REST API & WebSocket |
| Persistence | Spring Data JPA (api-service), direct JDBC-style repository (websocket-service) | Data access |
| Database | PostgreSQL 16 | Primary data store |
| Cache | Redis 7 | Presence, pub/sub fan-out |
| Search | Elasticsearch 8.12 | Full-text search |
| Scheduling | Spring `@Scheduled` (cron) in worker-service | Maintenance/cleanup jobs |
| Auth | JWT via `jjwt`, Spring Security, BCrypt | Stateless authentication |
| Build | Maven | Per-service build/test |

### Frontend
| Layer | Technology | Purpose |
|-------|-----------|---------|
| Web | React 18 + Redux Toolkit + Vite | Web application |
| Desktop | Electron + React + Vite | Cross-platform desktop |
| Mobile | React Native + Expo | iOS/Android application |

### Infrastructure
| Component | Technology | Purpose |
|-----------|-----------|---------|
| Containers | Docker | Containerization |
| Orchestration | Docker Compose | Local dev & prod (no Kubernetes manifests in this repo) |
| Proxy | Nginx | Reverse proxy + SSL |
| Monitoring | Prometheus | Metrics collection |
| Visualization | Grafana | Dashboard visualization |
| Tracing | Jaeger | Distributed tracing |

---

## 📝 Key API Endpoints

### Authentication
```
POST   /api/v1/auth/register      # Sign up
POST   /api/v1/auth/login         # Sign in
POST   /api/v1/auth/refresh       # Refresh token
POST   /api/v1/auth/logout        # Sign out
```

### Users
```
GET    /api/v1/users/me                     # Get profile
PATCH  /api/v1/users/me                     # Update profile
POST   /api/v1/users/me/change-password     # Change password
GET    /api/v1/users/search?q=...           # Search users
GET    /api/v1/users/{userId}               # Get a user by id
```

### Rooms
```
POST   /api/v1/rooms                        # Create room
GET    /api/v1/rooms                        # List rooms
GET    /api/v1/rooms/{roomId}               # Get room details
PUT    /api/v1/rooms/{roomId}               # Update room
DELETE /api/v1/rooms/{roomId}               # Delete room
POST   /api/v1/rooms/{roomId}/members       # Add member
DELETE /api/v1/rooms/{roomId}/members/{id}  # Remove member
POST   /api/v1/rooms/{roomId}/leave         # Leave room
```

### Messages
```
GET    /api/v1/rooms/{roomId}/messages                       # List messages
POST   /api/v1/rooms/{roomId}/messages                       # Send message
PUT    /api/v1/rooms/{roomId}/messages/{messageId}            # Edit message
DELETE /api/v1/rooms/{roomId}/messages/{messageId}            # Delete message
POST   /api/v1/rooms/{roomId}/messages/{messageId}/reactions  # Add reaction
DELETE /api/v1/rooms/{roomId}/messages/{messageId}/reactions/{reactionId}  # Remove reaction
```

📖 **[Full API Documentation](docs/API_REFERENCE.md)**

---

## 🔌 WebSocket Events

Real-time events are transmitted via WebSocket at `ws://localhost:8001/ws?token=<jwt>`:

```javascript
// Client → Server
{ "type": "message:send", "payload": { "room_id": "...", "content": "..." } }
{ "type": "typing:start", "payload": { "room_id": "..." } }
{ "type": "room:join", "payload": { "room_id": "..." } }

// Server → Client
{ "type": "message:new", "payload": { "id": "...", "content": "...", ... } }
{ "type": "typing:indicator", "payload": { "user_id": "...", "room_id": "...", "is_typing": true } }
{ "type": "presence:online", "payload": { "user_id": "...", "username": "..." } }
```

📖 **[WebSocket Protocol Details](docs/WEBSOCKET_EVENTS.md)**

---

## 📊 Development Commands

```bash
# Build & start services
make dev              # Development compose with dev overrides
make up               # Start services in background
make down             # Stop services
make build            # Build images

# Testing (per service, via Maven)
make test-api         # cd services/api-service && mvn test
make test-ws          # cd services/websocket-service && mvn test
make test-worker      # cd services/worker-service && mvn test
make test-search      # cd services/search-service && mvn test
make test-all         # run all four

# Cleanup
make clean            # Remove containers & volumes
make logs             # View service logs
```

See **[Makefile](Makefile)** for all available commands. Note: the Makefile's `migrate` target is a dead leftover — see the "Database Schema" section above.

---

## 🧪 Testing

Each Java service has its own JUnit test suite under `src/test/java`, run with Maven:

```bash
cd services/api-service && mvn test
cd services/websocket-service && mvn test
cd services/worker-service && mvn test
cd services/search-service && mvn test
```

---

## 🔐 Security Features

- ✅ **JWT Authentication** - Stateless auth via `jjwt`, enforced by a custom `JwtAuthFilter` in Spring Security
- ✅ **Password Hashing** - BCrypt
- ✅ **CORS Protection** - Configurable allowed origins (`WebConfig`, backed by `ApiProperties`)
- ✅ **Rate Limiting** - `RateLimitingFilter` servlet filter
- ✅ **SQL Injection Prevention** - Parameterized queries via Spring Data JPA
- ✅ **File Upload Validation** - Content-type allowlist & 50MB size limit (see `UploadController`)
- ✅ **HTTPS/TLS** - Nginx SSL/TLS termination

📖 **[Security Guidelines](docs/SECURITY.md)**

---

## 📈 Monitoring & Observability

- **Prometheus** - Metrics collection (requests, latency, errors)
- **Grafana** - Visual dashboards for real-time monitoring
- **Jaeger** - Distributed tracing for request flow analysis

Access monitoring at:
- Grafana Dashboard: http://localhost:3001
- Jaeger UI: http://localhost:16686
- Prometheus: http://localhost:9090

📖 **[Monitoring Setup](docs/MONITORING.md)**

---

## 🚀 Deployment

### Development
```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up
```

### Production
```bash
# Set environment variables in .env.production
cp .env.production .env

# Build and start services
docker compose up -d
```

There is no migration step for production deploys — the schema SQL is applied once, when the Postgres volume is first initialized. This repository does not include Kubernetes manifests or a CI/CD pipeline configuration for deployment; it is containerized with Docker and can in principle be adapted to another orchestrator, but that adaptation does not exist here today.

📖 **[Detailed Deployment Guide](docs/DEPLOYMENT.md)**

---

## 📚 Documentation

- **[Architecture](docs/ARCHITECTURE.md)** - System design and component interactions
- **[API Reference](docs/API_REFERENCE.md)** - Complete API documentation
- **[Installation](docs/INSTALLATION.md)** - Detailed setup instructions
- **[Deployment](docs/DEPLOYMENT.md)** - Production deployment guide
- **[WebSocket Protocol](docs/WEBSOCKET_EVENTS.md)** - Real-time event specification
- **[Monitoring](docs/MONITORING.md)** - Observability setup
- **[Security](docs/SECURITY.md)** - Security best practices
- **[Troubleshooting](docs/TROUBLESHOOTING.md)** - Common issues & solutions
- **[Scaling](docs/SCALING.md)** - Performance optimization & scaling

---

## 🤝 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](docs/CONTRIBUTING.md) for guidelines on:
- Code style and standards
- Testing requirements
- Commit message format
- Pull request process

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👤 Author

**Amin Albooyeh**
- Email: amin.albooyeh@gmail.com
- GitHub: [@MohammadaminAlbooyeh](https://github.com/MohammadaminAlbooyeh)

---

## 📞 Support

For issues, questions, or suggestions:
- **GitHub Issues**: [Open an issue](https://github.com/MohammadaminAlbooyeh/real_time_chat_app/issues)
- **Documentation**: Check the [docs](docs/) folder
- **Email**: amin.albooyeh@gmail.com

---

## 🎯 Roadmap

Not yet implemented:

- [ ] Push notifications (Firebase Cloud Messaging)
- [ ] Message encryption (end-to-end)
- [ ] Video/voice calls (WebRTC)
- [ ] Message forwarding
- [ ] User blocking
- [ ] Group voice channels
- [ ] Message pinning
- [ ] Custom emoji reactions
- [ ] Mobile app publishing (App Store/Play Store)
- [ ] Desktop app publishing (Windows/macOS installers)

---

**A production-grade real-time chat platform, built on Java/Spring Boot.**
