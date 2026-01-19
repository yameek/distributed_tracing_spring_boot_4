# Testing Guide

This guide explains how to test the complete distributed tracing system.

## Prerequisites

### 1. Start Infrastructure
```bash
docker compose up -d
```

Verify all containers are running:
```bash
docker compose ps
```

You should see:
- ✅ rabbitmq (healthy)
- ✅ postgres (healthy)
- ✅ otel-collector
- ✅ tempo
- ✅ loki
- ✅ grafana

### 2. Start All Services
```bash
./run_all.sh
```

This starts:
- GraphQL Service (8080)
- Order Service (8081)
- Inventory Service (8082)
- Notification Service (8083)
- CQRS Service (8084)
- Orchestrator Service (8085)

**Wait 30-60 seconds** for all services to fully start.

## Test Scripts

### 1. Complete System Test (Recommended)
Tests all services and features:
```bash
./test_complete_system.sh
```

**What it tests:**
- ✅ Infrastructure (Docker services)
- ✅ All 6 application services
- ✅ GraphQL → Order → Inventory/Notification flow
- ✅ CQRS service CRUD operations
- ✅ Distributed tracing (HTTP + RabbitMQ)
- ✅ Trace ID propagation
- ✅ Log correlation

**Expected output:**
```
================================================================
  Complete System Test - Distributed Tracing Demo
================================================================

Step 1: Checking Infrastructure
================================================
✓ All services running

Step 2: Checking Application Services
================================================
✓ All 6 services operational

Step 3: Testing Core Flow
================================================
✓ Order created
✓ Messages processed

Step 4: Testing CQRS Service
================================================
✓ Product created
✓ Product retrieved
✓ Price updated

Step 5: Testing Distributed Tracing
================================================
✓ Workflow completed
✓ Trace ID: [trace-id]

Step 6: Checking Logs
================================================
✓ Trace ID found in logs

✓ System Test Complete!
```

### 2. Distributed Tracing Test
Tests HTTP + RabbitMQ tracing specifically:
```bash
./test_distributed_tracing.sh
```

**What it tests:**
- ✅ Orchestrator service workflow
- ✅ HTTP calls to CQRS service
- ✅ RabbitMQ message sending
- ✅ CQRS service message consumption
- ✅ Single trace ID across all operations

**Expected output:**
```
Distributed Tracing Test: HTTP + RabbitMQ
==================================================

✓ Orchestrator service is running
✓ CQRS service is running

Executing product workflow...
✓ Workflow completed

Trace ID: [trace-id]
Product ID: [product-id]

How to observe the trace:
1. Open Grafana: http://localhost:3000
2. Go to Explore → Tempo
3. Search for trace ID: [trace-id]
```

### 3. Quick Test
Quick health check of all services:
```bash
./quick_test.sh
```

**What it tests:**
- ✅ All services responding
- ✅ Basic GraphQL order creation
- ✅ Basic CQRS product creation

### 4. Individual Service Tests

**CQRS Service:**
```bash
./test_cqrs_service.sh
```

**Core Services:**
```bash
./test_system.sh
```

## Manual Testing

### Test 1: GraphQL Order Flow

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation { createOrder(productId: \"LAPTOP-001\", quantity: 2) { orderId status message } }"
  }'
```

**Expected:**
- Order created with ID
- Messages sent to RabbitMQ
- Inventory service processes message
- Notification service processes message
- All with same trace ID

### Test 2: CQRS Product Management

**Create Product:**
```bash
curl -X POST http://localhost:8084/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Laptop",
    "description": "High-performance laptop",
    "price": 1999.99,
    "initialStock": 50
  }'
```

**Get Product:**
```bash
curl http://localhost:8084/api/products/{productId}
```

**Update Price:**
```bash
curl -X PUT http://localhost:8084/api/products/{productId}/price \
  -H "Content-Type: application/json" \
  -d '{"newPrice": 1799.99}'
```

**Update Stock:**
```bash
curl -X PUT http://localhost:8084/api/products/{productId}/stock \
  -H "Content-Type: application/json" \
  -d '{"quantity": 75}'
```

### Test 3: Distributed Tracing Workflow

```bash
curl -X POST http://localhost:8085/api/workflows/product \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Gaming Laptop",
    "description": "Ultimate gaming laptop",
    "price": 3499.99,
    "initialStock": 20,
    "updatedPrice": 3299.99,
    "updatedStock": 25
  }'
```

**Expected:**
- Product created via HTTP
- Price updated via RabbitMQ
- Stock updated via RabbitMQ
- Product queried via HTTP
- Single trace ID for all operations

## Viewing Traces in Grafana

### Step 1: Open Grafana
```
http://localhost:3000
```

### Step 2: Go to Explore
Click the compass icon on the left sidebar

### Step 3: Select Tempo
Choose "Tempo" from the data source dropdown

### Step 4: Search for Trace
- **By Trace ID**: Paste the trace ID from test output
- **By Service**: Select "orchestrator-service" or "cqrs-service"
- **By Time**: Select recent time range

### Step 5: Analyze Trace
You'll see:
- **Span tree**: Hierarchical view of operations
- **Timeline**: When each operation occurred
- **Duration**: How long each operation took
- **Tags**: Metadata about each operation
- **Logs**: Log entries for each span

### Example Trace Structure
```
orchestrator-service: api-workflow-product
├─ orchestrator-service: workflow-product-complete
│  ├─ orchestrator-service: workflow-step-create-http
│  │  └─ cqrs-service: api-create-product [HTTP]
│  │     └─ cqrs-service: command.bus.dispatch
│  │
│  ├─ orchestrator-service: workflow-step-update-price-rabbitmq
│  │  └─ rabbitmq: publish
│  │     └─ cqrs-service: rabbitmq-command-received [RabbitMQ]
│  │        └─ cqrs-service: command.bus.dispatch
│  │
│  ├─ orchestrator-service: workflow-step-update-stock-rabbitmq
│  │  └─ rabbitmq: publish
│  │     └─ cqrs-service: rabbitmq-command-received [RabbitMQ]
│  │        └─ cqrs-service: command.bus.dispatch
│  │
│  └─ orchestrator-service: workflow-step-query-http
│     └─ cqrs-service: api-get-product [HTTP]
│        └─ cqrs-service: query.bus.dispatch
```

## Viewing Logs in Grafana

### Step 1: Go to Explore
Click the compass icon

### Step 2: Select Loki
Choose "Loki" from the data source dropdown

### Step 3: Query Logs

**All logs from a service:**
```
{service="orchestrator-service"}
```

**Logs for a specific trace:**
```
{service=~".*"} |= "your-trace-id-here"
```

**Logs with errors:**
```
{service=~".*"} |= "ERROR"
```

**Logs from multiple services:**
```
{service=~"orchestrator-service|cqrs-service"}
```

## Troubleshooting

### Services Won't Start

**Check ports:**
```bash
for port in 8080 8081 8082 8083 8084 8085; do
  lsof -i :$port
done
```

**Stop all services:**
```bash
./stop_all.sh
```

**Restart:**
```bash
./run_all.sh
```

### Infrastructure Issues

**Check Docker:**
```bash
docker compose ps
docker compose logs
```

**Restart infrastructure:**
```bash
docker compose down
docker compose up -d
```

### Traces Not Appearing

**Check OpenTelemetry Collector:**
```bash
docker logs otel-collector
```

**Check Tempo:**
```bash
docker logs tempo
```

**Wait longer:**
Traces can take 5-10 seconds to appear in Grafana

### RabbitMQ Messages Not Processing

**Check RabbitMQ UI:**
```
http://localhost:15672
Username: guest
Password: guest
```

**Check queues:**
- `cqrs.commands.queue` - should exist
- `cqrs.events.queue` - should exist

**Check service logs:**
```bash
tail -f logs/cqrs-service.log | grep "RabbitMQ"
```

### Build Failures

**Clean build:**
```bash
./gradlew clean build -x test
```

**Check Java version:**
```bash
java -version
```
Should be Java 25 LTS

**Check Gradle version:**
```bash
./gradlew --version
```
Should be Gradle 9.2.1

## Test Results Interpretation

### ✅ Success Indicators
- All services respond to health checks
- Orders are created successfully
- Products are created and updated
- Trace IDs appear in responses
- Trace IDs appear in logs
- Traces visible in Grafana
- Logs correlated by trace ID

### ⚠️ Warning Indicators
- RabbitMQ messages delayed (normal, wait longer)
- Trace not immediately in Grafana (normal, wait 10s)
- Some logs missing trace ID (check service startup)

### ❌ Failure Indicators
- Services not responding
- Errors in logs
- No trace IDs in responses
- Docker containers not running
- Ports already in use

## Performance Expectations

### Service Startup Times
- Infrastructure: 10-15 seconds
- Each service: 15-30 seconds
- Total system: 60-90 seconds

### Request Latencies
- GraphQL order: 100-300ms
- CQRS product creation: 50-200ms
- Distributed workflow: 2-3 seconds (includes async delays)

### Trace Ingestion
- Trace appears in Grafana: 5-10 seconds
- Logs appear in Loki: 2-5 seconds

## Continuous Testing

### Watch Logs
```bash
# All services
tail -f logs/*.log

# Specific service
tail -f logs/orchestrator-service.log

# With trace IDs
tail -f logs/*.log | grep "traceId"
```

### Monitor Services
```bash
# Health checks
watch -n 5 'curl -s http://localhost:8085/api/workflows/health'

# RabbitMQ queues
watch -n 5 'curl -s -u guest:guest http://localhost:15672/api/queues | jq'
```

## Summary

Use these test scripts in order:
1. ✅ `./run_all.sh` - Start everything
2. ✅ `./quick_test.sh` - Quick health check
3. ✅ `./test_complete_system.sh` - Full system test
4. ✅ `./test_distributed_tracing.sh` - Specific tracing test
5. ✅ View traces in Grafana
6. ✅ `./stop_all.sh` - Stop everything

For detailed information, see:
- [QUICK_START_HTTP_RABBITMQ.md](QUICK_START_HTTP_RABBITMQ.md)
- [DISTRIBUTED_TRACING_GUIDE.md](DISTRIBUTED_TRACING_GUIDE.md)
- [IMPLEMENTATION_COMPLETE.md](IMPLEMENTATION_COMPLETE.md)
