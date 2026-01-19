# Orchestrator Service

## Overview

The Orchestrator Service demonstrates **distributed tracing across HTTP and RabbitMQ** by orchestrating a business workflow that calls the CQRS service through both communication protocols within a single trace context.

## Purpose

This service helps you understand:
- How trace IDs propagate across different protocols (HTTP, RabbitMQ)
- How to observe the complete lifecycle of a distributed request
- How to debug issues across multiple services and communication channels

## Architecture

```
Client → Orchestrator Service → CQRS Service
              │                      ↑
              │                      │
              └─→ RabbitMQ ──────────┘
              
All operations share ONE trace ID!
```

## Workflow

When you call `POST /api/workflows/product`, the orchestrator executes:

1. **HTTP POST** → Creates product in CQRS service via REST API
2. **RabbitMQ** → Sends price update command via message queue
3. **RabbitMQ** → Sends stock update command via message queue
4. **HTTP GET** → Queries product from CQRS service via REST API

## Quick Start

### 1. Start Infrastructure
```bash
cd ../
docker-compose up -d
```

### 2. Start CQRS Service (Terminal 1)
```bash
./gradlew :cqrs-service:bootRun
```

### 3. Start Orchestrator Service (Terminal 2)
```bash
./gradlew :orchestrator-service:bootRun
```

### 4. Run Test
```bash
./test_distributed_tracing.sh
```

## API Endpoints

### Execute Product Workflow
```bash
POST http://localhost:8085/api/workflows/product
Content-Type: application/json

{
  "name": "Gaming Laptop",
  "description": "High-performance gaming laptop",
  "price": 2499.99,
  "initialStock": 10,
  "updatedPrice": 2299.99,
  "updatedStock": 15
}
```

**Response:**
```json
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
```

### Health Check
```bash
GET http://localhost:8085/api/workflows/health
```

## Configuration

**Port:** 8085

**Dependencies:**
- CQRS Service (localhost:8084)
- RabbitMQ (localhost:5672)
- OpenTelemetry Collector (localhost:4317)

## Observing Traces

1. Copy the `traceId` from the response
2. Open Grafana: http://localhost:3000
3. Go to Explore → Select Tempo
4. Search for the trace ID
5. View the complete trace showing:
   - HTTP calls to CQRS service
   - RabbitMQ message publishing
   - CQRS service processing messages
   - All with the same trace ID!

## Key Features

### Automatic Trace Propagation

**HTTP Calls:**
```java
// WebClient automatically propagates trace context
webClient.post()
    .uri(cqrsServiceUrl + "/api/products")
    .bodyValue(request)
    .retrieve()
    .bodyToMono(Map.class)
    .block();
```

**RabbitMQ Messages:**
```java
// RabbitTemplate automatically propagates trace context
rabbitTemplate.convertAndSend(exchange, routingKey, message);
```

### Observability

All operations are instrumented with `@Observed` annotations:
- `api.workflow.product` - Entry point
- `workflow.product.complete` - Complete workflow
- `workflow.step.create.http` - HTTP product creation
- `workflow.step.update.price.rabbitmq` - RabbitMQ price update
- `workflow.step.update.stock.rabbitmq` - RabbitMQ stock update
- `workflow.step.query.http` - HTTP product query

## Components

### Controllers
- `WorkflowController` - REST API endpoints

### Services
- `ProductWorkflowService` - Orchestrates the workflow

### Configuration
- `RabbitMqConfig` - RabbitMQ with trace propagation
- `WebClientConfig` - WebClient with trace propagation

## Logs

Logs are written to:
- Console (JSON format)
- File: `logs/orchestrator-service.json.log`
- Loki (for aggregation)

All logs include trace context:
```json
{
  "traceId": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "spanId": "1234567890abcdef",
  "message": "Starting product workflow"
}
```

## Use Cases

This pattern is useful for:
- **E-commerce**: Order processing with sync and async operations
- **Inventory**: Product updates via multiple channels
- **Notifications**: Triggering events across services
- **Workflows**: Complex business processes spanning multiple services

## Troubleshooting

### Service won't start
- Check if port 8085 is available
- Verify RabbitMQ is running: `docker ps | grep rabbitmq`
- Check logs: `tail -f logs/orchestrator-service.json.log`

### Workflow fails
- Verify CQRS service is running on port 8084
- Check RabbitMQ queues: http://localhost:15672
- Look for errors in logs with the trace ID

### Trace not visible
- Wait a few seconds for trace to be ingested
- Check OpenTelemetry Collector: `docker logs otel-collector`
- Verify Tempo is running: `docker ps | grep tempo`

## Learn More

See the complete guide: [DISTRIBUTED_TRACING_GUIDE.md](../DISTRIBUTED_TRACING_GUIDE.md)

## Summary

The Orchestrator Service demonstrates how a **single trace ID** can help you observe the complete lifecycle of a request that spans:
- Multiple services
- Multiple protocols (HTTP + RabbitMQ)
- Multiple operations

This gives you complete visibility into your distributed system!
