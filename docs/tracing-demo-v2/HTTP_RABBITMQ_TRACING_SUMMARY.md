# HTTP + RabbitMQ Distributed Tracing - Implementation Summary

## What Was Implemented

A complete demonstration of **distributed tracing across HTTP and RabbitMQ** that shows how a single trace ID can help you observe the entire lifecycle of a request across multiple services and communication protocols.

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────────┐
│                    Single Trace ID Flow                           │
└──────────────────────────────────────────────────────────────────┘

    Client Request
         │
         ▼
┌─────────────────────┐
│ Orchestrator Service│  ← Trace ID starts here
│    (Port 8085)      │
└──────┬──────────────┘
       │
       ├─ Step 1: HTTP POST (Create Product)
       │     │
       │     ▼
       │  ┌──────────────┐
       │  │ CQRS Service │
       │  │ REST API     │
       │  └──────────────┘
       │
       ├─ Step 2: RabbitMQ (Update Price)
       │     │
       │     ▼
       │  ┌──────────────┐
       │  │  RabbitMQ    │
       │  │   Queue      │
       │  └──────┬───────┘
       │         │
       │         ▼
       │  ┌──────────────┐
       │  │ CQRS Service │
       │  │ MQ Listener  │
       │  └──────────────┘
       │
       ├─ Step 3: RabbitMQ (Update Stock)
       │     │
       │     ▼
       │  ┌──────────────┐
       │  │  RabbitMQ    │
       │  │   Queue      │
       │  └──────┬───────┘
       │         │
       │         ▼
       │  ┌──────────────┐
       │  │ CQRS Service │
       │  │ MQ Listener  │
       │  └──────────────┘
       │
       └─ Step 4: HTTP GET (Query Product)
             │
             ▼
          ┌──────────────┐
          │ CQRS Service │
          │ REST API     │
          └──────────────┘

ALL OPERATIONS SHARE THE SAME TRACE ID!
```

## Components Created

### 1. Enhanced CQRS Service

#### New Files:
- `cqrs-service/src/main/java/com/example/tracing/cqrs/messaging/CommandMessageListener.java`
  - RabbitMQ listener for incoming commands
  - Receives commands via messaging (async)
  - Automatically extracts trace context from message headers
  - Dispatches commands to the Command Bus

#### Modified Files:
- `cqrs-service/src/main/java/com/example/tracing/cqrs/config/RabbitMqConfig.java`
  - Added new queue: `cqrs.commands.queue`
  - Added new exchange: `cqrs.commands.exchange`
  - Enabled observation on RabbitTemplate and listener factory
  - Configured trace propagation for both sending and receiving

### 2. New Orchestrator Service

**Location:** `orchestrator-service/`

**Purpose:** Demonstrates distributed tracing by orchestrating a workflow that uses both HTTP and RabbitMQ to call the CQRS service.

#### Files Created:
```
orchestrator-service/
├── build.gradle
├── README.md
└── src/main/
    ├── java/com/example/tracing/orchestrator/
    │   ├── OrchestratorServiceApplication.java
    │   ├── controller/
    │   │   └── WorkflowController.java
    │   ├── service/
    │   │   └── ProductWorkflowService.java
    │   ├── dto/
    │   │   ├── ProductWorkflowRequest.java
    │   │   └── ProductWorkflowResponse.java
    │   └── config/
    │       ├── RabbitMqConfig.java
    │       └── WebClientConfig.java
    └── resources/
        ├── application.yml
        └── logback-spring.xml
```

### 3. Testing and Documentation

#### Scripts:
- `test_distributed_tracing.sh` - Automated test script
- `start_orchestrator.sh` - Convenience startup script

#### Documentation:
- `DISTRIBUTED_TRACING_GUIDE.md` - Comprehensive guide
- `orchestrator-service/README.md` - Service-specific documentation
- `HTTP_RABBITMQ_TRACING_SUMMARY.md` - This file

## How It Works

### Workflow Execution

When you call the orchestrator's workflow endpoint:

```bash
POST http://localhost:8085/api/workflows/product
```

The following happens:

1. **Orchestrator receives request**
   - Spring Boot creates a new trace ID
   - Trace context is stored in the current thread

2. **Step 1: HTTP Product Creation**
   - Orchestrator calls CQRS service via WebClient
   - WebClient automatically adds `traceparent` header
   - CQRS service extracts trace context from header
   - Creates child span for processing
   - Returns response

3. **Step 2: RabbitMQ Price Update**
   - Orchestrator sends message via RabbitTemplate
   - RabbitTemplate automatically adds trace context to message headers
   - Message is queued in RabbitMQ
   - CQRS listener receives message
   - Listener factory extracts trace context from headers
   - Creates child span for processing

4. **Step 3: RabbitMQ Stock Update**
   - Same as Step 2, but for stock update

5. **Step 4: HTTP Product Query**
   - Same as Step 1, but for querying

### Trace Context Propagation

#### HTTP (W3C Trace Context)
```
traceparent: 00-{trace-id}-{parent-span-id}-{flags}
```

Example:
```
traceparent: 00-a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6-1234567890abcdef-01
```

#### RabbitMQ (Message Headers)
```
Message Headers:
  traceparent: 00-{trace-id}-{parent-span-id}-{flags}
```

Spring Boot's OpenTelemetry starter handles this automatically!

## Key Features

### ✅ Automatic Trace Propagation
- No manual header manipulation
- Works across HTTP and RabbitMQ
- Uses W3C Trace Context standard

### ✅ Complete Observability
- Every operation creates a span
- Parent-child relationships preserved
- Timing information captured

### ✅ Protocol Agnostic
- HTTP REST calls
- RabbitMQ messaging
- Easy to extend to other protocols

### ✅ Production Ready
- Proper error handling
- Structured logging with trace IDs
- Metrics and monitoring

## Running the Demo

### Step 1: Start Infrastructure
```bash
cd tracing-demo-v2
docker-compose up -d
```

### Step 2: Start Services

**Terminal 1 - CQRS Service:**
```bash
./gradlew :cqrs-service:bootRun
```

**Terminal 2 - Orchestrator Service:**
```bash
./gradlew :orchestrator-service:bootRun
```

### Step 3: Run Test
```bash
./test_distributed_tracing.sh
```

### Step 4: View Trace

The test script will output:
```
Trace ID: a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6
Product ID: 550e8400-e29b-41d4-a716-446655440000
```

1. Open Grafana: http://localhost:3000
2. Go to Explore → Tempo
3. Paste the trace ID
4. View the complete trace!

## What You'll See in the Trace

```
Root Span: orchestrator-service - api-workflow-product
│
├─ orchestrator-service - workflow-product-complete
│  │
│  ├─ orchestrator-service - workflow-step-create-http
│  │  └─ cqrs-service - api-create-product (HTTP)
│  │     └─ cqrs-service - command.bus.dispatch
│  │        └─ cqrs-service - command.handler.execute
│  │
│  ├─ orchestrator-service - workflow-step-update-price-rabbitmq
│  │  └─ rabbitmq - publish
│  │     └─ cqrs-service - rabbitmq-command-received (RabbitMQ)
│  │        └─ cqrs-service - rabbitmq-update-price
│  │           └─ cqrs-service - command.bus.dispatch
│  │
│  ├─ orchestrator-service - workflow-step-update-stock-rabbitmq
│  │  └─ rabbitmq - publish
│  │     └─ cqrs-service - rabbitmq-command-received (RabbitMQ)
│  │        └─ cqrs-service - rabbitmq-update-stock
│  │           └─ cqrs-service - command.bus.dispatch
│  │
│  └─ orchestrator-service - workflow-step-query-http
│     └─ cqrs-service - api-get-product (HTTP)
│        └─ cqrs-service - query.bus.dispatch
```

## Benefits Demonstrated

### 1. Complete Request Lifecycle Visibility
You can see:
- Where the request started
- Which services it touched
- What operations were performed
- How long each step took
- Where errors occurred (if any)

### 2. Cross-Protocol Tracing
The same trace ID flows through:
- HTTP requests (synchronous)
- RabbitMQ messages (asynchronous)
- Database operations
- Business logic

### 3. Debugging Power

**Scenario:** User reports an issue
```
User: "My product update failed!"
You: "What's the product ID?"
User: "PROD-12345"

→ Search logs for PROD-12345
→ Find trace ID in logs
→ Search Grafana for trace ID
→ See complete flow
→ Identify exact failure point
→ See timing and context
→ Fix the issue!
```

### 4. Performance Analysis
- Identify slow operations
- Compare traces to find patterns
- Optimize bottlenecks
- Monitor SLAs

## Real-World Use Cases

### E-Commerce Order Processing
```
1. HTTP: Create order (sync - user waits)
2. RabbitMQ: Reserve inventory (async)
3. RabbitMQ: Process payment (async)
4. RabbitMQ: Send notification (async)
5. HTTP: Query order status (sync - user checks)

One trace ID for the entire order lifecycle!
```

### Microservices Workflow
```
1. API Gateway: HTTP request
2. Service A: Process via HTTP
3. Service A → RabbitMQ: Publish event
4. Service B: Consume from RabbitMQ
5. Service B → Service C: HTTP call
6. Service C: Final processing

One trace ID across all services!
```

## Configuration Highlights

### Orchestrator Service

**Enable Tracing:**
```yaml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0
  opentelemetry:
    tracing:
      export:
        otlp:
          enabled: true
          endpoint: http://localhost:4317
```

**Enable RabbitMQ Trace Propagation:**
```java
@Bean
public RabbitTemplate rabbitTemplate(...) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setObservationEnabled(true);  // ← Key line
    return template;
}
```

### CQRS Service

**Enable RabbitMQ Listener Trace Propagation:**
```java
@Bean
public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(...) {
    SimpleRabbitListenerContainerFactory factory = 
        new SimpleRabbitListenerContainerFactory();
    factory.setObservationEnabled(true);  // ← Key line
    return factory;
}
```

## Technical Details

### Dependencies Added
- `spring-boot-starter-opentelemetry` - Auto-instrumentation
- `spring-boot-starter-webflux` - WebClient for HTTP calls
- `spring-boot-starter-amqp` - RabbitMQ support
- `loki-logback-appender` - Log aggregation
- `logstash-logback-encoder` - Structured logging

### Ports
- Orchestrator Service: 8085
- CQRS Service: 8084
- RabbitMQ: 5672 (AMQP), 15672 (Management UI)
- OpenTelemetry Collector: 4317 (gRPC)
- Grafana: 3000
- Tempo: 3200
- Loki: 3100

### Queues Created
- `cqrs.commands.queue` - Incoming commands to CQRS service
- `cqrs.events.queue` - Outgoing events from CQRS service (existing)

## Observability Stack

### Traces
- **Collected by**: OpenTelemetry Collector
- **Stored in**: Tempo
- **Viewed in**: Grafana

### Logs
- **Format**: JSON with trace IDs
- **Stored in**: Loki
- **Viewed in**: Grafana
- **Correlation**: Use trace ID to find all logs

### Metrics
- **Exposed at**: `/actuator/prometheus`
- **Format**: Prometheus format
- **Includes**: Request counts, durations, errors

## Testing Scenarios

### 1. Happy Path
```bash
./test_distributed_tracing.sh
```
Result: Complete trace with all steps successful

### 2. Service Failure
Stop CQRS service, run test
Result: Trace shows where failure occurred

### 3. Performance Test
```bash
for i in {1..10}; do
  ./test_distributed_tracing.sh
done
```
Result: Multiple traces to compare performance

### 4. Concurrent Requests
Run test from multiple terminals
Result: Each request has unique trace ID

## Troubleshooting

### Trace Not Visible
1. Check OpenTelemetry Collector: `docker logs otel-collector`
2. Check Tempo: `docker logs tempo`
3. Wait a few seconds for ingestion
4. Verify trace ID in logs

### RabbitMQ Issues
1. Check RabbitMQ UI: http://localhost:15672
2. Verify queues exist
3. Check for messages in queues
4. Review CQRS service logs

### Service Won't Start
1. Check port availability
2. Verify dependencies are running
3. Review logs for errors
4. Check configuration

## Key Takeaways

1. ✅ **Single Trace ID** spans multiple services and protocols
2. ✅ **Automatic Propagation** via Spring Boot OpenTelemetry
3. ✅ **HTTP and RabbitMQ** both support trace context
4. ✅ **Complete Visibility** into request lifecycle
5. ✅ **Production Ready** pattern
6. ✅ **Easy to Implement** with minimal configuration
7. ✅ **Powerful Debugging** with correlated logs and traces

## Summary

This implementation demonstrates how **distributed tracing helps you observe the complete lifecycle of a request** across:

- ✅ Multiple services (orchestrator, cqrs)
- ✅ Multiple protocols (HTTP, RabbitMQ)
- ✅ Multiple operations (create, update, query)
- ✅ Synchronous and asynchronous flows

All connected by a **single trace ID** that flows automatically through your entire system, giving you complete visibility into your distributed architecture.

## Next Steps

1. ✅ Run the demo and explore traces in Grafana
2. ✅ Correlate logs with traces using trace IDs
3. ✅ Add more services to the workflow
4. ✅ Try other protocols (gRPC, Kafka)
5. ✅ Set up alerts based on trace data
6. ✅ Implement in your own services

## Questions Answered

**Q: Can I link HTTP and RabbitMQ calls in a single trace?**
✅ Yes! This demo shows exactly how.

**Q: How does trace context propagate across RabbitMQ?**
✅ Automatically via message headers when observation is enabled.

**Q: Can I observe the complete lifecycle of a request?**
✅ Yes! The trace shows every step from start to finish.

**Q: Is this production ready?**
✅ Yes! Uses Spring Boot's official OpenTelemetry starter.

**Q: How do I debug issues across services?**
✅ Use the trace ID to find all related logs and spans.

---

**For detailed instructions, see:** [DISTRIBUTED_TRACING_GUIDE.md](DISTRIBUTED_TRACING_GUIDE.md)
