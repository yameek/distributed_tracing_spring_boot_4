# Distributed Tracing Guide: HTTP + RabbitMQ

## Overview

This guide demonstrates how **distributed tracing with a single trace ID** works across multiple communication protocols (HTTP and RabbitMQ) to help you observe the complete lifecycle of a request.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Client Request                               │
│                    POST /api/workflows/product                       │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
                   ┌─────────────────────┐
                   │ Orchestrator Service │ ◄── Trace ID starts here
                   │   (Port 8085)        │
                   └─────────┬───────────┘
                             │
            ┌────────────────┼────────────────┐
            │                │                │
            ▼                ▼                ▼
    ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
    │  Step 1:     │  │  Step 2:     │  │  Step 3:     │
    │  HTTP POST   │  │  RabbitMQ    │  │  RabbitMQ    │
    │  Create      │  │  Update      │  │  Update      │
    │  Product     │  │  Price       │  │  Stock       │
    └──────┬───────┘  └──────┬───────┘  └──────┬───────┘
           │                 │                 │
           ▼                 ▼                 ▼
    ┌──────────────────────────────────────────────┐
    │          CQRS Service (Port 8084)            │
    │                                              │
    │  ┌────────────┐      ┌─────────────────┐   │
    │  │ REST API   │      │ RabbitMQ        │   │
    │  │ Controller │      │ Listener        │   │
    │  └─────┬──────┘      └────────┬────────┘   │
    │        │                      │             │
    │        └──────────┬───────────┘             │
    │                   ▼                         │
    │            ┌──────────────┐                 │
    │            │ Command Bus  │                 │
    │            └──────┬───────┘                 │
    │                   ▼                         │
    │            ┌──────────────┐                 │
    │            │   Handlers   │                 │
    │            └──────────────┘                 │
    └──────────────────────────────────────────────┘
           │
           ▼
    ┌──────────────┐
    │  Step 4:     │
    │  HTTP GET    │
    │  Query       │
    │  Product     │
    └──────────────┘

All steps share the SAME TRACE ID!
```

## What Was Built

### 1. Enhanced CQRS Service
- **Added**: RabbitMQ listener (`CommandMessageListener`) to receive commands via messaging
- **Added**: New RabbitMQ queue (`cqrs.commands.queue`) for incoming commands
- **Existing**: HTTP REST API for synchronous operations
- **Result**: CQRS service can now be called via both HTTP and RabbitMQ

### 2. Orchestrator Service (New)
- **Purpose**: Demonstrates a business workflow that uses both HTTP and RabbitMQ
- **Port**: 8085
- **Features**:
  - Calls CQRS service via HTTP REST API
  - Sends commands to CQRS service via RabbitMQ
  - All operations within a single trace context

### 3. Complete Workflow
The orchestrator executes this workflow:
1. **HTTP POST** → Create product via REST API
2. **RabbitMQ** → Update price via message
3. **RabbitMQ** → Update stock via message
4. **HTTP GET** → Query product via REST API

## How Trace Propagation Works

### HTTP Trace Propagation
When the orchestrator makes HTTP calls:
```
Orchestrator Service                    CQRS Service
      │                                      │
      │  HTTP Request                        │
      │  Headers:                            │
      │    traceparent: 00-{traceId}-...    │
      │─────────────────────────────────────>│
      │                                      │
      │                      Extracts trace context
      │                      Creates child span
      │                      Processes request
      │                                      │
      │  HTTP Response                       │
      │<─────────────────────────────────────│
```

### RabbitMQ Trace Propagation
When the orchestrator sends messages:
```
Orchestrator Service                    CQRS Service
      │                                      │
      │  RabbitMQ Message                    │
      │  Headers:                            │
      │    traceparent: 00-{traceId}-...    │
      │─────────────────────────────────────>│
      │                                      │
      │                      Extracts trace context
      │                      Creates child span
      │                      Processes message
```

### Key Points
- **Automatic**: Spring Boot's OpenTelemetry starter handles propagation
- **Transparent**: No manual header manipulation needed
- **Consistent**: Same trace ID across all protocols
- **Standard**: Uses W3C Trace Context format

## Setup Instructions

### Prerequisites
1. Infrastructure running:
```bash
cd tracing-demo-v2
docker-compose up -d
```

2. Verify services are healthy:
```bash
docker-compose ps
```

### Start Services

**Terminal 1 - CQRS Service:**
```bash
./gradlew :cqrs-service:bootRun
```

**Terminal 2 - Orchestrator Service:**
```bash
./gradlew :orchestrator-service:bootRun
# Or use the convenience script:
./start_orchestrator.sh
```

### Run the Test

```bash
./test_distributed_tracing.sh
```

This script will:
1. Execute the complete workflow
2. Display the response with trace ID
3. Show you how to find the trace in Grafana

## Observing the Trace

### In Grafana (Recommended)

1. **Open Grafana**: http://localhost:3000
2. **Go to Explore**: Click the compass icon
3. **Select Tempo**: Choose Tempo as data source
4. **Search**: Paste the trace ID from the test output
5. **Analyze**: You'll see the complete trace with all spans

### What You'll See in the Trace

```
Trace Timeline (Single Trace ID):

orchestrator-service: api-workflow-product (ROOT SPAN)
├─ orchestrator-service: workflow-product-complete
│  ├─ orchestrator-service: workflow-step-create-http
│  │  └─ cqrs-service: api-create-product (HTTP)
│  │     └─ cqrs-service: command.bus.dispatch
│  │        └─ cqrs-service: command.handler.execute
│  │           └─ cqrs-service: outbox.save
│  │
│  ├─ orchestrator-service: workflow-step-update-price-rabbitmq
│  │  └─ rabbitmq: publish
│  │     └─ cqrs-service: rabbitmq-command-received (RabbitMQ)
│  │        └─ cqrs-service: rabbitmq-update-price
│  │           └─ cqrs-service: command.bus.dispatch
│  │
│  ├─ orchestrator-service: workflow-step-update-stock-rabbitmq
│  │  └─ rabbitmq: publish
│  │     └─ cqrs-service: rabbitmq-command-received (RabbitMQ)
│  │        └─ cqrs-service: rabbitmq-update-stock
│  │           └─ cqrs-service: command.bus.dispatch
│  │
│  └─ orchestrator-service: workflow-step-query-http
│     └─ cqrs-service: api-get-product (HTTP)
│        └─ cqrs-service: query.bus.dispatch
```

### Trace Insights

From the trace, you can observe:

1. **Request Lifecycle**: Complete journey from initial request to final response
2. **Service Boundaries**: Which service handles which part
3. **Protocol Transitions**: HTTP → RabbitMQ → HTTP transitions
4. **Timing**: How long each operation takes
5. **Dependencies**: Parent-child relationships between operations
6. **Bottlenecks**: Which operations are slow
7. **Errors**: Where failures occur (if any)

## Benefits of This Approach

### 1. Unified Observability
- Single trace ID for the entire business transaction
- No matter how many services or protocols involved
- Easy to debug issues across the stack

### 2. Protocol Agnostic
- Works with HTTP, RabbitMQ, gRPC, Kafka, etc.
- Trace context automatically propagated
- No manual instrumentation needed

### 3. Real-World Scenarios
This pattern is common in microservices:
- **Synchronous operations**: HTTP for immediate responses
- **Asynchronous operations**: Messaging for background tasks
- **Mixed workflows**: Combining both in a single business flow

### 4. Debugging Power
When something goes wrong:
```
User: "My order update failed!"
You: "What's the order ID?"
User: "ORDER-12345"

→ Search logs for ORDER-12345
→ Find trace ID in logs
→ View complete trace in Grafana
→ See exactly where it failed
→ See timing of each step
→ See which service had the issue
```

## Example Use Cases

### E-Commerce Order Processing
```
1. HTTP: Create order (immediate response to user)
2. RabbitMQ: Reserve inventory (async)
3. RabbitMQ: Process payment (async)
4. RabbitMQ: Send notification (async)
5. HTTP: Query order status (user checks status)

All tracked with one trace ID!
```

### Product Management (This Demo)
```
1. HTTP: Create product (immediate)
2. RabbitMQ: Update price (async)
3. RabbitMQ: Update stock (async)
4. HTTP: Verify changes (immediate)

All tracked with one trace ID!
```

## Configuration Details

### Orchestrator Service Configuration

**application.yml:**
```yaml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0  # Trace 100% of requests
  opentelemetry:
    tracing:
      export:
        otlp:
          enabled: true
          endpoint: http://localhost:4317
```

**RabbitMQ Configuration:**
```java
@Bean
public RabbitTemplate rabbitTemplate(
        ConnectionFactory connectionFactory,
        MessageConverter messageConverter) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(messageConverter);
    template.setObservationEnabled(true);  // ← Enables trace propagation
    return template;
}
```

**WebClient Configuration:**
```java
@Bean
public WebClient webClient(WebClient.Builder builder) {
    return builder.build();  // Auto-instrumented by Spring Boot
}
```

### CQRS Service Configuration

**RabbitMQ Listener:**
```java
@Bean
public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
        ConnectionFactory connectionFactory,
        MessageConverter messageConverter) {
    SimpleRabbitListenerContainerFactory factory = 
        new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(messageConverter);
    factory.setObservationEnabled(true);  // ← Enables trace propagation
    return factory;
}
```

## Testing Different Scenarios

### Scenario 1: Success Path
```bash
./test_distributed_tracing.sh
```
Observe: Complete trace with all steps successful

### Scenario 2: Simulate Failure
Stop the CQRS service:
```bash
# Stop cqrs-service
# Run test again
./test_distributed_tracing.sh
```
Observe: Trace shows where the failure occurred

### Scenario 3: Performance Analysis
Run multiple requests:
```bash
for i in {1..5}; do
  ./test_distributed_tracing.sh
  sleep 2
done
```
Observe: Compare traces to find performance patterns

## Advanced: Trace Context in Logs

All logs include trace context:
```json
{
  "@timestamp": "2026-01-19T10:30:45.123Z",
  "level": "INFO",
  "message": "Processing UpdatePrice command",
  "traceId": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "spanId": "1234567890abcdef",
  "service": "cqrs-service"
}
```

### Query Logs by Trace ID
In Grafana Loki:
```
{service="orchestrator-service"} |= "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6"
```
or
```
{service="cqrs-service"} |= "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6"
```

This shows all logs across all services for that trace!

## Troubleshooting

### Trace Not Appearing in Grafana

1. **Check OpenTelemetry Collector**:
```bash
docker logs otel-collector
```

2. **Check Tempo**:
```bash
docker logs tempo
```

3. **Verify services are sending traces**:
```bash
# Check orchestrator logs
tail -f logs/orchestrator-service.json.log | grep traceId

# Check cqrs logs
tail -f logs/cqrs-service.json.log | grep traceId
```

### RabbitMQ Messages Not Processed

1. **Check RabbitMQ Management UI**: http://localhost:15672
   - Username: guest
   - Password: guest

2. **Verify queues exist**:
   - `cqrs.commands.queue` should exist
   - Check for messages

3. **Check CQRS service logs**:
```bash
tail -f logs/cqrs-service.json.log | grep "Received command via RabbitMQ"
```

## Key Takeaways

1. ✅ **Single Trace ID** spans multiple services and protocols
2. ✅ **Automatic Propagation** via Spring Boot OpenTelemetry
3. ✅ **HTTP and RabbitMQ** both support trace context
4. ✅ **Complete Visibility** into request lifecycle
5. ✅ **Production Ready** pattern for microservices
6. ✅ **Easy Debugging** with correlated logs and traces

## Next Steps

1. **Explore the traces** in Grafana
2. **Correlate logs** with traces using trace IDs
3. **Add more services** to the workflow
4. **Try other protocols** (gRPC, Kafka, etc.)
5. **Set up alerts** based on trace data

## Summary

This demo shows how distributed tracing helps you **observe the complete lifecycle of a request** across:
- Multiple services (orchestrator, cqrs)
- Multiple protocols (HTTP, RabbitMQ)
- Multiple operations (create, update, query)

All connected by a **single trace ID** that flows automatically through your entire system, giving you complete visibility into what's happening in your distributed architecture.
