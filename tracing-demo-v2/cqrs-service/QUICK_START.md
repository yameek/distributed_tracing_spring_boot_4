# CQRS Service - Quick Start Guide

## 1. Start Infrastructure (5 minutes)

```bash
cd tracing-demo-v2
docker-compose up -d
```

Wait for all services to be healthy:
```bash
docker-compose ps
```

You should see:
- ✅ postgres (port 5432)
- ✅ rabbitmq (ports 5672, 15672)
- ✅ otel-collector (port 4317)
- ✅ tempo (port 3200)
- ✅ loki (port 3100)
- ✅ grafana (port 3000)

## 2. Build the Service (2 minutes)

```bash
./gradlew :cqrs-service:build
```

## 3. Run the Service (1 minute)

```bash
./gradlew :cqrs-service:bootRun
```

Service starts on `http://localhost:8084`

Look for these log messages:
```
Registering command handlers...
Command handlers registered successfully
Registering event handlers...
Event handlers registered successfully
Registering query handlers...
Query handlers registered successfully
```

## 4. Test the Service (2 minutes)

### Create a Product
```bash
curl -X POST http://localhost:8084/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Gaming Laptop",
    "description": "High-performance gaming laptop with RTX 4090",
    "price": 2499.99,
    "initialStock": 25
  }'
```

**Expected Response:**
```json
{
  "productId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Product created successfully"
}
```

**What Happens:**
1. ✅ Command dispatched through CommandBus
2. ✅ Product saved to database
3. ✅ ProductCreatedEvent stored in outbox table
4. ✅ Event published to RabbitMQ (within 5 seconds)
5. ✅ Event handlers process the event
6. ✅ Full trace created in Tempo

### Get All Products
```bash
curl http://localhost:8084/api/products
```

### Get Single Product
```bash
# Replace {productId} with the ID from create response
curl http://localhost:8084/api/products/{productId}
```

### Update Price
```bash
curl -X PUT http://localhost:8084/api/products/{productId}/price \
  -H "Content-Type: application/json" \
  -d '{"newPrice": 2299.99}'
```

### Update Stock
```bash
curl -X PUT http://localhost:8084/api/products/{productId}/stock \
  -H "Content-Type: application/json" \
  -d '{"quantity": 20}'
```

## 5. View Observability Data (3 minutes)

### Traces in Grafana
1. Open: `http://localhost:3000`
2. Go to: **Explore** → **Tempo**
3. Search: `service.name="cqrs-service"`
4. Click on any trace to see:
   - API request span
   - Command/Query bus dispatch
   - Handler execution
   - Database operations
   - Outbox storage
   - Event publishing
   - Event handler execution

**Example Trace Structure:**
```
api.create.product (200ms)
└─ command.bus.dispatch (180ms)
   └─ command.handler.create.product (170ms)
      ├─ Database Save (50ms)
      └─ outbox.store (30ms)
         └─ outbox.publish (async, 40ms)
            └─ event.bus.publish (35ms)
               └─ event.handler.product.created (25ms)
```

### Metrics in Grafana
1. Go to: **Explore** → **Prometheus**
2. Try these queries:
   ```promql
   # Command success rate
   rate(command_bus_success_total[5m])
   
   # Events published from outbox
   rate(outbox_events_published_total[5m])
   
   # Products created
   products_created_total
   
   # Command execution time (95th percentile)
   histogram_quantile(0.95, rate(command_bus_execution_time_bucket[5m]))
   ```

### Logs in Grafana
1. Go to: **Explore** → **Loki**
2. Query: `{service="cqrs-service"}`
3. Filter by trace ID to see all logs for a specific request
4. Example queries:
   ```logql
   # All logs
   {service="cqrs-service"}
   
   # Only errors
   {service="cqrs-service"} |= "ERROR"
   
   # Command handling
   {service="cqrs-service"} |= "command"
   
   # Event handling
   {service="cqrs-service"} |= "event"
   ```

### RabbitMQ Management
1. Open: `http://localhost:15672`
2. Login: `guest` / `guest`
3. View:
   - **Exchanges** → `cqrs.events.exchange`
   - **Queues** → `cqrs.events.queue`
   - **Messages** → See published events

### PostgreSQL Database
```bash
# Connect to database
docker exec -it tracing-demo-v2-postgres-1 psql -U postgres -d cqrs_db

# View products
SELECT * FROM products;

# View outbox events
SELECT id, event_type, aggregate_id, status, created_at 
FROM outbox_events 
ORDER BY created_at DESC;

# Count events by status
SELECT status, COUNT(*) 
FROM outbox_events 
GROUP BY status;
```

## 6. Understanding the Flow

### Command Flow (Write Operation)
```
1. HTTP POST /api/products
   ↓
2. ProductController.createProduct()
   ↓ (traced as "api.create.product")
3. CommandBus.dispatch(CreateProductCommand)
   ↓ (traced as "command.bus.dispatch")
4. CreateProductCommandHandler.handle()
   ↓ (traced as "command.handler.create.product")
5. Product saved to database
   ↓
6. ProductCreatedEvent stored in outbox (SAME TRANSACTION)
   ↓ (traced as "outbox.store")
7. HTTP 201 Created response
   ↓
8. [ASYNC] OutboxPublisher polls outbox table (every 5s)
   ↓ (traced as "outbox.poll")
9. Event published to RabbitMQ
   ↓ (traced as "outbox.publish")
10. EventBus receives event
    ↓ (traced as "event.bus.publish")
11. ProductCreatedEventHandler.handle()
    ↓ (traced as "event.handler.product.created")
12. Business logic executed (notifications, analytics, etc.)
```

### Query Flow (Read Operation)
```
1. HTTP GET /api/products/{id}
   ↓
2. ProductController.getProduct()
   ↓ (traced as "api.get.product")
3. QueryBus.dispatch(GetProductByIdQuery)
   ↓ (traced as "query.bus.dispatch")
4. GetProductByIdQueryHandler.handle()
   ↓ (traced as "query.handler.get.product.by.id")
5. Product loaded from database
   ↓
6. HTTP 200 OK response with product data
```

## 7. Key Observations

### Transactional Consistency
- Product save and event storage happen in **same transaction**
- If product save fails, event is not stored
- If event storage fails, product save is rolled back
- **Guaranteed consistency** between domain state and events

### Reliable Event Delivery
- Events stored in outbox table first
- Background poller publishes to RabbitMQ
- Automatic retry on failure (max 3 times)
- **At-least-once delivery** guarantee

### Complete Observability
- Every operation creates a trace span
- Trace ID propagates through entire flow
- Logs include trace ID for correlation
- Metrics track success/failure rates

### Performance
- Commands execute synchronously (fast response)
- Event publishing happens asynchronously (doesn't block)
- Outbox polling optimized with indexes
- Pessimistic locking prevents duplicate processing

## 8. Common Scenarios

### Scenario 1: Create Product and Watch Event Flow
1. Create a product
2. Check logs: See command handling
3. Wait 5 seconds
4. Check logs: See outbox polling and event publishing
5. Check logs: See event handler execution
6. View trace in Grafana: See complete flow

### Scenario 2: Update Price and Trigger Alert
1. Create a product
2. Update price (lower it)
3. Check event handler logs
4. View `products_price_updated_total` metric
5. See trace showing price update flow

### Scenario 3: Low Stock Alert
1. Create product with stock = 8
2. Check logs for "Low stock alert"
3. View `alerts_low_stock_total` metric
4. See event handler processing

### Scenario 4: Outbox Retry
1. Stop RabbitMQ: `docker-compose stop rabbitmq`
2. Create a product
3. Check outbox table: Status = FAILED
4. Start RabbitMQ: `docker-compose start rabbitmq`
5. Wait 60 seconds (retry interval)
6. Check outbox table: Status = PUBLISHED
7. View retry in logs and traces

## 9. Troubleshooting

### Service Won't Start
```bash
# Check if port 8084 is in use
lsof -i :8084

# Check PostgreSQL connection
docker exec -it tracing-demo-v2-postgres-1 pg_isready

# Check logs
./gradlew :cqrs-service:bootRun --info
```

### Events Not Publishing
```bash
# Check outbox table
docker exec -it tracing-demo-v2-postgres-1 psql -U postgres -d cqrs_db \
  -c "SELECT * FROM outbox_events WHERE status = 'PENDING';"

# Check RabbitMQ
docker logs tracing-demo-v2-rabbitmq-1

# Check service logs for "Polling outbox"
```

### No Traces in Grafana
```bash
# Check OpenTelemetry Collector
docker logs tracing-demo-v2-otel-collector-1

# Check Tempo
docker logs tracing-demo-v2-tempo-1

# Verify OTLP endpoint in application.yml
grep -A 5 "opentelemetry" cqrs-service/src/main/resources/application.yml
```

## 10. Next Steps

### Extend the Service
1. Add new commands (e.g., `DeleteProductCommand`)
2. Add new events (e.g., `ProductDeletedEvent`)
3. Add new queries (e.g., `SearchProductsQuery`)
4. Add new event handlers for different business logic

### Experiment with Patterns
1. Try concurrent updates (optimistic locking)
2. Simulate failures (stop RabbitMQ)
3. Load test with multiple products
4. Analyze traces for performance bottlenecks

### Integrate with Other Services
1. Call from `order-service` when order is placed
2. Publish events to other services
3. Subscribe to events from other services
4. Build distributed saga patterns

## Summary

You now have a fully functional CQRS service with:
- ✅ Command Bus with tracing and metrics
- ✅ Event Bus with pub/sub pattern
- ✅ Outbox pattern for reliable events
- ✅ Complete observability (traces, metrics, logs)
- ✅ Transactional consistency
- ✅ Automatic retry mechanism

**Total Setup Time: ~15 minutes**

Explore the code, experiment with the APIs, and view the observability data in Grafana!
