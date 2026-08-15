# Real-Time Chat Application

A **production-grade, scalable real-time chat platform** built with microservices architecture. Supports web, desktop (Electron), and mobile (React Native) clients with advanced features like file uploads, full-text search, message reactions, and presence tracking.

[![CI](https://github.com/MohammadaminAlbooyeh/real_time_chat_app/actions/workflows/ci.yml/badge.svg)](https://github.com/MohammadaminAlbooyeh/real_time_chat_app/actions/workflows/ci.yml)
[![CD](https://github.com/MohammadaminAlbooyeh/real_time_chat_app/actions/workflows/cd.yml/badge.svg)](https://github.com/MohammadaminAlbooyeh/real_time_chat_app/actions/workflows/cd.yml)
[![Coverage](https://codecov.io/gh/MohammadaminAlbooyeh/real_time_chat_app/branch/master/graph/badge.svg)](https://codecov.io/gh/MohammadaminAlbooyeh/real_time_chat_app)

---

## 🎯 Key Features

- ✅ **Real-Time Messaging** - WebSocket-based instant message delivery with sub-100ms latency
- ✅ **Multi-Platform Clients** - Web (React), Desktop (Electron), Mobile (React Native/Expo)
- ✅ **User Authentication** - JWT-based auth with refresh tokens and session management
- ✅ **Message Features** - Reactions, attachments, edit/delete, typing indicators
- ✅ **Advanced Search** - Full-text search powered by Elasticsearch
- ✅ **User Relationships** - Friend requests, relationships, user discovery
- ✅ **File Uploads** - Secure file handling with validation
- ✅ **Presence Tracking** - Online/offline status and last seen tracking
- ✅ **Async Tasks** - Background jobs via Celery (notifications, maintenance, indexing)
- ✅ **Monitoring & Observability** - Prometheus, Grafana, Jaeger tracing
- ✅ **Production Ready** - Docker, Nginx, security middleware, CI/CD

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
        ┌─────────┼─────────┬──────────────┐
        │         │         │              │
┌───────▼──────┐  │  ┌──────▼──────┐  ┌───▼──────────┐
│  API Service │  │  │WebSocket    │  │Search Service│
│  (FastAPI)   │  │  │Service      │  │(FastAPI)     │
│  Port: 8000  │  │  │(FastAPI)    │  │Port: 8002    │
│              │  │  │Port: 8001   │  │              │
└──────┬───────┘  │  └──────┬──────┘  └───┬──────────┘
       │          │         │              │
       │          │         │         ┌────▼──────────┐
       │          │         │         │ Elasticsearch │
       │          │         │         │   :9200       │
       │          │         │         └───────────────┘
       │          └─────────┼─────────┐
       │                    │         │
   ┌───▼────────────────────▼──┐  ┌──▼─────────┐
   │      PostgreSQL (DB)      │  │  Redis     │
   │   Primary Data Store      │  │ Cache/Pub-Sub
   │      Port: 5432           │  │ Port: 6379 │
   └───────────────────────────┘  └──────┬─────┘
                                         │
                                   ┌─────▼──────────┐
                                   │ Worker Service │
                                   │  (Celery)      │
                                   │ Async Tasks    │
                                   └────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                  MONITORING & OBSERVABILITY                      │
├──────────────────┬──────────────────┬──────────────────────────────┤
│   Prometheus     │     Grafana      │      Jaeger                  │
│   :9090          │     :3001        │      :16686                  │
└──────────────────┴──────────────────┴──────────────────────────────┘
```

---

## 📁 Project Structure

```
real-time-chat-app/
│
├── 📂 services/                    # Backend microservices
│   ├── api-service/               # REST API gateway
│   │   ├── routes/               # API endpoints (auth, users, rooms, messages, etc.)
│   │   ├── models/               # SQLAlchemy ORM models
│   │   ├── schemas/              # Pydantic request/response models
│   │   ├── middleware/           # Auth, CORS, error handling
│   │   ├── config.py             # Configuration management
│   │   ├── database.py           # Database connection
│   │   ├── main.py               # FastAPI app initialization
│   │   └── requirements.txt      # Dependencies
│   │
│   ├── websocket-service/        # Real-time WebSocket service
│   │   ├── message_handler.py   # WebSocket message processing
│   │   ├── connection_manager.py # Connection lifecycle
│   │   ├── main.py              # WebSocket server
│   │   └── requirements.txt
│   │
│   ├── search-service/           # Full-text search (Elasticsearch)
│   │   ├── routes.py            # Search endpoints
│   │   ├── elasticsearch_client.py
│   │   ├── models.py            # Index models
│   │   ├── main.py
│   │   └── requirements.txt
│   │
│   └── worker-service/           # Async task queue (Celery)
│       ├── tasks/               # Celery task definitions
│       ├── celery_beat.py      # Scheduled tasks
│       ├── main.py
│       └── requirements.txt
│
├── 📂 frontend/                    # Client applications
│   ├── web/                       # React web application
│   │   ├── src/
│   │   │   ├── pages/            # ChatPage, LoginPage, ProfilePage
│   │   │   ├── components/       # Reusable UI components
│   │   │   ├── store/            # Redux state management
│   │   │   ├── services/         # API client, WebSocket service
│   │   │   ├── hooks/            # Custom React hooks
│   │   │   ├── utils/            # Helper functions
│   │   │   ├── styles/           # CSS stylesheets
│   │   │   ├── main.jsx          # React entry point
│   │   │   └── App.jsx           # Root component
│   │   ├── public/               # Static assets
│   │   ├── vite.config.js        # Vite build config
│   │   ├── package.json
│   │   └── Dockerfile
│   │
│   ├── desktop/                   # Electron desktop application
│   │   ├── src/                  # React components (shared with web)
│   │   ├── main.js              # Electron main process
│   │   ├── preload.js           # IPC bridge
│   │   ├── package.json
│   │   └── Dockerfile
│   │
│   └── mobile/                    # React Native mobile application
│       ├── App.js               # Root component
│       ├── screens/             # Screen components
│       ├── navigation/          # React Navigation
│       ├── components/          # Reusable components
│       ├── services/            # API client
│       ├── app.json             # Expo configuration
│       └── package.json
│
├── 📂 database/                    # Database schema & migrations
│   ├── migrations/
│   │   ├── versions/            # Alembic migration files
│   │   ├── env.py
│   │   └── script.py.mako
│   ├── schema.sql               # Database schema
│   ├── indexes.sql              # Database indexes
│   └── seeds/                   # Seed data for development
│
├── 📂 shared/                      # Shared utilities
│   ├── models/                  # Shared data models
│   ├── schemas/                 # Common Pydantic schemas
│   ├── utils/                   # Utility functions
│   └── constants/               # Constants & enums
│
├── 📂 monitoring/                  # Monitoring & observability
│   ├── prometheus.yml           # Prometheus config
│   ├── grafana/
│   │   ├── dashboards/         # Grafana dashboard definitions
│   │   ├── provisioning/       # Data source configs
│   │   └── Dockerfile
│   └── jaeger/                  # Distributed tracing config
│
├── 📂 nginx/                       # Reverse proxy configuration
│   ├── nginx.conf               # Main Nginx config
│   ├── ssl/                     # SSL certificates
│   └── Dockerfile
│
├── 📂 scripts/                     # Utility scripts
│   ├── backup.sh                # Database backup
│   ├── deploy.sh                # Deployment script
│   ├── migration.sh              # Database migration
│   └── seed.sh                  # Seed data script
│
├── 📂 tests/                       # Test suite
│   ├── unit/                    # Unit tests
│   │   ├── test_auth.py
│   │   ├── test_models.py
│   │   ├── test_validators.py
│   │   └── test_utils.py
│   ├── integration/             # Integration tests
│   │   ├── test_api_endpoints.py
│   │   ├── test_websocket.py
│   │   ├── test_message_flow.py
│   │   └── test_database.py
│   ├── e2e/                     # End-to-end tests
│   │   ├── test_chat_flow.py
│   │   └── test_user_journey.py
│   └── conftest.py              # Pytest fixtures
│
├── 📂 .github/                     # GitHub configuration
│   └── workflows/
│       ├── ci.yml               # Continuous Integration
│       ├── cd.yml               # Continuous Deployment
│       └── security.yml         # Security scanning
│
├── 📂 docs/                        # Documentation
│   ├── ARCHITECTURE.md          # System design
│   ├── API_REFERENCE.md         # API endpoints
│   ├── INSTALLATION.md          # Setup instructions
│   ├── DEPLOYMENT.md            # Production deployment
│   ├── TROUBLESHOOTING.md       # Common issues
│   ├── WEBSOCKET_EVENTS.md     # WebSocket protocol
│   ├── MONITORING.md            # Observability setup
│   ├── SECURITY.md              # Security guidelines
│   ├── SCALING.md               # Performance & scaling
│   └── CONTRIBUTING.md          # Contribution guide
│
├── docker-compose.yml           # Development compose
├── docker-compose.dev.yml       # Dev overrides
├── Makefile                     # Build & run commands
├── .env.example                 # Environment variables template
├── .env.production              # Production variables
├── .dockerignore
├── .gitignore
├── version.txt                  # Current version
└── README.md                    # This file
```

---

## 🚀 Quick Start

### Prerequisites

- **Docker** 24+ & **Docker Compose** v2.20+
- **Node.js** 20+ (for local frontend development)
- **Python** 3.12+ (for local backend development)

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

### 3️⃣ Run Database Migrations

```bash
make migrate

# OR
docker compose exec api-service alembic upgrade head
```

### 4️⃣ Seed Sample Data (Optional)

```bash
make seed
```

### 5️⃣ Access Application

| Service | URL |
|---------|-----|
| 🌐 Web UI | http://localhost:3000 |
| 🔌 API Docs | http://localhost:8000/docs |
| 📊 Grafana | http://localhost:3001 |
| 🔍 Jaeger | http://localhost:16686 |
| 📈 Prometheus | http://localhost:9090 |

---

## 🛠 Tech Stack

### Backend
| Component | Technology | Purpose |
|-----------|-----------|---------|
| Framework | FastAPI + SQLAlchemy | REST API & WebSocket |
| Database | PostgreSQL 16 | Primary data store |
| Cache | Redis 7 | Session cache, pub/sub |
| Search | Elasticsearch 8 | Full-text search |
| Task Queue | Celery + Redis | Async task processing |
| Auth | JWT + python-jose | Stateless authentication |

### Frontend
| Layer | Technology | Purpose |
|-------|-----------|---------|
| Web | React 18 + Redux Toolkit | Web application |
| Desktop | Electron | Cross-platform desktop |
| Mobile | React Native + Expo | iOS/Android application |
| Bundler | Vite | Fast module bundling |
| UI State | Redux Toolkit | Centralized state |

### Infrastructure
| Component | Technology | Purpose |
|-----------|-----------|---------|
| Containers | Docker | Containerization |
| Orchestration | Docker Compose | Local dev & prod |
| Proxy | Nginx | Reverse proxy + SSL |
| Monitoring | Prometheus | Metrics collection |
| Visualization | Grafana | Dashboard visualization |
| Tracing | Jaeger | Distributed tracing |

### CI/CD
| Tool | Usage |
|------|-------|
| GitHub Actions | Automated testing, building, deploying |
| Codecov | Code coverage tracking |
| Docker Hub / GHCR | Container registry |

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
GET    /api/v1/users/me           # Get profile
PATCH  /api/v1/users/me           # Update profile
POST   /api/v1/users/me/change-password  # Change password
GET    /api/v1/users/search       # Search users
```

### Rooms
```
POST   /api/v1/rooms              # Create room
GET    /api/v1/rooms              # List rooms
GET    /api/v1/rooms/{id}         # Get room details
PATCH  /api/v1/rooms/{id}         # Update room
DELETE /api/v1/rooms/{id}         # Delete room
```

### Messages
```
POST   /api/v1/messages           # Send message
GET    /api/v1/rooms/{id}/messages  # List messages
PATCH  /api/v1/messages/{id}      # Edit message
DELETE /api/v1/messages/{id}      # Delete message
```

📖 **[Full API Documentation](docs/API_REFERENCE.md)**

---

## 🔌 WebSocket Events

Real-time events are transmitted via WebSocket at `ws://localhost:8001/ws`:

```javascript
// Client → Server
{ "type": "message:send", "data": { "room_id": "...", "content": "..." } }
{ "type": "typing:start", "data": { "room_id": "..." } }
{ "type": "room:join", "data": { "room_id": "..." } }

// Server → Client
{ "type": "message:new", "data": { "id": "...", "content": "...", ... } }
{ "type": "user:typing", "data": { "user_id": "...", "room_id": "..." } }
{ "type": "user:online", "data": { "user_id": "..." } }
```

📖 **[WebSocket Protocol Details](docs/WEBSOCKET_EVENTS.md)**

---

## 📊 Development Commands

```bash
# Build & start services
make dev              # Development with hot-reload
make up               # Start services in background
make down             # Stop services
make build            # Build images

# Database
make migrate          # Run migrations
make seed             # Seed sample data

# Testing
make test             # Run API tests
docker compose exec api-service pytest --cov

# Cleanup
make clean            # Remove containers & volumes
make logs             # View service logs
```

See **[Makefile](Makefile)** for all available commands.

---

## 🧪 Testing

The project includes comprehensive test coverage:

```bash
# Run all tests
docker compose exec api-service pytest --cov=. --cov-report=html

# Run specific test suite
docker compose exec api-service pytest tests/unit/
docker compose exec api-service pytest tests/integration/
docker compose exec api-service pytest tests/e2e/

# Run linting
docker compose exec api-service ruff check .

# Run type checking
docker compose exec api-service mypy .
```

**Test Coverage:**
- ✅ Unit tests: Auth, models, validators, utilities
- ✅ Integration tests: Database, WebSocket, API endpoints, message flow
- ✅ E2E tests: Complete user journeys and chat flows

---

## 🔐 Security Features

- ✅ **JWT Authentication** - Secure token-based auth with expiration
- ✅ **Password Hashing** - bcrypt with salt
- ✅ **CORS Protection** - Configurable allowed origins
- ✅ **Rate Limiting** - Request throttling
- ✅ **SQL Injection Prevention** - Parameterized queries via SQLAlchemy
- ✅ **XSS Protection** - Input validation & sanitization
- ✅ **HTTPS/TLS** - Nginx SSL/TLS termination
- ✅ **File Upload Validation** - Type & size verification

📖 **[Security Guidelines](docs/SECURITY.md)**

---

## 📈 Monitoring & Observability

The application includes production-grade observability:

- **Prometheus** - Metrics collection (requests, latency, errors)
- **Grafana** - Visual dashboards for real-time monitoring
- **Jaeger** - Distributed tracing for request flow analysis
- **Structured Logging** - JSON logs for easy parsing

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

# Run migrations
docker compose exec api-service alembic upgrade head
```

### Docker Swarm / Kubernetes
The application is containerized and can be deployed to any orchestration platform.

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

**Made with ❤️ as a production-grade real-time chat platform**
