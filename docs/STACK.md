# Stack

## Application

- **Java**: 25 (LTS in this repo’s context)
- **Spring Boot**: 4.0.1
- **Tracing**: Micrometer Tracing + OpenTelemetry (via Spring Boot’s OpenTelemetry starter)
- **Protocols**:
  - **GraphQL** entry point
  - **REST** service-to-service calls
  - **RabbitMQ** for async fan-out
- **Logging**: JSON logs with trace/span correlation

## Observability / infrastructure

- **Grafana**: UI for exploration
- **Tempo**: trace storage (OTLP receiver)
- **Loki**: log storage (optional in this demo depending on configuration)
- **RabbitMQ**: message broker
- **Docker Compose**: runs the infra stack locally

