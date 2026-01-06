# Distributed Tracing Demo v2 - Spring Boot 4.0.1

A fresh implementation of the distributed tracing demo using Spring Boot 4.0.1, Micrometer Tracing, OpenTelemetry, Tempo, and Loki.

## Architecture

- **GraphQL Service** (Port 8080) - Entry point, receives GraphQL mutations
- **Order Service** (Port 8081) - REST service, processes orders and publishes to RabbitMQ
- **Inventory Service** (Port 8082) - RabbitMQ consumer, updates inventory
- **Notification Service** (Port 8083) - RabbitMQ consumer, sends notifications

## Infrastructure

- **RabbitMQ** - Message broker (Ports 5672, 15672)
- **Tempo** - Distributed tracing backend (Ports 3200, 4317, 4318, 9411)
- **Loki** - Log aggregation (Port 3100)
- **Grafana** - Visualization (Port 3000)

## Prerequisites

- Docker & Docker Compose
- Java 21+
- Maven 3.9+

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
