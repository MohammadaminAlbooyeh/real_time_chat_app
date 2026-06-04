# Contributing Guide

## Development Setup

### Prerequisites

- Python 3.12+
- Node.js 20+
- Docker and Docker Compose
- PostgreSQL 16 (optional, Docker provides it)
- Redis 7 (optional, Docker provides it)

### Quick Start

```bash
# Clone the repository
git clone https://github.com/yourorg/real-time-chat-app.git
cd real-time-chat-app

# Copy environment files
cp .env.example services/api-service/.env
cp .env.example services/websocket-service/.env
cp .env.example services/worker-service/.env
cp .env.example services/search-service/.env

# Start all services with hot reload
make dev

# Run migrations
make migrate

# Seed demo data (if available)
make seed
```

See [INSTALLATION.md](INSTALLATION.md) for complete setup instructions.

## Code Style

### Python (Backend Services)

All Python services follow the same style guidelines:

- **Formatter:** [Ruff](https://docs.astral.sh/ruff/) with default settings
- **Type hints:** Required for all function signatures and public methods
- **Imports:** Organized by standard library → third-party → local, separated by blank lines
- **Naming:**
  - `snake_case` for functions, variables, methods
  - `PascalCase` for classes, models, enums
  - `UPPER_CASE` for constants
  - Prefix private methods/functions with `_`
- **Line length:** 100 characters max
- **Docstrings:** Optional but encouraged for public APIs and complex logic

**Linting check:**
```bash
ruff check .
```

**Key conventions from the codebase:**
```python
# Route handlers use async/await with type hints
@router.get("/example", response_model=ExampleResponse)
async def get_example(
    param: str = Query(...),
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    ...

# Models use SQLAlchemy 2.0 Mapped notation
class Example(Base, TimestampMixin, UUIDMixin):
    __tablename__ = "examples"
    name: Mapped[str] = mapped_column(String(100), nullable=False)

# Schemas use Pydantic v2 BaseModel
class ExampleCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=100)
```

### Python Version & Dependencies

- Python 3.12+
- Dependencies pinned in `requirements.txt` per service
- Use `pip freeze` to update requirements after adding dependencies
- All new dependencies must be reviewed for security (run `safety check`)

### JavaScript/React (Frontend)

- **Formatter:** Prettier with default configuration
- **Linter:** ESLint with React plugin
- **Naming:**
  - `PascalCase` for components and files
  - `camelCase` for functions, variables, hooks
  - `UPPER_SNAKE_CASE` for constants
- **Component style:** Functional components with hooks (no class components)
- **State management:** Redux Toolkit slices for global state
- **Styling:** CSS modules or inline `style` objects (no Tailwind)

**Frontend checks:**
```bash
cd frontend/web
npm run lint
npm run build
```

## Project Structure

```
root/
├── services/
│   ├── api-service/          # REST API (FastAPI)
│   ├── websocket-service/    # WebSocket server (FastAPI)
│   ├── search-service/       # Search API (FastAPI + ES)
│   └── worker-service/       # Async worker (Celery)
├── frontend/
│   ├── web/                  # React web app
│   ├── desktop/              # Electron desktop app
│   └── mobile/               # React Native mobile app
├── shared/                   # Shared Python library
├── database/                 # Migrations, schema, seeds
├── monitoring/               # Prometheus, Grafana, Jaeger configs
├── nginx/                    # Reverse proxy configuration
├── tests/                    # Integration and E2E tests
├── docs/                     # Documentation
├── docker-compose.yml        # Production compose
└── docker-compose.dev.yml    # Development overrides
```

## Workflow

### Branching Strategy

- `main` - Stable, production-ready code
- `develop` - Integration branch for features
- `feature/*` - New features (branched from `develop`)
- `fix/*` - Bug fixes
- `release/*` - Release preparation

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]
```

**Types:** `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `perf`

**Examples:**
```
feat(api): add user search endpoint
fix(ws): handle reconnection race condition
docs(api): document message reactions endpoint
test(worker): add notification task tests
```

### Pull Request Process

1. Create a feature/fix branch from `develop`
2. Make your changes, ensuring tests pass and lint is clean
3. Write or update tests for new functionality
4. Update documentation if needed (API docs, README, etc.)
5. Create a PR to `develop` with a clear description
6. Request review from at least one team member
7. All CI checks must pass before merging
8. Squash-merge into `develop`

### PR Checklist

- [ ] Code follows style guidelines
- [ ] Type hints are complete
- [ ] Tests pass (pytest / npm test)
- [ ] Lint passes (ruff / eslint)
- [ ] Documentation updated
- [ ] No hardcoded secrets or credentials
- [ ] Database migrations are backward-compatible
- [ ] API changes are backward-compatible or versioned

## Testing

### Backend Testing

Each service has its own test suite using `pytest` with `pytest-asyncio`.

```bash
# Run all tests
make test

# Run tests for a specific service
docker compose exec api-service pytest

# Run with coverage
docker compose exec api-service pytest --cov=. --cov-report=term

# Run specific test file
docker compose exec api-service pytest tests/test_auth.py
```

**Test requirements:**
- Unit tests for all new route handlers and business logic
- Integration tests for database operations
- Async tests must use `pytest-asyncio` with `@pytest.mark.asyncio`

### E2E Testing

End-to-end tests are in `tests/e2e/`:

```bash
docker compose exec api-service pytest tests/e2e/
```

### Frontend Testing

```bash
cd frontend/web
npm test
```

## Documentation

- API changes require updating `/API_DOCS.md` and `docs/API_REFERENCE.md`
- New WebSocket events require updating `docs/WEBSOCKET_EVENTS.md`
- Architecture changes require updating `ARCHITECTURE.md` and `docs/ARCHITECTURE.md`
- Keep `docs/README.md` index up to date

## Security

- Never commit secrets, API keys, or passwords
- Use environment variables for all configuration
- Run `safety check` on Python dependencies weekly
- Run `npm audit` on frontend dependencies
- Validate all user inputs on the server side
- Use parameterized queries (SQLAlchemy ORM) — never raw string interpolation

## Getting Help

- Open an issue on GitHub for bugs or feature requests
- Review [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for common development issues
- Ask in the team's communication channel
