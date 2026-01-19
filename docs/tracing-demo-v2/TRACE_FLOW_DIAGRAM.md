# Trace Flow Diagram: HTTP + RabbitMQ

## Complete Request Lifecycle with Single Trace ID

```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃  Trace ID: a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6                      ┃
┃  This single ID connects ALL operations below                    ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛

┌─────────────────────────────────────────────────────────────────┐
│ 1. Client Request                                               │
│    POST /api/workflows/product                                  │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ Creates trace ID
                         │ traceparent: 00-a1b2c3...
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. Orchestrator Service (Port 8085)                             │
│    Span: api-workflow-product                                   │
│    ├─ Receives request                                          │
│    ├─ Starts trace                                              │
│    └─ Orchestrates workflow                                     │
└────────────────────────┬────────────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
         ▼               ▼               ▼
    ┌────────┐     ┌────────┐     ┌────────┐
    │ Step 1 │     │ Step 2 │     │ Step 3 │
    │  HTTP  │     │  MQ    │     │  MQ    │
    └────┬───┘     └────┬───┘     └────┬───┘
         │              │              │
         │              │              │

╔════════════════════════════════════════════════════════════════╗
║ STEP 1: HTTP Product Creation (Synchronous)                   ║
╚════════════════════════════════════════════════════════════════╝

Orchestrator                          CQRS Service
     │                                     │
     │ HTTP POST /api/products             │
     │ Headers:                            │
     │   traceparent: 00-a1b2c3...        │
     │────────────────────────────────────>│
     │                                     │
     │                    ┌────────────────┤
     │                    │ Extract trace  │
     │                    │ Create child   │
     │                    │ span           │
     │                    └────────────────┤
     │                                     │
     │                    ┌────────────────┤
     │                    │ Command Bus    │
     │                    │ dispatch       │
     │                    └────────────────┤
     │                                     │
     │                    ┌────────────────┤
     │                    │ Save to DB     │
     │                    └────────────────┤
     │                                     │
     │ HTTP 201 Created                    │
     │ Body: {productId: "..."}           │
     │<────────────────────────────────────│
     │                                     │

Result: Product created
Trace: HTTP span recorded with trace ID a1b2c3...


╔════════════════════════════════════════════════════════════════╗
║ STEP 2: RabbitMQ Price Update (Asynchronous)                  ║
╚════════════════════════════════════════════════════════════════╝

Orchestrator              RabbitMQ              CQRS Service
     │                       │                       │
     │ Publish message       │                       │
     │ Headers:              │                       │
     │   traceparent:        │                       │
     │   00-a1b2c3...       │                       │
     │──────────────────────>│                       │
     │                       │                       │
     │                       │ Queue message         │
     │                       │ (with trace headers)  │
     │                       │                       │
     │                       │ Consume message       │
     │                       │ Headers:              │
     │                       │   traceparent:        │
     │                       │   00-a1b2c3...       │
     │                       │──────────────────────>│
     │                       │                       │
     │                       │          ┌────────────┤
     │                       │          │ Extract    │
     │                       │          │ trace      │
     │                       │          └────────────┤
     │                       │                       │
     │                       │          ┌────────────┤
     │                       │          │ Command    │
     │                       │          │ Bus        │
     │                       │          └────────────┤
     │                       │                       │
     │                       │          ┌────────────┤
     │                       │          │ Update DB  │
     │                       │          └────────────┤
     │                       │                       │
     │                       │ ACK                   │
     │                       │<──────────────────────│
     │                       │                       │

Result: Price updated
Trace: RabbitMQ span recorded with trace ID a1b2c3...


╔════════════════════════════════════════════════════════════════╗
║ STEP 3: RabbitMQ Stock Update (Asynchronous)                  ║
╚════════════════════════════════════════════════════════════════╝

Orchestrator              RabbitMQ              CQRS Service
     │                       │                       │
     │ Publish message       │                       │
     │ Headers:              │                       │
     │   traceparent:        │                       │
     │   00-a1b2c3...       │                       │
     │──────────────────────>│                       │
     │                       │                       │
     │                       │ Queue message         │
     │                       │ (with trace headers)  │
     │                       │                       │
     │                       │ Consume message       │
     │                       │ Headers:              │
     │                       │   traceparent:        │
     │                       │   00-a1b2c3...       │
     │                       │──────────────────────>│
     │                       │                       │
     │                       │          ┌────────────┤
     │                       │          │ Extract    │
     │                       │          │ trace      │
     │                       │          └────────────┤
     │                       │                       │
     │                       │          ┌────────────┤
     │                       │          │ Command    │
     │                       │          │ Bus        │
     │                       │          └────────────┤
     │                       │                       │
     │                       │          ┌────────────┤
     │                       │          │ Update DB  │
     │                       │          └────────────┤
     │                       │                       │
     │                       │ ACK                   │
     │                       │<──────────────────────│
     │                       │                       │

Result: Stock updated
Trace: RabbitMQ span recorded with trace ID a1b2c3...


╔════════════════════════════════════════════════════════════════╗
║ STEP 4: HTTP Product Query (Synchronous)                      ║
╚════════════════════════════════════════════════════════════════╝

Orchestrator                          CQRS Service
     │                                     │
     │ HTTP GET /api/products/{id}         │
     │ Headers:                            │
     │   traceparent: 00-a1b2c3...        │
     │────────────────────────────────────>│
     │                                     │
     │                    ┌────────────────┤
     │                    │ Extract trace  │
     │                    │ Create child   │
     │                    │ span           │
     │                    └────────────────┤
     │                                     │
     │                    ┌────────────────┤
     │                    │ Query Bus      │
     │                    │ dispatch       │
     │                    └────────────────┤
     │                                     │
     │                    ┌────────────────┤
     │                    │ Query DB       │
     │                    └────────────────┤
     │                                     │
     │ HTTP 200 OK                         │
     │ Body: {product details}            │
     │<────────────────────────────────────│
     │                                     │

Result: Product retrieved
Trace: HTTP span recorded with trace ID a1b2c3...


┌─────────────────────────────────────────────────────────────────┐
│ 5. Final Response to Client                                     │
│    {                                                            │
│      "productId": "550e8400-...",                              │
│      "traceId": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",          │
│      "message": "Workflow completed successfully",             │
│      "steps": { ... }                                          │
│    }                                                            │
└─────────────────────────────────────────────────────────────────┘


╔════════════════════════════════════════════════════════════════╗
║ GRAFANA TRACE VIEW                                             ║
╚════════════════════════════════════════════════════════════════╝

Trace ID: a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6

Timeline View:
┌────────────────────────────────────────────────────────────────┐
│ orchestrator-service: api-workflow-product                     │
│ ├─ orchestrator-service: workflow-product-complete             │
│ │  ├─ orchestrator-service: workflow-step-create-http         │
│ │  │  └─ cqrs-service: api-create-product [HTTP]              │
│ │  │     └─ cqrs-service: command.bus.dispatch                │
│ │  │        └─ cqrs-service: command.handler.execute          │
│ │  │                                                           │
│ │  ├─ orchestrator-service: workflow-step-update-price-mq     │
│ │  │  └─ rabbitmq: publish                                    │
│ │  │     └─ cqrs-service: rabbitmq-command-received [MQ]      │
│ │  │        └─ cqrs-service: rabbitmq-update-price            │
│ │  │           └─ cqrs-service: command.bus.dispatch          │
│ │  │                                                           │
│ │  ├─ orchestrator-service: workflow-step-update-stock-mq     │
│ │  │  └─ rabbitmq: publish                                    │
│ │  │     └─ cqrs-service: rabbitmq-command-received [MQ]      │
│ │  │        └─ cqrs-service: rabbitmq-update-stock            │
│ │  │           └─ cqrs-service: command.bus.dispatch          │
│ │  │                                                           │
│ │  └─ orchestrator-service: workflow-step-query-http          │
│ │     └─ cqrs-service: api-get-product [HTTP]                 │
│ │        └─ cqrs-service: query.bus.dispatch                  │
└────────────────────────────────────────────────────────────────┘

Duration: ~2.5s
Spans: 15
Services: 2 (orchestrator-service, cqrs-service)
Protocols: 2 (HTTP, RabbitMQ)


╔════════════════════════════════════════════════════════════════╗
║ KEY INSIGHTS FROM THE TRACE                                    ║
╚════════════════════════════════════════════════════════════════╝

1. SINGLE TRACE ID
   ✓ All operations share: a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6
   ✓ Easy to find all related operations

2. CROSS-SERVICE VISIBILITY
   ✓ See operations in orchestrator-service
   ✓ See operations in cqrs-service
   ✓ Understand service boundaries

3. CROSS-PROTOCOL VISIBILITY
   ✓ HTTP calls clearly marked
   ✓ RabbitMQ operations clearly marked
   ✓ See protocol transitions

4. TIMING INFORMATION
   ✓ Total workflow duration: ~2.5s
   ✓ HTTP create: ~200ms
   ✓ RabbitMQ price update: ~300ms
   ✓ RabbitMQ stock update: ~300ms
   ✓ HTTP query: ~100ms
   ✓ Identify bottlenecks

5. PARENT-CHILD RELATIONSHIPS
   ✓ See which operation triggered which
   ✓ Understand causality
   ✓ Debug complex flows

6. ERROR TRACKING
   ✓ If any step fails, see exactly where
   ✓ See error messages and stack traces
   ✓ Understand impact on downstream operations


╔════════════════════════════════════════════════════════════════╗
║ LOG CORRELATION                                                ║
╚════════════════════════════════════════════════════════════════╝

All logs include the trace ID:

orchestrator-service.log:
{
  "traceId": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "message": "Starting product workflow"
}

cqrs-service.log:
{
  "traceId": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "message": "Received command via RabbitMQ"
}

Query in Grafana Loki:
{service=~".*"} |= "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6"

Result: ALL logs from ALL services for this request!


╔════════════════════════════════════════════════════════════════╗
║ SUMMARY                                                        ║
╚════════════════════════════════════════════════════════════════╝

ONE TRACE ID provides:
✓ Complete request lifecycle visibility
✓ Cross-service operation tracking
✓ Cross-protocol operation tracking
✓ Timing and performance insights
✓ Error tracking and debugging
✓ Log correlation
✓ Understanding of system behavior

This is the power of distributed tracing!
```

## How to Use This Diagram

1. **Follow the flow** from top to bottom
2. **Notice the trace ID** stays the same throughout
3. **See how headers propagate** the trace context
4. **Understand the timing** of sync vs async operations
5. **Visualize the Grafana view** at the bottom

## Key Concepts Illustrated

- **Trace Propagation**: How trace context flows via headers
- **Protocol Agnostic**: Works with HTTP and RabbitMQ
- **Parent-Child Spans**: Hierarchical relationship of operations
- **Service Boundaries**: Clear separation of concerns
- **Observability**: Complete visibility into the system

---

**Ready to see it in action?** Run `./test_distributed_tracing.sh`
