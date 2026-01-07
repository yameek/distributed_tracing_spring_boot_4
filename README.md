# Tracing Basics

This repository contains a comprehensive distributed tracing demonstration and SDK integration documentation.

## Project Structure

```
.
├── tracing-demo-v2/          # Main microservices demo application
│   ├── order-service/        # Order management service
│   ├── inventory-service/    # Inventory checking service
│   ├── notification-service/ # Notification service
│   ├── graphql-service/      # GraphQL gateway
│   ├── config/               # Observability stack configuration
│   └── logs/                 # Service logs
│
├── docs/                     # All project documentation
│   ├── sdk-integration/      # SDK integration guides
│   ├── migration/            # Migration documentation
│   ├── planning/             # Planning and design docs
│   └── planned_sdk_doc/      # Planned SDK documentation
│
└── todo.txt                  # Project tasks
```

## Quick Start

### Running the Demo

1. Navigate to the demo directory:
   ```bash
   cd tracing-demo-v2
   ```

2. Start all services:
   ```bash
   ./setup.sh
   ./run_all.sh
   ```

3. Access the services:
   - **Grafana**: http://localhost:3000 (admin/admin)
   - **Order Service**: http://localhost:8081
   - **GraphQL Service**: http://localhost:8084

For detailed instructions, see [`tracing-demo-v2/README.md`](./tracing-demo-v2/README.md)

## Documentation

All documentation has been organized in the [`docs/`](./docs/) directory:

- **SDK Integration**: Guides for integrating the tracing SDK
- **Migration**: Migration strategies and comparisons
- **Planning**: Architecture and planning documents
- **Planned SDK**: Future SDK documentation structure

See [`docs/README.md`](./docs/README.md) for the complete documentation map.

## Key Features

- **Distributed Tracing**: Full trace propagation across microservices
- **Multiple Protocols**: REST, GraphQL, and RabbitMQ messaging
- **Observability Stack**: Tempo, Loki, and Grafana integration
- **Structured Logging**: JSON logging with trace correlation
- **Spring Boot 3.4.1**: Latest Spring Boot with Micrometer Tracing

## Technologies

- **Java 25**
- **Spring Boot 3.4.1**
- **Micrometer Tracing**
- **Grafana Tempo** (distributed tracing)
- **Grafana Loki** (log aggregation)
- **RabbitMQ** (message broker)
- **PostgreSQL** (database)

## License

MIT
