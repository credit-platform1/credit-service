# credit-service

## Tests

| Type           | What is tested                                 |
| -------------- | ---------------------------------------------- |
| Unit           | StatusTransitionPolicy, DTO mapping            |
| WebMvc         | API contracts, validation, errors              |
| DataJpaTest    | Entity, version, indexes                       |
| Testcontainers | PostgreSQL + Liquibase                         |
| Testcontainers Kafka | Outbox publisher and consumer            |

## Observability

### Health Checks

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Combined health (liveness + readiness) |
| `GET /actuator/health/liveness` | Kubernetes liveness probe |
| `GET /actuator/health/readiness` | Kubernetes readiness probe |
| `GET /actuator/prometheus` | Prometheus metrics |
| `GET /actuator/metrics` | Query individual metrics |

### Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `credit.applications.created` | Counter | Total credit applications created |
| `credit.applications.cancelled` | Counter | Total credit applications cancelled |
| `credit.applications.status.changed` | Counter | Total status changes (tags: from, to) |
| `credit.outbox.events.published` | Counter | Total outbox events published (tag: eventType) |
| `credit.outbox.events.retried` | Counter | Total outbox retries (tag: eventType) |
| `credit.outbox.events.failed` | Counter | Total outbox failures (tag: eventType) |
| `credit.outbox.publish.duration` | Timer | Outbox publish duration (seconds) |

All metrics have tag `application=credit-service`. No high-cardinality tags (applicationId, correlationId, customerInn).

### Tracing

- **Propagation:** W3C Trace Context
- **Bridge:** Micrometer Tracing → OpenTelemetry
- **Exporter:** OTLP (`http://localhost:4318/v1/traces`)
- **Kafka:** TracingProducerInterceptor / TracingConsumerInterceptor
- **MDC:** `traceId`, `spanId`, `correlationId` in logs

### Correlation ID

Flow: `Client → X-Correlation-Id → CorrelationIdFilter → MDC → Response header`

- Filter reads `X-Correlation-Id` from request, generates UUID if absent
- Stores in MDC and baggage for trace context propagation
- Returns in response header
- Cleaned up after request

### Local Observability Stack

1. Start infrastructure:
```bash
cd credit-platform-deploy
docker compose --profile observability up -d
```

2. Check Prometheus: http://localhost:9090
3. Check Grafana: http://localhost:3000 (admin/admin)

### Useful PromQL Queries

```promql
# Applications created rate
increase(credit_applications_created_total[5m])

# Outbox published events
increase(credit_outbox_events_published_total[5m])

# Outbox failures
increase(credit_outbox_events_failed_total[5m])

# Outbox publish duration (avg)
rate(credit_outbox_publish_duration_seconds_sum[5m])
/
rate(credit_outbox_publish_duration_seconds_count[5m])
```

### How to Run Locally

```bash
# Start infra + services
docker compose --profile infra --profile services up -d

# Run tests
./mvnw verify

# Check health
curl http://localhost:8080/actuator/health

# Check metrics
curl http://localhost:8080/actuator/prometheus | grep credit_
```
