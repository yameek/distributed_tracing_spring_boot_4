# Example Trace Output

This document shows what you'll actually see when running the distributed tracing demo.

## Test Execution

```bash
$ ./test_distributed_tracing.sh
```

## Console Output

```
==================================================
Distributed Tracing Test: HTTP + RabbitMQ
==================================================

Checking if orchestrator service is running...
✓ Orchestrator service is running

Checking if CQRS service is running...
✓ CQRS service is running

Executing product workflow...
This workflow will:
  1. Create a product via HTTP REST API
  2. Update price via RabbitMQ message
  3. Update stock via RabbitMQ message
  4. Query product via HTTP REST API

Response:
{
  "productId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Workflow completed successfully",
  "traceId": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "steps": {
    "step1CreateViaHttp": "Created product via HTTP REST API",
    "step2UpdatePriceViaRabbitMQ": "Updated price via RabbitMQ message",
    "step3UpdateStockViaRabbitMQ": "Updated stock via RabbitMQ message",
    "step4QueryViaHttp": "Queried product via HTTP REST API"
  }
}

==================================================
Workflow Completed Successfully!
==================================================

Trace ID: a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6
Product ID: 550e8400-e29b-41d4-a716-446655440000

How to observe the trace:

1. Open Grafana: http://localhost:3000
2. Go to 'Explore' (compass icon on the left)
3. Select 'Tempo' as the data source
4. Search for trace ID: a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6

What you'll see in the trace:
  • orchestrator-service: Initial HTTP request
  • orchestrator-service: Workflow orchestration span
  • cqrs-service: HTTP product creation (via REST API)
  • cqrs-service: Command bus processing
  • RabbitMQ: Message publishing to outbox
  • orchestrator-service: RabbitMQ price update message
  • cqrs-service: RabbitMQ message consumption (price update)
  • orchestrator-service: RabbitMQ stock update message
  • cqrs-service: RabbitMQ message consumption (stock update)
  • cqrs-service: HTTP product query (via REST API)

The trace shows how a single request lifecycle spans:
  ✓ Multiple services (orchestrator-service, cqrs-service)
  ✓ Multiple protocols (HTTP REST, RabbitMQ messaging)
  ✓ Multiple operations (create, update, query)
  ✓ All connected by a single trace ID!

Verify the product was updated:
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Gaming Laptop",
  "description": "High-performance gaming laptop with RTX 4090",
  "price": 2299.99,
  "stock": 15,
  "createdAt": "2026-01-19T10:30:45.123Z",
  "updatedAt": "2026-01-19T10:30:47.456Z"
}

Test completed!
```

## Service Logs

### Orchestrator Service Logs

```json
{
  "@timestamp": "2026-01-19T10:30:45.123Z",
  "level": "INFO",
  "logger": "com.example.tracing.orchestrator.controller.WorkflowController",
  "message": "Received product workflow request: name=Gaming Laptop",
  "traceId": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "spanId": "1234567890abcdef",
  "service": "orchestrator-service"
}

{
  "@timestamp": "2026-01-19T10:30:45.125Z",
  "level": "INFO",
  "logger": "com.example.tracing.orchestrator.service.ProductWorkflowService",
  "message": "Starting product workflow with traceId: a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "traceId": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "spanId": "2345678901bcdefg",
  "service": "orchestrator-service"
}

{
  "@timestamp": "2026-01-19T10:30:45.130Z",
  "level": "INFO",
  "logger": "com.example.tracing.orchestrator.service.ProductWorkflowService",
  "message": "Creating product via HTTP: name=Gaming Laptop",
  "traceId": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "spanId": "3456789012cdefgh",
  "service": "orchestrator-service"
}

{
  "@timestamp": "2026-01-19T10:30:45.345Z",
  "level": "INFO",
  "logger": "com.example.tracing.orchestrator.service.ProductWorkflowService",
  "message": "Step 1 completed: Product created via HTTP, productId=550e8400-e29b-41d4-a716-446655440000, traceId=a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "traceId": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "spanId": "3456789012cdefgh",
  "service": "orchestrator-service"
}

{
  "@timestamp": "2026-01-19T10:30:45.850Z",
  "level": "INFO",
  "logger": "com.example.tracing.orchestrator.service.ProductWorkflowService",
  "message": "Sending price update via RabbitMQ: productId=550e8400-e29b-41d4-a716-446655440000, newPrice=2299.99",
  "traceId": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "spanId": "4567890123defghi",
  "service": "orchestrator-service"
}

{
  "@timestamp": "2026-01-19T10:30:45.855Z",
  "level": "INFO",
  "logger": "com.example.tracing.orchestrator.service.ProductWorkflowService",
  "message": "Price update message sent to RabbitMQ",
  "traceId": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "spanId": "4567890123defghi",
  "service": "orchestrator-service"
}
```

### CQRS Service Logs

```json
{
  "@timestamp": "2026-01-19T10:30:45.135Z",
  "level": "INFO",
  "logger": "com.example.tracing.cqrs.api.ProductController",
  "message": "Received request to create product: Gaming Laptop",
  "traceId": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "spanId": "5678901234efghij",
  "parentSpanId": "3456789012cdefgh",
  "service": "cqrs-service"
}

{
  "@timestamp": "2026-01-19T10:30:45.140Z",
  "level": "INFO",
  "logger": "com.example.tracing.cqrs.infrastructure.command.CommandBus",
  "message": "Dispatching command: CreateProductCommand",
  "traceId": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "spanId": "6789012345fghijk",
  "parentSpanId": "5678901234efghij",
  "service": "cqrs-service"
}

{
  "@timestamp": "2026-01-19T10:30:45.340Z",
  "level": "INFO",
  "logger": "com.example.tracing.cqrs.api.ProductController",
  "message": "Product created successfully with ID: 550e8400-e29b-41d4-a716-446655440000",
  "traceId": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "spanId": "5678901234efghij",
  "service": "cqrs-service"
}

{
  "@timestamp": "2026-01-19T10:30:45.860Z",
  "level": "INFO",
  "logger": "com.example.tracing.cqrs.messaging.CommandMessageListener",
  "message": "Received command via RabbitMQ: type=UpdatePrice",
  "traceId": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "spanId": "7890123456ghijkl",
  "parentSpanId": "4567890123defghi",
  "service": "cqrs-service"
}

{
  "@timestamp": "2026-01-19T10:30:45.865Z",
  "level": "INFO",
  "logger": "com.example.tracing.cqrs.messaging.CommandMessageListener",
  "message": "Processing UpdatePrice command: productId=550e8400-e29b-41d4-a716-446655440000",
  "traceId": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "spanId": "8901234567hijklm",
  "parentSpanId": "7890123456ghijkl",
  "service": "cqrs-service"
}
```

## Grafana Trace View

When you search for trace ID `a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6` in Grafana Tempo:

### Trace Summary
```
Trace ID: a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6
Duration: 2.5s
Spans: 15
Services: 2
  - orchestrator-service
  - cqrs-service
```

### Span Tree View
```
▼ orchestrator-service: api-workflow-product (2.5s)
  ▼ orchestrator-service: workflow-product-complete (2.4s)
    ▼ orchestrator-service: workflow-step-create-http (215ms)
      ▼ cqrs-service: api-create-product (210ms) [HTTP]
        ▼ cqrs-service: command.bus.dispatch (205ms)
          ▶ cqrs-service: command.handler.execute (200ms)
    
    ▼ orchestrator-service: workflow-step-update-price-rabbitmq (5ms)
      ▼ rabbitmq: publish (3ms)
        ▼ cqrs-service: rabbitmq-command-received (320ms) [RabbitMQ]
          ▼ cqrs-service: rabbitmq-update-price (315ms)
            ▼ cqrs-service: command.bus.dispatch (310ms)
              ▶ cqrs-service: command.handler.execute (305ms)
    
    ▼ orchestrator-service: workflow-step-update-stock-rabbitmq (5ms)
      ▼ rabbitmq: publish (3ms)
        ▼ cqrs-service: rabbitmq-command-received (310ms) [RabbitMQ]
          ▼ cqrs-service: rabbitmq-update-stock (305ms)
            ▼ cqrs-service: command.bus.dispatch (300ms)
              ▶ cqrs-service: command.handler.execute (295ms)
    
    ▼ orchestrator-service: workflow-step-query-http (105ms)
      ▼ cqrs-service: api-get-product (100ms) [HTTP]
        ▼ cqrs-service: query.bus.dispatch (95ms)
          ▶ cqrs-service: query.handler.execute (90ms)
```

### Timeline View
```
Time (ms)    0    500   1000  1500  2000  2500
             |     |     |     |     |     |
orchestrator ████████████████████████████████
  workflow   ███████████████████████████████
    create   ███
      cqrs   ███
    update-p     █
      rabbitmq   █
      cqrs           ███
    update-s               █
      rabbitmq             █
      cqrs                     ███
    query                            ██
      cqrs                            ██
```

### Span Details (Example: HTTP Create)

**Span:** `cqrs-service: api-create-product`
- **Trace ID:** a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6
- **Span ID:** 5678901234efghij
- **Parent Span ID:** 3456789012cdefgh
- **Service:** cqrs-service
- **Operation:** api-create-product
- **Duration:** 210ms
- **Start Time:** 2026-01-19T10:30:45.135Z
- **End Time:** 2026-01-19T10:30:45.345Z

**Tags:**
- `http.method`: POST
- `http.url`: /api/products
- `http.status_code`: 201
- `span.kind`: server

**Logs:**
- 10:30:45.135 - Received request to create product
- 10:30:45.140 - Dispatching command
- 10:30:45.340 - Product created successfully

### Span Details (Example: RabbitMQ Update)

**Span:** `cqrs-service: rabbitmq-command-received`
- **Trace ID:** a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6
- **Span ID:** 7890123456ghijkl
- **Parent Span ID:** 4567890123defghi
- **Service:** cqrs-service
- **Operation:** rabbitmq-command-received
- **Duration:** 320ms
- **Start Time:** 2026-01-19T10:30:45.860Z
- **End Time:** 2026-01-19T10:30:46.180Z

**Tags:**
- `messaging.system`: rabbitmq
- `messaging.destination`: cqrs.commands.queue
- `messaging.operation`: receive
- `span.kind`: consumer

**Logs:**
- 10:30:45.860 - Received command via RabbitMQ
- 10:30:45.865 - Processing UpdatePrice command
- 10:30:46.180 - Command processed successfully

## Log Correlation in Grafana Loki

Query:
```
{service=~".*"} |= "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6"
```

Results (chronological):
```
10:30:45.123 orchestrator-service Received product workflow request
10:30:45.125 orchestrator-service Starting product workflow
10:30:45.130 orchestrator-service Creating product via HTTP
10:30:45.135 cqrs-service        Received request to create product
10:30:45.140 cqrs-service        Dispatching command
10:30:45.340 cqrs-service        Product created successfully
10:30:45.345 orchestrator-service Step 1 completed
10:30:45.850 orchestrator-service Sending price update via RabbitMQ
10:30:45.855 orchestrator-service Price update message sent
10:30:45.860 cqrs-service        Received command via RabbitMQ
10:30:45.865 cqrs-service        Processing UpdatePrice command
10:30:46.180 cqrs-service        Command processed successfully
10:30:46.185 orchestrator-service Step 2 completed
... and so on
```

All logs from both services, correlated by trace ID!

## Key Observations

### 1. Single Trace ID Throughout
✅ Same trace ID in all logs and spans
✅ Easy to find all related operations

### 2. Parent-Child Relationships
✅ Each span has a parent span ID
✅ Clear hierarchy of operations

### 3. Cross-Service Visibility
✅ See operations in both services
✅ Understand service boundaries

### 4. Cross-Protocol Visibility
✅ HTTP calls clearly marked
✅ RabbitMQ operations clearly marked

### 5. Timing Information
✅ Duration of each operation
✅ Identify bottlenecks
✅ Understand async delays

### 6. Complete Context
✅ All logs include trace context
✅ Easy to correlate logs with traces
✅ Full visibility into request lifecycle

## Summary

This example shows the **actual output** you'll see when running the distributed tracing demo. The trace ID `a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6` connects:

- ✅ 2 services
- ✅ 2 protocols (HTTP, RabbitMQ)
- ✅ 4 operations (create, update price, update stock, query)
- ✅ 15 spans
- ✅ All logs from both services

Giving you **complete visibility** into the request lifecycle!
