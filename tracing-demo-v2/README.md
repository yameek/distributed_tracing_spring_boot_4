# Distributed Tracing Demo v2 - Spring Boot 4.0.1 with Java 25 LTS

A production-ready implementation of distributed tracing using Spring Boot 4.0.1, Java 25 LTS, Micrometer Tracing, OpenTelemetry, Tempo, and Loki.

> ✅ **Migrated to Java 25 LTS** (January 2026)
> 🆕 **Now using @Observed annotation** for cleaner, more maintainable tracing code

## 🆕 Latest Updates

- **@Observed Annotation Migration** - All services migrated from manual span creation to `@Observed` annotation
  - 📖 [Migration Guide](./OBSERVED_ANNOTATION_MIGRATION_GUIDE.md) - Complete migration guide with examples
  - ⚡ [Quick Reference](./OBSERVED_QUICK_REFERENCE.md) - Quick reference for developers
  - 📊 [Migration Summary](./MIGRATION_SUMMARY.md) - What changed and why

## Documentation

📚 **All detailed documentation has been moved to** [`../docs/`](../docs/)

- **Quick Start**: [`docs/tracing-demo/tracing_quick_reference.md`](../docs/tracing-demo/tracing_quick_reference.md)
- **Demo Script**: [`docs/tracing-demo/tracing_demo_script.md`](../docs/tracing-demo/tracing_demo_script.md)
- **Implementation Guide**: [`docs/tracing-demo/tracing_comprehensive_implementation_guide.md`](../docs/tracing-demo/tracing_comprehensive_implementation_guide.md)
- **Architecture Diagrams**: [`docs/tracing-demo/tracing_architecture_diagrams.md`](../docs/tracing-demo/tracing_architecture_diagrams.md)
- **Migration Notes**: [`docs/migration/tracing_java_25_migration.md`](../docs/migration/tracing_java_25_migration.md)
- **Complete Documentation Index**: [`docs/README.md`](../docs/README.md)
- **🆕 OpenTelemetry Collector Guide**: [`COLLECTOR_GUIDE.md`](COLLECTOR_GUIDE.md) - Learn how to switch backends without code changes!

## Architecture

- **GraphQL Service** (Port 8080) - Entry point, receives GraphQL mutations
- **Order Service** (Port 8081) - REST service, processes orders and publishes to RabbitMQ
- **Inventory Service** (Port 8082) - RabbitMQ consumer, updates inventory
- **Notification Service** (Port 8083) - RabbitMQ consumer, sends notifications

## Infrastructure

- **RabbitMQ** - Message broker (Ports 5672, 15672)
- **OpenTelemetry Collector** - Trace collection & routing (Ports 4317, 4318, 8888)
- **Tempo** - Distributed tracing backend (Port 3200)
- **Loki** - Log aggregation (Port 3100)
- **Grafana** - Visualization (Port 3000)

### Why OpenTelemetry Collector?

This project uses an **OpenTelemetry Collector** as an intermediary between services and the tracing backend. This provides:

- ✅ **Vendor Independence**: Switch between Tempo, Jaeger, Zipkin, or any backend without changing service code
- ✅ **Centralized Configuration**: All export logic in one place
- ✅ **Multiple Backends**: Send traces to multiple destinations simultaneously
- ✅ **Processing Capabilities**: Sampling, filtering, enrichment, batching

See [`COLLECTOR_GUIDE.md`](COLLECTOR_GUIDE.md) for details on switching backends.

## Prerequisites

- Docker & Docker Compose
- **Java 25 LTS** (recommended) or Java 21+
- Maven 3.8.7+

### Installing Java 25 LTS

```bash
# Using SDKMAN (recommended)
sdk install java 25.0.1-open
sdk use java 25.0.1-open

# Verify installation
java -version
# Should show: openjdk version "25.0.1"
```

## Setup

### 1. Install Spring Boot 4.0.1 Parent POMs

Due to Maven's validation process, we need to install the parent POMs manually first:

```bash
bash setup.sh
```

This script downloads and installs the Spring Boot 4.0.1 parent POMs to your local Maven repository.

### 2. Start Infrastructure

```bash
docker compose up -d
```

This starts RabbitMQ, Tempo, Loki, and Grafana.

### 3. Start All Services

```bash
bash run_all.sh
```

This will:
- Start all 4 microservices in the background
- Show logs from all services
- Keep running until you press CTRL+C

### 4. Test the System

In another terminal:

```bash
bash test_system.sh
```

This sends a GraphQL mutation and saves the response to `response.json`.

### 5. Test the Collector (Optional)

Verify that the OpenTelemetry Collector is working correctly:

```bash
bash test_collector.sh
```

This will check that traces are flowing through the collector to Tempo.

## View Traces

1. Open Grafana: http://localhost:3000
2. Go to **Explore**
3. Select **Tempo** datasource
4. Click "Search" to see traces
5. You can also view logs in **Loki** datasource

## Manual Service Start (Alternative)

If you prefer to start services manually in separate terminals:

```bash
# Terminal 1
cd graphql-service && mvn spring-boot:run

# Terminal 2
cd order-service && mvn spring-boot:run

# Terminal 3
cd inventory-service && mvn spring-boot:run

# Terminal 4
cd notification-service && mvn spring-boot:run
```

## Troubleshooting

### Maven Dependency Resolution Issues

If you see errors about missing dependency versions:

1. Make sure you ran `bash setup.sh` first
2. Try: `mvn dependency:purge-local-repository` then `mvn dependency:resolve`
3. Check that Spring Boot 4.0.1 parent POMs are installed:
   ```bash
   ls ~/.m2/repository/org/springframework/boot/spring-boot-starter-parent/4.0.1/
   ```

### Services Not Starting

- Check logs in the `logs/` directory
- Verify Docker containers are running: `docker compose ps`
- Check if ports are already in use: `netstat -tuln | grep -E '8080|8081|8082|8083'`

## Project Structure

```
tracing-demo-v2/
├── config/              # Tempo, Loki, Grafana configurations
├── graphql-service/     # GraphQL entry point
├── order-service/       # Order processing service
├── inventory-service/   # Inventory management
├── notification-service/# Notification service
├── logs/               # Service logs
├── docker-compose.yml  # Infrastructure setup
├── run_all.sh         # Start all services
├── test_system.sh     # Test script
└── setup.sh           # Setup parent POMs
```

## Technologies

- **Spring Boot 4.0.1** - Application framework
- **Spring GraphQL** - GraphQL support
- **Spring AMQP** - RabbitMQ integration
- **Micrometer Tracing** - Distributed tracing abstraction
- **OpenTelemetry** - Tracing implementation
- **Loki** - Log aggregation
- **Tempo** - Trace storage
- **Grafana** - Visualization
