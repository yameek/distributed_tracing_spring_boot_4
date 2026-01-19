# CQRS Service with Command Bus, Event Bus, and Outbox Pattern

A comprehensive implementation of CQRS (Command Query Responsibility Segregation) pattern with:
- **Command Bus** - Centralized command dispatching with method-level tracing
- **Event Bus** - Pub/sub event handling with method-level tracing
- **Outbox Pattern** - Reliable event publishing with transactional guarantees
- **Full Observability** - Distributed tracing, metrics, and structured logging

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                         REST API Layer                               │
│                      (ProductController)                             │
└────────────────┬────────────────────────────────┬───────────────────┘
                 │                                 │
                 ▼                                 ▼
        ┌────────────────┐              ┌──────────────────┐
        │  Command Bus   │              │   Query Bus      │
        │  (Write Side)  │              │   (Read Side)    │
        └────────┬───────┘              └────────┬─────────┘
                 │                                │
                 ▼                                ▼
        ┌────────────────┐              ┌──────────────────┐
        │ Command Handler│              │  Query Handler   │
        │   + Tracing    │              │   + Tracing      │
        │   + Metrics    │              │                  │
        └────────┬───────┘              └────────┬─────────┘
                 │                                │
                 ▼                                ▼
        ┌────────────────┐              ┌──────────────────┐
        │   Domain       │              │   Repository     │
        │   Model        │◄─────────────┤   (Read Model)   │
        └────────┬───────┘              └──────────────────┘
                 │
                 ▼
        ┌────────────────┐
        │  Outbox        │
        │  Service       │
        │  (Same TX)     │
        └────────┬───────┘
                 │
                 ▼
        ┌────────────────┐
        │  Outbox        │
        │  Publisher     │
        │  (Polling)     │
        └────────┬───────┘
                 │
                 ▼
        ┌────────────────┐
        │   RabbitMQ     │
        │   Exchange     │
        └────────┬───────┘
                 │
                 ▼
        ┌────────────────┐
        │   Event Bus    │
        │   (Pub/Sub)    │
        └────────┬───────┘
                 │
                 ▼
        ┌────────────────┐
        │ Event Handlers │
        │   + Tracing    │
        │   + Metrics    │
        └────────────────┘
```

## Key Components

### 1. Command Bus (`CommandBus.java`)
- **Purpose**: Centralized command dispatching
- **Features**:
  - Single handler per command type
  - Method-level tracing with OpenTelemetry
  - Metrics: success/failure counters, execution time
  - Automatic error handling and logging
  
**Tracing Points**:
- `command.bus.dispatch` - Overall command dispatch
- Command type and ID captured as span attributes

### 2. Event Bus (`EventBus.java`)
- **Purpose**: Publish-subscribe event handling
- **Features**:
  - Multiple handlers per event type
  - Method-level tracing for each handler
  - Metrics: published/handled counters, execution time
  - Isolated error handling (one handler failure doesn't affect others)

**Tracing Points**:
- `event.bus.publish` - Event publication
- `event.handler.execute` - Individual handler execution
- Event type, ID, and aggregate ID captured

### 3. Outbox Pattern (`OutboxService.java`, `OutboxPublisher.java`)
- **Purpose**: Reliable event publishing with transactional guarantees
- **How it works**:
  1. Domain changes and events stored in same transaction
  2. Background poller reads pending events from outbox table
  3. Events published to RabbitMQ
  4. Status updated (PENDING → PROCESSING → PUBLISHED/FAILED)
  5. Automatic retry with exponential backoff

**Features**:
- Transactional consistency
- At-least-once delivery guarantee
- Automatic retry logic (max 3 retries)
- Pessimistic locking to prevent duplicate processing
- Method-level tracing for storage and publishing

**Tracing Points**:
- `outbox.store` - Event storage
- `outbox.poll` - Polling operation
- `outbox.publish` - Event publishing to RabbitMQ

### 4. Query Bus (`QueryBus.java`)
- **Purpose**: Centralized query dispatching (read side)
- **Features**:
  - Single handler per query type
  - Method-level tracing
  - Metrics: success/failure counters, execution time
  - Read-only operations

**Tracing Points**:
- `query.bus.dispatch` - Query dispatch
- Query type and ID captured

## Domain Model: Product Aggregate

The service implements a complete Product aggregate with:

### Commands (Write Operations)
1. **CreateProductCommand** - Create new product
2. **UpdateProductPriceCommand** - Update product price
3. **UpdateStockCommand** - Update product stock

### Events (Domain Events)
1. **ProductCreatedEvent** - Product was created
2. **ProductPriceUpdatedEvent** - Price was updated
3. **ProductStockUpdatedEvent** - Stock was updated

### Queries (Read Operations)
1. **GetProductByIdQuery** - Get single product
2. **GetAllProductsQuery** - Get all products

## Method-Level Tracing

Every operation is traced at the method level:

### Command Flow Trace
```
HTTP Request → api.create.product
  └─→ command.bus.dispatch
      └─→ command.handler.create.product
          ├─→ Database Save
          └─→ outbox.store
              └─→ outbox.poll (async)
                  └─→ outbox.publish
                      └─→ event.bus.publish
                          └─→ event.handler.execute (per handler)
```

### Trace Attributes
- **Command/Query/Event Type**: Identifies the operation
- **Command/Query/Event ID**: Unique identifier for correlation
- **Aggregate ID**: Domain entity identifier
- **Handler Name**: Which handler processed the operation

## Metrics

### Command Bus Metrics
- `command.bus.success` - Successful command executions
- `command.bus.failure` - Failed command executions
- `command.bus.execution.time` - Command execution duration

### Event Bus Metrics
- `event.bus.published` - Events published
- `event.bus.handled` - Events successfully handled
- `event.bus.handling.failure` - Event handling failures
- `event.bus.handling.time` - Event handling duration

### Query Bus Metrics
- `query.bus.success` - Successful query executions
- `query.bus.failure` - Failed query executions
- `query.bus.execution.time` - Query execution duration

### Outbox Metrics
- `outbox.event.stored` - Events stored in outbox
- `outbox.event.storage.failure` - Storage failures
- `outbox.events.published` - Events published from outbox
- `outbox.events.failed` - Publishing failures

### Domain Metrics
- `products.created` - Products created
- `products.price.updated` - Price updates
- `products.stock.updated` - Stock updates
- `alerts.low.stock` - Low stock alerts triggered
- `events.product.*.handled` - Event handling counters

## Logging

Structured JSON logging with:
- **Trace ID** - Correlates logs with traces
- **Span ID** - Identifies specific operation
- **Service Name** - `cqrs-service`
- **Log Level** - INFO, WARN, ERROR
- **Timestamp** - ISO8601 format
- **Context** - Command/Event/Query IDs, aggregate IDs

Logs are sent to:
- Console (JSON format)
- Loki (centralized log aggregation)

## Database Schema

### Products Table
```sql
CREATE TABLE products (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(19,2) NOT NULL,
    stock_quantity INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    version BIGINT
);
```

### Outbox Events Table
```sql
CREATE TABLE outbox_events (
    id VARCHAR(36) PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(36) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    version BIGINT
);

CREATE INDEX idx_outbox_status_created ON outbox_events(status, created_at);
CREATE INDEX idx_outbox_aggregate ON outbox_events(aggregate_id);
```

## API Endpoints

### Create Product
```bash
POST /api/products
Content-Type: application/json

{
  "name": "Laptop",
  "description": "High-performance laptop",
  "price": 999.99,
  "initialStock": 50
}

Response: 201 Created
{
  "productId": "uuid",
  "message": "Product created successfully"
}
```

### Update Price
```bash
PUT /api/products/{productId}/price
Content-Type: application/json

{
  "newPrice": 899.99
}

Response: 200 OK
{
  "message": "Price updated successfully"
}
```

### Update Stock
```bash
PUT /api/products/{productId}/stock
Content-Type: application/json

{
  "quantity": 45
}

Response: 200 OK
{
  "message": "Stock updated successfully"
}
```

### Get Product
```bash
GET /api/products/{productId}

Response: 200 OK
{
  "id": "uuid",
  "name": "Laptop",
  "description": "High-performance laptop",
  "price": 899.99,
  "stockQuantity": 45,
  "status": "ACTIVE",
  "createdAt": "2026-01-18T10:00:00Z",
  "updatedAt": "2026-01-18T11:00:00Z",
  "version": 2
}
```

### Get All Products
```bash
GET /api/products

Response: 200 OK
[
  {
    "id": "uuid",
    "name": "Laptop",
    ...
  }
]
```

## Running the Service

### Prerequisites
1. PostgreSQL running on `localhost:5432`
2. RabbitMQ running on `localhost:5672`
3. OpenTelemetry Collector on `localhost:4317`

### Start Infrastructure
```bash
cd tracing-demo-v2
docker-compose up -d
```

This starts:
- RabbitMQ (ports 5672, 15672)
- PostgreSQL (port 5432)
- OpenTelemetry Collector (port 4317)
- Tempo (traces)
- Loki (logs)
- Grafana (visualization on port 3000)

### Build and Run
```bash
# Build all services
./gradlew build

# Run CQRS service
./gradlew :cqrs-service:bootRun
```

Service runs on `http://localhost:8084`

### Test the Service
```bash
# Create a product
curl -X POST http://localhost:8084/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop",
    "description": "High-performance laptop",
    "price": 999.99,
    "initialStock": 50
  }'

# Get product ID from response, then:

# Update price
curl -X PUT http://localhost:8084/api/products/{productId}/price \
  -H "Content-Type: application/json" \
  -d '{"newPrice": 899.99}'

# Update stock
curl -X PUT http://localhost:8084/api/products/{productId}/stock \
  -H "Content-Type: application/json" \
  -d '{"quantity": 45}'

# Get product
curl http://localhost:8084/api/products/{productId}

# Get all products
curl http://localhost:8084/api/products
```

## Observability

### View Traces in Grafana
1. Open Grafana: `http://localhost:3000`
2. Go to Explore → Select Tempo
3. Search for traces with service name: `cqrs-service`
4. View complete trace spanning:
   - API request
   - Command/Query dispatch
   - Handler execution
   - Database operations
   - Outbox storage
   - Event publishing
   - Event handling

### View Metrics
1. Open Grafana: `http://localhost:3000`
2. Go to Explore → Select Prometheus
3. Query metrics:
   - `command_bus_success_total`
   - `event_bus_published_total`
   - `outbox_events_published_total`
   - `products_created_total`

### View Logs
1. Open Grafana: `http://localhost:3000`
2. Go to Explore → Select Loki
3. Query: `{service="cqrs-service"}`
4. Filter by trace ID to see all logs for a specific request

### RabbitMQ Management
- URL: `http://localhost:15672`
- Username: `guest`
- Password: `guest`
- View exchanges, queues, and message flow

## Benefits of This Architecture

### 1. Separation of Concerns
- Commands (write) and Queries (read) are clearly separated
- Each handler has a single responsibility
- Easy to understand and maintain

### 2. Scalability
- Read and write sides can scale independently
- Event handlers can be distributed across multiple instances
- Outbox pattern ensures reliable event delivery

### 3. Observability
- Every operation is traced end-to-end
- Metrics provide insights into system behavior
- Structured logs enable easy debugging

### 4. Reliability
- Outbox pattern ensures transactional consistency
- Automatic retry mechanism for failed events
- Pessimistic locking prevents duplicate processing

### 5. Extensibility
- Easy to add new commands, events, or queries
- New event handlers can be added without modifying existing code
- Clear extension points for new features

## Advanced Features

### Outbox Polling Configuration
The outbox publisher runs on a scheduled basis:
- **Pending Events**: Polled every 5 seconds
- **Failed Events**: Retried every 60 seconds
- **Max Retries**: 3 attempts
- **Retry Cutoff**: 24 hours

### Event Handler Isolation
- Each event handler executes in its own traced span
- Handler failures don't affect other handlers
- Failed handlers are logged but don't block event processing

### Optimistic Locking
- Product entity uses `@Version` for optimistic locking
- Prevents concurrent modification issues
- Automatic retry on version conflicts

### Low Stock Alerts
- Automatic alerts when stock falls below 10 items
- Tracked via `alerts.low.stock` metric
- Can trigger notifications or reorder processes

## Troubleshooting

### Events Not Being Published
1. Check outbox table: `SELECT * FROM outbox_events WHERE status = 'PENDING'`
2. Check RabbitMQ connection in logs
3. Verify RabbitMQ is running: `docker ps | grep rabbitmq`

### Traces Not Appearing
1. Verify OpenTelemetry Collector is running
2. Check service logs for OTLP export errors
3. Verify Tempo is receiving traces in Grafana

### Database Connection Issues
1. Verify PostgreSQL is running: `docker ps | grep postgres`
2. Check connection string in `application.yml`
3. Verify database exists: `psql -U postgres -l`

## Future Enhancements

1. **Event Sourcing**: Store all events as the source of truth
2. **CQRS Read Models**: Separate optimized read models
3. **Saga Pattern**: Coordinate distributed transactions
4. **Event Replay**: Rebuild state from events
5. **Snapshots**: Optimize aggregate loading
6. **Dead Letter Queue**: Handle permanently failed events
7. **Event Versioning**: Support event schema evolution

## References

- [CQRS Pattern](https://martinfowler.com/bliki/CQRS.html)
- [Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html)
- [OpenTelemetry](https://opentelemetry.io/)
- [Spring Boot Observability](https://spring.io/blog/2022/10/12/observability-with-spring-boot-3)
