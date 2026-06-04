# Monitoring Guide

## Overview

The application includes a comprehensive monitoring stack:

| Component | Purpose | Port |
|-----------|---------|------|
| **Prometheus** | Metrics collection and alerting | 9090 |
| **Grafana** | Metrics visualization and dashboards | 3001 |
| **Jaeger** | Distributed tracing | 16686 |
| **Sentry** | Error tracking | - |

## Prometheus

### Configuration

Prometheus configuration is in `monitoring/prometheus.yml`:

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
```

### Metrics Endpoints

Each FastAPI service exposes Prometheus metrics via the `prometheus-fastapi-instrumentator` or manual instrumentation:

**API Service** (`http://api-service:8000/metrics`):
- `http_requests_total` - Total HTTP requests by method, path, status
- `http_request_duration_seconds` - Request duration histogram
- `http_requests_in_progress` - Currently in-flight requests

**WebSocket Service** (`http://websocket-service:8001/metrics`):
- `websocket_active_connections` - Current active connections
- `websocket_messages_sent_total` - Messages sent through WebSocket
- `websocket_messages_received_total` - Messages received through WebSocket
- `websocket_connection_duration_seconds` - Connection duration histogram
- `websocket_errors_total` - WebSocket error count

**Search Service** (`http://search-service:8002/metrics`):
- `search_requests_total` - Total search requests
- `search_request_duration_seconds` - Search latency
- `elasticsearch_index_size_bytes` - ES index size

### Alerting Rules

Alerting rules are defined in `monitoring/alerting-rules.yml`:

| Alert | Condition | Severity | Description |
|-------|-----------|----------|-------------|
| `HighErrorRate` | 5xx rate > 5% over 5 min | Critical | HTTP error rate threshold exceeded |
| `HighLatency` | p95 latency > 1s over 5 min | Warning | Response times degrading |
| `ServiceDown` | `up == 0` for 1 min | Critical | Service is unreachable |
| `LowWebSocketConnections` | Connections < 10 for 5 min | Warning | Unusually low connection count |
| `DatabaseConnectionHigh` | Connections > 50 for 5 min | Warning | Database connection pool near limit |

Alerts are evaluated every 15 seconds (Prometheus `evaluation_interval`).

## Grafana

### Access

- URL: `http://localhost:3001` (mapped to Grafana port 3000)
- Default credentials: `admin` / `admin`

### Provisioning

Dashboards and data sources are auto-provisioned on startup:

**Data Sources** (`monitoring/grafana/provisioning/datasources.yml`):
- **Prometheus** - `http://prometheus:9090` (default source)

**Dashboard Provider** (`monitoring/grafana/provisioning/dashboards.yml`):
- Loads dashboards from `/var/lib/grafana/dashboards`

### Pre-Provisioned Dashboards

**1. System Health** (`monitoring/grafana/dashboards/system-health.json`)
- CPU and memory usage per container
- Service up/down status
- Database connection count
- Redis memory usage
- Disk I/O

**2. API Performance** (`monitoring/grafana/dashboards/api-performance.json`)
- Request rate by endpoint (rps)
- Response time percentiles (p50, p95, p99)
- Error rate by status code
- Request distribution across services
- Slowest endpoints

**3. WebSocket Connections** (`monitoring/grafana/dashboards/websocket-connections.json`)
- Active connections over time
- Connection rate (connects/disconnects per second)
- Messages per second
- Connection duration distribution
- Room-level connection counts

**4. Message Throughput** (`monitoring/grafana/dashboards/message-throughput.json`)
- Messages sent per second
- Messages by type (text, image, file, system)
- Peak messaging times
- Average message size
- Attachment upload rate

**5. Database Metrics** (`monitoring/grafana/dashboards/database-metrics.json`)
- Query execution time
- Active connections
- Transactions per second
- Cache hit ratio
- Table size and index usage
- Slow query monitoring

### Creating Custom Dashboards

1. Log in to Grafana at `http://localhost:3001`
2. Navigate to Dashboards → New Dashboard
3. Add panels with PromQL queries
4. Export as JSON and save to `monitoring/grafana/dashboards/`

**Useful PromQL queries:**
```promql
# Request rate per endpoint
rate(http_requests_total{job="api-service"}[5m])

# Error rate
rate(http_requests_total{status=~"5.."}[5m]) / rate(http_requests_total[5m]) * 100

# p95 latency
histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))

# Active WebSocket connections
websocket_active_connections{job="websocket-service"}

# Database connections
pg_stat_activity_count{job="postgres"}
```

## Jaeger (Distributed Tracing)

### Configuration

Jaeger configuration is in `monitoring/jaeger-config.yml`:

```yaml
service:
  name: real-time-chat
  version: 1.0.0

sampler:
  type: const
  param: 1

reporter:
  queueSize: 100
  bufferFlushInterval: 1

collector:
  endpoint: http://jaeger:14268/api/traces
```

### Access

- UI: `http://localhost:16686`
- OTLP HTTP endpoint: `http://jaeger:4318`

### Traced Operations

The following operations are traced across services:
- HTTP request lifecycle (incoming request → database query → response)
- WebSocket message flow (client → server → broadcast)
- Celery task execution (enqueue → process → complete)
- Redis pub/sub event propagation
- Elasticsearch indexing and search operations

### Viewing Traces

1. Open Jaeger UI at `http://localhost:16686`
2. Select service: `real-time-chat`
3. Choose operation, tags, or time range
4. Click "Find Traces"

Each trace shows:
- Service-to-service call timing
- Database query duration
- Redis operation timing
- Error locations and stack traces

## Sentry (Error Tracking)

### Configuration

Sentry configuration is in `monitoring/sentry.conf`. Each service initializes the Sentry SDK with its DSN:

```python
import sentry_sdk
from sentry_sdk.integrations.fastapi import FastApiIntegration
from sentry_sdk.integrations.sqlalchemy import SqlalchemyIntegration

sentry_sdk.init(
    dsn="https://your-dsn@sentry.io/project-id",
    integrations=[
        FastApiIntegration(),
        SqlalchemyIntegration(),
    ],
    traces_sample_rate=1.0,
    environment="production",
)
```

### Events Captured

- Unhandled exceptions (500 errors)
- Slow requests (>2s)
- Database query errors
- WebSocket disconnection errors
- Celery task failures
- Elasticsearch errors
- JWT validation failures

### Sentry Alerts

Configure alerts in Sentry for:
- Error rate spikes (>1% of requests)
- New error types appearing
- Critical errors (database connection failures, auth failures)
- Performance degradation (p95 > 3s)

## Logging

All services log to stdout (Docker-friendly) with structured JSON format:

```json
{
  "timestamp": "2025-06-04T12:00:00Z",
  "level": "INFO",
  "service": "api-service",
  "request_id": "uuid",
  "method": "GET",
  "path": "/api/v1/rooms",
  "status": 200,
  "duration_ms": 45,
  "user_id": "uuid"
}
```

### Log Levels

- **ERROR** - Application errors requiring immediate attention
- **WARNING** - Potential issues (slow queries, retries)
- **INFO** - Normal operations (request completion, user actions)
- **DEBUG** - Development details (only in development mode)

## Production Monitoring Checklist

- [ ] Prometheus targets all show `UP` status
- [ ] Grafana dashboards display data correctly
- [ ] Alerting rules configured and tested
- [ ] Alert notifications configured (email, Slack, PagerDuty)
- [ ] Jaeger traces visible for all services
- [ ] Sentry capturing errors correctly
- [ ] Log aggregation set up (ELK, Loki, etc.)
- [ ] Metrics retention configured (default: 15 days Prometheus)
- [ ] Dashboard backups scheduled
- [ ] Performance baselines established
