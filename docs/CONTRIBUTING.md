# Contributing Guide

## Development Setup

### Prerequisites

- Java 21
- Maven 3.9+
- Node.js 20+
- Docker and Docker Compose
- PostgreSQL 16 (optional, Docker provides it)
- Redis 7 (optional, Docker provides it)

### Quick Start

```bash
# Clone the repository
git clone https://github.com/MohammadaminAlbooyeh/real_time_chat_app.git
cd real_time_chat_app

# Copy environment files
cp .env.example services/api-service/.env
cp .env.example services/websocket-service/.env
cp .env.example services/worker-service/.env
cp .env.example services/search-service/.env

# Start all services with hot reload
make dev
```

There is no `make migrate` or `make seed` — the schema loads automatically into a fresh Postgres volume from `database/schema.sql`/`indexes.sql`. See [INSTALLATION.md](INSTALLATION.md) for complete setup instructions.

## Code Style

### Java (Backend Services)

All four services (`api-service`, `websocket-service`, `worker-service`, `search-service`) are Spring Boot 3.5 apps on Java 21, built with Maven. There is no Checkstyle/Spotless/formatter plugin configured in any `pom.xml` today, so style is enforced by convention and review, not by a build-breaking linter:

- **Naming:** `camelCase` for methods/fields/locals, `PascalCase` for classes/interfaces, `UPPER_SNAKE_CASE` for constants
- **Packages:** `com.chatapp.<service>.{controller,service,repository,model,dto,config,security}` — follow the existing package layout per service
- **Controllers:** thin `@RestController` classes that delegate to a `@Service`; request/response shapes are dedicated `record`/class DTOs in `dto/`, not entities
- **Persistence:** Spring Data JPA repositories (`interface XRepository extends JpaRepository<...>`); entities under `model/` extend `BaseEntity`
- **Validation:** `jakarta.validation` annotations (`@Valid`, `@NotBlank`, etc.) on request DTOs
- **Line length:** keep it reasonable (~120 chars); no enforced limit today

**Key conventions from the codebase (api-service):**
```java
@RestController
@RequestMapping("/api/v1/rooms/{roomId}/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable UUID roomId,
            @Valid @RequestBody MessageSend payload,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(messageService.sendMessage(roomId, payload, user));
    }
}
```

### Dependencies

- Each service manages its own dependencies in its own `pom.xml` (no shared parent POM beyond `spring-boot-starter-parent`)
- Pin new dependency versions explicitly; don't rely on `LATEST`
- Review new dependencies for known CVEs before adding them (e.g. `mvn dependency-check:check` if the plugin is added, or check manually — nothing automated is wired in today)

### JavaScript/React (Frontend)

- **Formatter:** Prettier with default configuration
- **Linter:** ESLint with React plugin
- **Naming:**
  - `PascalCase` for components and files
  - `camelCase` for functions, variables, hooks
  - `UPPER_SNAKE_CASE` for constants
- **Component style:** Functional components with hooks (no class components)
- **State management:** Redux Toolkit slices for global state (`@reduxjs/toolkit`, `react-redux`)
- **Styling:** CSS modules or inline `style` objects (no Tailwind)
- `frontend/web` is Vite + React 18; `frontend/desktop` wraps the same React app in Electron; `frontend/mobile` is Expo/React Native

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
│   ├── api-service/          # REST API (Spring Boot, Java 21)
│   ├── websocket-service/    # WebSocket server (Spring Boot, Java 21)
│   ├── search-service/       # Search API (Spring Boot + Elasticsearch client)
│   └── worker-service/       # Scheduled maintenance jobs (Spring Boot, @Scheduled)
├── frontend/
│   ├── web/                  # React web app (Vite)
│   ├── desktop/              # Electron desktop app (same React UI)
│   └── mobile/               # React Native mobile app (Expo)
├── database/                 # schema.sql, indexes.sql, seeds — no migration tool
├── monitoring/               # Prometheus, Grafana, Jaeger configs
├── nginx/                    # Reverse proxy configuration
├── tests/                    # Integration and E2E tests
├── docs/                     # Documentation
├── docker-compose.yml        # Base compose (all services + infra)
└── docker-compose.dev.yml    # Development overrides (mounted sources, dev profile)
```

Each service also has its own `src/test/java` suite (JUnit 5 via `spring-boot-starter-test`).

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
test(worker): add scheduled-task tests
```

### Pull Request Process

1. Create a feature/fix branch from `develop`
2. Make your changes, ensuring `mvn test` (backend) and `npm run lint`/`npm run build` (frontend) pass
3. Write or update tests for new functionality
4. Update documentation if needed (API docs, README, etc.)
5. Create a PR to `develop` with a clear description
6. Request review from at least one team member
7. Squash-merge into `develop`

### PR Checklist

- [ ] Code follows the conventions above
- [ ] New/changed endpoints have request validation (`jakarta.validation`) and DTOs
- [ ] Tests pass (`mvn test` per service / `npm test` for frontend)
- [ ] Documentation updated
- [ ] No hardcoded secrets or credentials
- [ ] Schema changes to `database/schema.sql`/`indexes.sql` are additive/backward-compatible (there's no migration tool to roll them out safely otherwise)
- [ ] API changes are backward-compatible or versioned (`/api/v1/...`)

## Testing

### Backend Testing

Each service has its own JUnit 5 test suite under `src/test/java`, using `spring-boot-starter-test` (and `spring-security-test` where relevant).

```bash
# Run tests for a specific service
cd services/api-service
mvn test

# Or via the Makefile
make test-api
make test-search
make test-worker
make test-ws
make test-all
```

**Test requirements:**
- Unit tests for new service/business logic
- Controller tests for new or changed endpoints
- No async/Celery-style test markers apply anymore — tests are plain JUnit 5

### E2E Testing

End-to-end tests live in `tests/e2e/`. Check that directory's own README/setup for how they're currently run — they are not wired into `make test-*`.

### Frontend Testing

```bash
cd frontend/web
npm test
```

## Documentation

- API changes require updating `docs/API_REFERENCE.md` (and `API_DOCS.md` at the repo root if it documents the same endpoints)
- New WebSocket events require updating `docs/WEBSOCKET_EVENTS.md`
- Architecture changes require updating `ARCHITECTURE.md` and `docs/ARCHITECTURE.md`
- Keep `docs/README.md` index up to date

## Security

- Never commit secrets, API keys, or passwords
- Use environment variables for all configuration
- Run `npm audit` on frontend dependencies
- Validate all user inputs on the server side (`jakarta.validation` on DTOs)
- Use Spring Data JPA / parameterized queries — never raw string interpolation into SQL

## Getting Help

- Open an issue on GitHub for bugs or feature requests
- Review [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for common development issues
- Ask in the team's communication channel
