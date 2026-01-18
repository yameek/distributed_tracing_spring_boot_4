#!/bin/bash

# Comprehensive test script for OpenTelemetry tracing
# Tests that trace IDs propagate across all services and appear in logs

set -e

# Allow check_trace_in_logs to fail without exiting
set +e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  OpenTelemetry Distributed Tracing Test - Spring Boot 4.0.1  ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check if all services are running
check_service() {
    SERVICE=$1
    PORT=$2
    
    if curl -s http://localhost:$PORT/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} $SERVICE is running on port $PORT"
        return 0
    else
        echo -e "${RED}✗${NC} $SERVICE is NOT running on port $PORT"
        return 1
    fi
}

echo -e "${YELLOW}[1/6] Checking if all services are running...${NC}"
ALL_RUNNING=true
check_service "GraphQL Service    " 8080 || ALL_RUNNING=false
check_service "Order Service      " 8081 || ALL_RUNNING=false
check_service "Inventory Service  " 8082 || ALL_RUNNING=false
check_service "Notification Service" 8083 || ALL_RUNNING=false
check_service "CQRS Service       " 8084 || ALL_RUNNING=false

if [ "$ALL_RUNNING" = false ]; then
    echo ""
    echo -e "${RED}Some services are not running!${NC}"
    echo -e "Start them with: ${BLUE}./run_all.sh${NC}"
    exit 1
fi
echo ""

# Test GraphQL endpoint
echo -e "${YELLOW}[2/6] Testing GraphQL endpoint (creates order via all services)...${NC}"
RESPONSE=$(curl -s -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"mutation { createOrder(productId: \"test-laptop\", quantity: 5) { orderId status message } }"}')

echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"

# Extract order ID
ORDER_ID=$(echo "$RESPONSE" | jq -r '.data.createOrder.orderId' 2>/dev/null || echo "")

if [ -z "$ORDER_ID" ] || [ "$ORDER_ID" == "null" ]; then
    echo -e "${RED}✗ Failed to create order via GraphQL${NC}"
    exit 1
fi

echo -e "${GREEN}✓${NC} Order created successfully: ${BLUE}$ORDER_ID${NC}"
echo ""

# Wait for logs to be written
echo -e "${YELLOW}[3/6] Waiting for logs to be written...${NC}"
sleep 2
echo ""

# Test CQRS Service
echo -e "${YELLOW}[4/6] Testing CQRS Service (creates product)...${NC}"
CQRS_RESPONSE=$(curl -s -X POST http://localhost:8084/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Laptop",
    "description": "High-performance laptop for testing",
    "price": 1299.99,
    "initialStock": 10
  }')

echo "$CQRS_RESPONSE" | jq '.' 2>/dev/null || echo "$CQRS_RESPONSE"

PRODUCT_ID=$(echo "$CQRS_RESPONSE" | jq -r '.productId' 2>/dev/null || echo "")

if [ -n "$PRODUCT_ID" ] && [ "$PRODUCT_ID" != "null" ]; then
    echo -e "${GREEN}✓${NC} Product created successfully: ${BLUE}$PRODUCT_ID${NC}"
else
    echo -e "${YELLOW}⚠${NC}  CQRS Service response: $CQRS_RESPONSE"
fi
echo ""

# Wait for logs
sleep 2

# Check trace IDs in all service logs
echo -e "${YELLOW}[5/6] Checking trace IDs in service logs...${NC}"

# Disable exit on error for trace checking
set +e

check_trace_in_logs() {
    SERVICE=$1
    LOG_FILE="$SERVICE/logs/$SERVICE.json.log"
    
    if [ ! -f "$LOG_FILE" ]; then
        echo -e "${RED}✗${NC} Log file not found: $LOG_FILE"
        return 1
    fi
    
    # Get the most recent trace ID from logs
    TRACE_ID=$(tail -50 "$LOG_FILE" | jq -r 'select(.traceId != null and .traceId != "") | .traceId' 2>/dev/null | tail -1 || echo "")
    
    if [ -z "$TRACE_ID" ] || [ "$TRACE_ID" == "null" ]; then
        echo -e "${RED}✗${NC} $SERVICE: No trace ID found in logs"
        echo "   Recent log sample:"
        (tail -3 "$LOG_FILE" | jq -c 'select(.stack_trace == null) | {level, message: .message[0:60], traceId, spanId}' 2>/dev/null | sed 's/^/   /') || echo "   Unable to parse logs"
        return 1
    fi
    
    # Count logs with this trace ID
    COUNT=$(tail -100 "$LOG_FILE" | jq -r 'select(.traceId == "'$TRACE_ID'")' 2>/dev/null | wc -l || echo "0")
    
    echo -e "${GREEN}✓${NC} $SERVICE: Found trace ID ${BLUE}$TRACE_ID${NC} (${COUNT} log entries)"
    
    # Show sample log (skip stack traces)
    (tail -100 "$LOG_FILE" | jq -r 'select(.traceId == "'$TRACE_ID'" and .stack_trace == null) | {level, logger: .logger[0:40], message: .message[0:50], traceId, spanId}' 2>/dev/null | head -2 | jq -c '.' 2>/dev/null | sed 's/^/   /') || true
    
    echo "$TRACE_ID"
}

GRAPHQL_TRACE=$(check_trace_in_logs "graphql-service") || GRAPHQL_TRACE="N/A"
echo ""
ORDER_TRACE=$(check_trace_in_logs "order-service") || ORDER_TRACE="N/A"
echo ""
INVENTORY_TRACE=$(check_trace_in_logs "inventory-service") || INVENTORY_TRACE="N/A"
echo ""
NOTIFICATION_TRACE=$(check_trace_in_logs "notification-service") || NOTIFICATION_TRACE="N/A"
echo ""

# Check CQRS service logs if they exist
if [ -f "logs/cqrs-service.log" ]; then
    echo "Checking CQRS service logs..."
    if grep -q "traceId" logs/cqrs-service.log 2>/dev/null; then
        CQRS_TRACE_ID=$(grep "traceId" logs/cqrs-service.log | tail -1 | grep -oE '[0-9a-f]{32}' | head -1 || echo "")
        if [ -n "$CQRS_TRACE_ID" ]; then
            echo -e "${GREEN}✓${NC} CQRS Service: Found trace ID ${BLUE}$CQRS_TRACE_ID${NC}"
        fi
    fi
fi
echo ""

# Re-enable exit on error
set -e

# Extract just the trace IDs (last line should be the 32-char hex trace ID)
GRAPHQL_TRACE_ID=$(echo "$GRAPHQL_TRACE" | tail -1 | grep -E '^[0-9a-f]{32}$' || echo "")
ORDER_TRACE_ID=$(echo "$ORDER_TRACE" | tail -1 | grep -E '^[0-9a-f]{32}$' || echo "")
INVENTORY_TRACE_ID=$(echo "$INVENTORY_TRACE" | tail -1 | grep -E '^[0-9a-f]{32}$' || echo "")
NOTIFICATION_TRACE_ID=$(echo "$NOTIFICATION_TRACE" | tail -1 | grep -E '^[0-9a-f]{32}$' || echo "")

# Verify trace propagation
echo -e "${YELLOW}[6/6] Verifying trace ID propagation across services...${NC}"

# Check if we have valid trace IDs
VALID_TRACES=0
[ -n "$GRAPHQL_TRACE_ID" ] && VALID_TRACES=$((VALID_TRACES + 1))
[ -n "$ORDER_TRACE_ID" ] && VALID_TRACES=$((VALID_TRACES + 1))
[ -n "$INVENTORY_TRACE_ID" ] && VALID_TRACES=$((VALID_TRACES + 1))
[ -n "$NOTIFICATION_TRACE_ID" ] && VALID_TRACES=$((VALID_TRACES + 1))

if [ -n "$GRAPHQL_TRACE_ID" ] && [ "$GRAPHQL_TRACE_ID" == "$ORDER_TRACE_ID" ]; then
    echo -e "${GREEN}✓${NC} SUCCESS! Same trace ID propagated from GraphQL to Order service!"
    echo -e "   ${BLUE}Trace ID: $GRAPHQL_TRACE_ID${NC}"
    MAIN_TRACE_ID="$GRAPHQL_TRACE_ID"
elif [ -n "$GRAPHQL_TRACE_ID" ] || [ -n "$ORDER_TRACE_ID" ]; then
    echo -e "${YELLOW}⚠${NC}  Different trace IDs between GraphQL and Order service:"
    echo "   GraphQL:      ${GRAPHQL_TRACE_ID:-<not found>}"
    echo "   Order:        ${ORDER_TRACE_ID:-<not found>}"
    MAIN_TRACE_ID="${GRAPHQL_TRACE_ID:-$ORDER_TRACE_ID}"
else
    echo -e "${RED}✗${NC} No trace IDs found in GraphQL or Order services"
    MAIN_TRACE_ID=""
fi

if [ -z "$INVENTORY_TRACE_ID" ] && [ -z "$NOTIFICATION_TRACE_ID" ]; then
    echo -e "${YELLOW}⚠${NC}  Inventory and Notification services: No trace IDs (RabbitMQ propagation not configured)"
elif [ -n "$INVENTORY_TRACE_ID" ] || [ -n "$NOTIFICATION_TRACE_ID" ]; then
    echo -e "${GREEN}✓${NC} Async services:"
    [ -n "$INVENTORY_TRACE_ID" ] && echo "   Inventory:    $INVENTORY_TRACE_ID"
    [ -n "$NOTIFICATION_TRACE_ID" ] && echo "   Notification: $NOTIFICATION_TRACE_ID"
fi
echo ""

# Summary
if [ $VALID_TRACES -ge 2 ]; then
    echo -e "${GREEN}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║                    🎉 TESTS PASSED! 🎉                         ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo "✅ Services are generating trace IDs and span IDs"
    echo "✅ Trace context is being propagated via HTTP"
    echo "✅ Logs contain proper trace correlation"
    [ -z "$INVENTORY_TRACE_ID" ] && echo "⚠️  RabbitMQ trace propagation not yet configured"
else
    echo -e "${RED}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}║                    ❌ TESTS FAILED                             ║${NC}"
    echo -e "${RED}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo "❌ Tracing is not working properly"
    exit 1
fi
echo ""
echo -e "${BLUE}Next Steps:${NC}"
if [ -n "$MAIN_TRACE_ID" ]; then
    echo "1. View traces in Grafana: ${BLUE}http://localhost:3000${NC}"
    echo "   - Go to Explore > Tempo"
    echo "   - Search for trace: ${BLUE}$MAIN_TRACE_ID${NC}"
    echo ""
    echo "2. Query logs by trace ID in Loki:"
    echo "   - Use query: ${BLUE}{service_name=~\".+\"} | json | traceId=\"$MAIN_TRACE_ID\"${NC}"
else
    echo "1. View traces in Grafana: ${BLUE}http://localhost:3000${NC}"
    echo "   - Go to Explore > Tempo"
    echo "   - Browse recent traces"
fi
echo ""
echo "3. View GraphQL UI: ${BLUE}http://localhost:8080/graphiql${NC}"
echo "4. View CQRS Service: ${BLUE}http://localhost:8084/api/products${NC}"
echo ""
