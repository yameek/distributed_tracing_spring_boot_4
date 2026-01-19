# test_tracing_complete.sh - Complete Architecture Test Update

## Overview

Updated `test_tracing_complete.sh` to test **ALL architecture use cases** of the distributed tracing demo project.

## What Was Added

### 1. Orchestrator Service Testing (NEW!)
- ✅ Checks if Orchestrator Service (port 8085) is running
- ✅ Tests the complete HTTP + RabbitMQ distributed tracing workflow
- ✅ Verifies trace ID propagation across both HTTP and RabbitMQ
- ✅ Validates product updates via RabbitMQ messages

### 2. Enhanced CQRS Service Testing
- ✅ Tests Command pattern (Create Product)
- ✅ Tests Query pattern (Get Product)
- ✅ Tests Event Sourcing (Price Update via Outbox)
- ✅ Verifies the complete CQRS flow

### 3. Comprehensive Trace Verification
- ✅ HTTP trace propagation (GraphQL → Order)
- ✅ RabbitMQ trace propagation (Order → Inventory/Notification)
- ✅ Orchestrator workflow trace propagation (HTTP + RabbitMQ)
- ✅ Cross-service trace correlation in logs

### 4. Detailed Test Summary
- ✅ 9-step testing process (was 6 steps)
- ✅ Tests 6 different architecture components
- ✅ Provides detailed pass/fail reporting
- ✅ Shows trace IDs for both flows (GraphQL and Orchestrator)

## Test Flow

### Step 1: Service Health Checks
- GraphQL Service (8080)
- Order Service (8081)
- Inventory Service (8082)
- Notification Service (8083)
- CQRS Service (8084)
- **Orchestrator Service (8085)** ← NEW!

### Step 2: GraphQL Flow Test
- Creates order via GraphQL mutation
- Tests: GraphQL → Order → RabbitMQ → Inventory + Notification

### Step 3: CQRS Service Test
- Creates product (Command)
- Queries product (Query)
- Updates price (Event Sourcing)

### Step 4: Orchestrator Workflow Test ← NEW!
- Executes complete HTTP + RabbitMQ workflow
- Tests distributed tracing across protocols
- Verifies:
  - HTTP: Create product
  - RabbitMQ: Price update message
  - RabbitMQ: Stock update message
  - HTTP: Query product
- Validates final product state

### Step 5: Log Trace ID Verification
- Checks all service logs for trace IDs
- Includes Orchestrator service logs ← NEW!

### Step 6: HTTP Trace Propagation
- Verifies trace ID propagation via HTTP

### Step 7: RabbitMQ Trace Propagation ← NEW!
- Verifies trace ID propagation via RabbitMQ

### Step 8: Orchestrator Trace Correlation ← NEW!
- Verifies workflow trace ID in CQRS logs
- Confirms trace propagation across HTTP and RabbitMQ

### Step 9: Comprehensive Summary
- 6 test categories with pass/fail status
- Detailed reporting of all architecture components

## Architecture Use Cases Tested

| Use Case | Description | Status |
|----------|-------------|--------|
| **GraphQL Entry Point** | GraphQL mutation creates orders | ✅ Tested |
| **HTTP Communication** | GraphQL → Order Service | ✅ Tested |
| **RabbitMQ Async** | Order → Inventory/Notification | ✅ Tested |
| **CQRS Command** | Create/Update products | ✅ Tested |
| **CQRS Query** | Read product data | ✅ Tested |
| **Event Sourcing** | Outbox pattern for events | ✅ Tested |
| **Orchestrator HTTP** | HTTP calls to CQRS service | ✅ Tested |
| **Orchestrator RabbitMQ** | Publishes messages to RabbitMQ | ✅ Tested |
| **HTTP Trace Propagation** | Trace IDs via HTTP headers | ✅ Tested |
| **RabbitMQ Trace Propagation** | Trace IDs via message headers | ✅ Tested |
| **Cross-Protocol Tracing** | Single trace across HTTP + RabbitMQ | ✅ Tested |
| **Log Correlation** | Trace IDs in all service logs | ✅ Tested |

## Output Example

```bash
╔════════════════════════════════════════════════════════════════╗
║              🎉 ALL ARCHITECTURE USE CASES TESTED! 🎉          ║
╚════════════════════════════════════════════════════════════════╝

✅ GraphQL → Order → Inventory/Notification (HTTP + RabbitMQ)
✅ CQRS Service (Command/Query with Event Sourcing)
✅ Orchestrator Service (HTTP + RabbitMQ Distributed Tracing)
✅ Trace ID propagation across all protocols
✅ Log correlation across all services

Tests passed: 6/6
```

## Trace IDs Captured

The script now captures and displays:

1. **GraphQL Flow Trace ID**: Shows trace propagation through HTTP and RabbitMQ
2. **Orchestrator Workflow Trace ID**: Shows complete HTTP + RabbitMQ distributed tracing

Both trace IDs can be searched in Grafana Tempo to visualize the complete flow.

## Usage

```bash
# Start all services first
./run_all.sh

# Wait 60-90 seconds for services to start

# Run the complete architecture test
./test_tracing_complete.sh
```

## Comparison with Other Test Scripts

| Script | Purpose | Services Tested | Orchestrator | CQRS Full | Trace Verification |
|--------|---------|----------------|--------------|-----------|-------------------|
| `quick_test.sh` | Quick health check | Basic | ❌ | ❌ | ❌ |
| `test_system.sh` | System integration | Core 4 | ❌ | Partial | Basic |
| `test_distributed_tracing.sh` | Orchestrator only | Orchestrator + CQRS | ✅ | Partial | Medium |
| `test_complete_system.sh` | Full system | All 6 | ✅ | ✅ | Medium |
| **`test_tracing_complete.sh`** | **Complete architecture** | **All 6** | **✅** | **✅** | **Comprehensive** |

## Key Improvements

1. **Complete Coverage**: Tests all 6 services and all architecture patterns
2. **Orchestrator Integration**: Full testing of HTTP + RabbitMQ distributed tracing
3. **Enhanced CQRS Testing**: Tests Command, Query, and Event Sourcing patterns
4. **Detailed Trace Verification**: Checks trace propagation across all protocols
5. **Better Reporting**: Clear pass/fail status for each component
6. **Helpful Output**: Provides trace IDs and links to view in Grafana

## Next Steps

After running the test:

1. **View traces in Grafana**: http://localhost:3000
   - Go to Explore → Tempo
   - Search for the displayed trace IDs

2. **Query logs in Loki**:
   - Use the provided Loki query with trace IDs
   - See correlated logs across all services

3. **Explore the architecture**:
   - See how a single trace ID flows through multiple services
   - Understand HTTP and RabbitMQ trace propagation
   - Learn about CQRS and Event Sourcing patterns

## Documentation

- **Quick Start**: `docs/tracing-demo-v2/QUICK_START_HTTP_RABBITMQ.md`
- **Complete Guide**: `docs/tracing-demo-v2/DISTRIBUTED_TRACING_GUIDE.md`
- **Testing Guide**: `docs/tracing-demo-v2/TESTING_GUIDE.md`
- **Architecture**: `docs/tracing-demo-v2/ARCHITECTURE_WITH_COLLECTOR.md`

---

**Updated**: January 19, 2026
**Script**: `test_tracing_complete.sh`
**Status**: ✅ Ready to use - Tests all architecture use cases!
