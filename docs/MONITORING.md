# Monitoring Guide

## Overview

`docker-compose.yml` wires up a monitoring stack alongside the four Java services:

| Component | Purpose | Port |
|-----------|---------|------|
| **Prometheus** | Metrics collection and alerting | 9090 |
| **Grafana** | Metrics visualization and dashboards | 3001 (container port 3000) |
| **Jaeger** | Distributed tracing | UI 16686, OTLP 4318 |
| **Sentry** | Error tracking (config only, see below) | - |

**Important caveat, verified against the code:** none of the four Spring Boot services (`api-service`, `websocket-service`, `worker-service`, `search-service`) currently depend on Micrometer, Spring Boot Actuator, the OpenTelemetry Java agent, or the Sentry Java SDK (checked every `pom.xml` under `services/*/pom.xml` — none list those artifacts). That means:
- There is **no `/metrics` endpoint** exposed by any service today, despite `monitoring/prometheus.yml` listing scrape targets for them.
- There is **no distributed tracing instrumentation** sending spans to Jaeger today, despite `monitoring/jaeger-config.yml` existing.
- There is **no Sentry SDK initialization** in any service, despite `monitoring/sentry.conf` existing.

The config files below describe the *intended* wiring and are useful scaffolding, but as of this writing the only real, working health signals are the custom `HealthController` (`GET /health`, `GET /health/db`) in `api-service` and `GET /api/v1/search/health` in `search-service`. `websocket-service` and `worker-service` have **no health endpoint at all today** (verified: no controller or route matching `/health` exists in either service's source) — their only observable liveness signal is `docker compose ps`/container state and their stdout logs. Treat the Prometheus/Jaeger/Sentry sections as "infrastructure ready, instrumentation pending" rather than "live today." Adding Micrometer (`spring-boot-starter-actuator` + `micrometer-registry-prometheus`) to each service's `pom.xml` and exposing `/actuator/prometheus` is the natural next step to make the Prometheus section accurate.

## Prometheus

### Configuration

`monitoring/prometheus.yml` (real, as committed):

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: "api-service"
    static_configs:
      - targets: ["api-service:8000"]

  - job_name: "websocket-service"
    static_configs:
      - targets: ["websocket-service:8001"]

  - job_name: "search-service"
    static_configs:
      - targets: ["search-service:8002"]

  - job_name: "postgres"
    static_configs:
      - targets: ["postgres-exporter:9187"]

  - job_name: "redis"
    static_configs:
      - targets: ["redis-exporter:9121"]

rule_files:
  - "alerting-rules.yml"
```

The ports (8000/8001/8002) do match each service's real `server.port` in `application.yml`. However:
- None of the three services actually expose Prometheus-format metrics at those ports yet (see caveat above) — Prometheus will scrape them and get connection errors or 404s, not metrics, until instrumentation is added.
- `postgres-exporter` and `redis-exporter` are **not** defined as services in `docker-compose.yml` — those scrape targets will simply fail to resolve/connect until those exporter containers are actually added.

### Alerting Rules

`monitoring/alerting-rules.yml` (real, as committed):

| Alert | Condition | Severity |
|-------|-----------|----------|
| `HighErrorRate` | `rate(http_requests_total{status=~"5.."}[5m]) > 0.05` | Critical |
| `HighLatency` | p95 of `http_request_duration_seconds_bucket` over 5m > 1s | Warning |
| `ServiceDown` | `up == 0` for 1 min | Critical |
| `LowWebSocketConnections` | `websocket_active_connections < 10` for 5 min | Warning |
| `DatabaseConnectionHigh` | `pg_stat_activity_count > 50` for 5 min | Warning |

These rules reference metric names (`http_requests_total`, `websocket_active_connections`, `pg_stat_activity_count`, etc.) that nothing in the current codebase emits — they will never fire as written until the corresponding exporters/instrumentation exist. They're a reasonable target shape for when Actuator/Micrometer is added.

## Grafana

### Access

- URL: `http://localhost:3001` (container listens on 3000, mapped to host 3001 per `docker-compose.yml`)
- Image is built from `monitoring/grafana/Dockerfile`, with provisioning under `monitoring/grafana/provisioning/` (`datasources.yml`, `dashboards.yml`) pointing Grafana at Prometheus (`http://prometheus:9090`) and auto-loading the JSON dashboards in `monitoring/grafana/dashboards/`.

### Pre-provisioned dashboard files (present in the repo)

- `system-health.json`
- `api-performance.json`
- `websocket-connections.json`
- `message-throughput.json`
- `database-metrics.json`

These dashboard JSON files query the same not-yet-emitted metric names described above, so they will render empty/"No data" panels until the services are instrumented (or until `postgres-exporter`/`redis-exporter` containers backing the Postgres/Redis panels are added to `docker-compose.yml`).

## Jaeger (Distributed Tracing)

`monitoring/jaeger-config.yml` (real, as committed) configures a Jaeger client with a constant (100%) sampler and a collector endpoint, and `docker-compose.yml` runs `jaegertracing/all-in-one` with the OTLP collector enabled (`COLLECTOR_OTLP_ENABLED=true`, port 4318) and the UI on 16686.

- UI: `http://localhost:16686`
- OTLP HTTP endpoint: `http://jaeger:4318` (reachable by other containers on `chat-network`)

As noted above, no service currently ships an OpenTelemetry/Jaeger client dependency, so no traces are actually produced yet — `jaeger-config.yml` describes the intended client config for whichever service(s) you instrument, not an active integration.

## Sentry (Error Tracking)

`monitoring/sentry.conf` (real, as committed):

```ini
[sentry]
dsn = https://your-sentry-dsn@sentry.io/your-project-id
environment = production
traces_sample_rate = 0.2
profiles_sample_rate = 0.1
send_default_pii = false
```

This is a template for whichever Sentry SDK you wire in — no service currently depends on `sentry-spring-boot-starter` or any Sentry Java artifact, so no errors are actually reported to Sentry today. To make this real, add `io.sentry:sentry-spring-boot-starter` to a service's `pom.xml`, point `sentry.dsn` at your real DSN (via `SENTRY_DSN` env var), and Spring Boot's auto-configuration picks it up.

## Logging

All services log via SLF4J/Logback (Spring Boot's default), controlled per service by the `LOG_LEVEL` env var mapped in `application.yml`, e.g.:

```yaml
logging:
  level:
    com.chatapp.api: ${LOG_LEVEL:INFO}
```

Logs go to stdout/stderr by default, which is Docker-friendly (`docker compose logs -f <service>`), but there is no structured JSON log format or log aggregation (ELK/Loki) configured in this repo — that would be an addition, not an existing feature.

## Current, Working Health Checks

Until the above instrumentation is added, use these real endpoints to check service health:

| Service | Endpoint | Returns |
|---------|----------|---------|
| api-service | `GET /health` | `{"status":"ok","service":"api-service"}` |
| api-service | `GET /health/db` | DB connectivity check via `SELECT 1` |
| search-service | `GET /api/v1/search/health` | `{"status":"ok","service":"search-service"}` |
| websocket-service | *(none)* | no `/health` endpoint exists — check `docker compose ps` / logs instead |
| worker-service | *(none)* | no `/health` endpoint exists — check `docker compose ps` / logs instead |
| postgres | `pg_isready -U postgres` (Docker healthcheck) | used by `docker-compose.yml`'s `condition: service_healthy` |
| elasticsearch | `GET http://localhost:9200` | cluster info |

## Production Monitoring Checklist

- [ ] Add Micrometer + Prometheus registry (or Spring Boot Actuator) to each service so `monitoring/prometheus.yml`'s scrape targets actually return metrics
- [ ] Add `postgres-exporter` and `redis-exporter` containers to `docker-compose.yml` if you want the Postgres/Redis panels and alerts to work
- [ ] Add an OpenTelemetry/Jaeger client to the services you want traced
- [ ] Add the Sentry Spring Boot starter and a real `SENTRY_DSN` if you want error tracking
- [ ] Once metrics exist: verify Prometheus targets show `UP`, Grafana dashboards render data, and alerting rules fire in a test
- [ ] Set up log aggregation (ELK, Loki, etc.) if centralized logs are needed beyond `docker compose logs`
