# Distributed Tracing Demo

A complete, runnable demonstration of distributed tracing in a Spring Boot microservices architecture. This project shows how to implement end-to-end trace propagation across REST, GraphQL, and RabbitMQ using OpenTelemetry and Grafana.

## What This Project Does

- **Demonstrates distributed tracing** across 4 microservices
- **Shows trace propagation** through HTTP and message queues
- **Correlates logs with traces** for debugging
- **Visualizes request flow** in Grafana
- **Provides working code examples** you can learn from

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 21+ (Java 25 recommended)
- Maven 3.8.7+

### Running the Demo

```bash
# 1. Navigate to the demo directory
cd tracing-demo-v2

# 2. Start infrastructure (Tempo, Loki, Grafana, RabbitMQ)
docker compose up -d

# 3. Start all services
./run_all.sh

# 4. Send a test request
./test_system.sh

# 5. View traces in Grafana
open http://localhost:3000
```

That's it! You now have a fully functional distributed tracing system.

## Project Structure

```
tracing-demo-v2/          # Main demo application
├── graphql-service/      # GraphQL entry point (port 8080)
├── order-service/        # Order processing (port 8081)
├── inventory-service/    # Inventory management (port 8082)
├── notification-service/ # Notifications (port 8083)
├── config/               # Grafana/Tempo/Loki configuration
└── docker-compose.yml    # Infrastructure setup

docs/                     # Documentation
├── IMPLEMENTATION_GUIDE.md   # Complete implementation guide
├── GRAFANA_GUIDE.md          # How to use Grafana
├── ARCHITECTURE.md           # System architecture
├── GOAL.md                   # Project objectives
├── STACK.md                  # Technology stack
└── FIX_HISTORY.md            # Troubleshooting history
```

## Documentation

### Getting Started
- **[PROJECT_RECREATION_GUIDE.md](docs/PROJECT_RECREATION_GUIDE.md)** - ⭐ Recreate this entire project from scratch
- **[IMPLEMENTATION_GUIDE.md](docs/IMPLEMENTATION_GUIDE.md)** - Complete implementation concepts and patterns
- **[Quick Start](tracing-demo-v2/README.md)** - How to run the existing demo
- **[Architecture Overview](docs/ARCHITECTURE.md)** - System design and data flow

### Using the System
- **[Grafana Guide](docs/GRAFANA_GUIDE.md)** - How to view traces and logs
- **[Access Instructions](docs/tracing-demo/tracing_access_instructions.md)** - URLs and credentials
- **[Visualization Guide](docs/tracing-demo/tracing_visualization_guide.md)** - Understanding trace visualizations

### Reference
- **[Branch Guide](docs/BRANCH_GUIDE.md)** - 🌿 Understand different branches and their implementations
- **[Technology Stack](docs/STACK.md)** - All technologies used
- **[Fix History](docs/FIX_HISTORY.md)** - Common issues and solutions
- **[Goal](docs/GOAL.md)** - What this project achieves

## Key Features

✅ **End-to-end tracing** - Single trace ID flows through all services  
✅ **Multiple protocols** - REST, GraphQL, and RabbitMQ  
✅ **Log correlation** - Logs include trace IDs for easy debugging  
✅ **Production-ready** - Uses Spring Boot 4.0.1 OpenTelemetry starter  
✅ **Observable** - Grafana dashboards for traces and logs  

## Technology Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Framework** | Spring Boot 4.0.1 | Application framework |
| **Language** | Java 21/25 | Programming language |
| **Tracing** | OpenTelemetry | Distributed tracing |
| **API** | GraphQL, REST | Service communication |
| **Messaging** | RabbitMQ | Async communication |
| **Trace Storage** | Grafana Tempo | Trace backend |
| **Log Storage** | Grafana Loki | Log aggregation |
| **Visualization** | Grafana | Observability UI |

## How It Works

1. **GraphQL Service** receives a mutation to create an order
2. **HTTP call** to Order Service (trace context propagated in headers)
3. **Order Service** saves to database and publishes event to RabbitMQ
4. **RabbitMQ** delivers event to two consumers in parallel (trace context in message headers)
   - **Inventory Service** updates inventory
   - **Notification Service** sends notification
5. All services export spans to **Tempo** via OTLP
6. All logs include **trace ID** for correlation
7. **Grafana** visualizes the complete request flow

## What You'll Learn

- How to add OpenTelemetry to Spring Boot applications
- How to propagate trace context across HTTP calls
- How to propagate trace context through message queues
- How to correlate logs with traces
- How to visualize distributed traces in Grafana
- Best practices for production observability

## Use Cases

This demo is useful for:

- **Learning** distributed tracing concepts
- **Reference** when implementing tracing in your services
- **Troubleshooting** tracing issues
- **Demonstrating** observability to stakeholders
- **Baseline** for building production observability

## Quick Access

| Service | URL | Purpose |
|---------|-----|---------|
| Grafana | http://localhost:3000 | View traces and logs |
| GraphQL | http://localhost:8080/graphiql | Test API |
| Order Service | http://localhost:8081 | REST endpoint |
| RabbitMQ | http://localhost:15672 | Message broker UI (guest/guest) |

## Next Steps

1. **Read the [Implementation Guide](docs/IMPLEMENTATION_GUIDE.md)** to understand how it works
2. **Run the demo** and explore traces in Grafana
3. **Review the code** in each service to see implementation patterns
4. **Apply these patterns** to your own microservices

## License

MIT
