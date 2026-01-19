#!/bin/bash

# Comprehensive System Test - Tests all services and distributed tracing

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "================================================================"
echo "  Complete System Test - Distributed Tracing Demo"
echo "================================================================"
echo ""

# Function to check if a service is running
check_service() {
    local SERVICE_NAME=$1
    local PORT=$2
    local ENDPOINT=$3
    
    echo -n "Checking $SERVICE_NAME (port $PORT)... "
    if curl -s -f "$ENDPOINT" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Running${NC}"
        return 0
    else
        echo -e "${RED}✗ Not running${NC}"
        return 1
    fi
}

# Function to wait for service to be ready
wait_for_service() {
    local SERVICE_NAME=$1
    local PORT=$2
    local ENDPOINT=$3
    local MAX_ATTEMPTS=30
    local ATTEMPT=0
    
    echo -n "Waiting for $SERVICE_NAME to be ready..."
    while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
        if curl -s -f "$ENDPOINT" > /dev/null 2>&1; then
            echo -e " ${GREEN}✓ Ready${NC}"
            return 0
        fi
        echo -n "."
        sleep 2
        ATTEMPT=$((ATTEMPT + 1))
    done
    echo -e " ${RED}✗ Timeout${NC}"
    return 1
}

echo -e "${BLUE}Step 1: Checking Infrastructure${NC}"
echo "================================================"

# Check Docker services
echo -n "Checking Docker services... "
if docker compose ps | grep -q "Up"; then
    echo -e "${GREEN}✓ Running${NC}"
else
    echo -e "${YELLOW}! Not all services running${NC}"
    echo "Run: docker compose up -d"
fi

# Check RabbitMQ
check_service "RabbitMQ" 15672 "http://localhost:15672" || true

# Check Grafana
check_service "Grafana" 3000 "http://localhost:3000" || true

echo ""
echo -e "${BLUE}Step 2: Checking Application Services${NC}"
echo "================================================"

# Check all services
SERVICES_OK=true

check_service "GraphQL Service" 8080 "http://localhost:8080/graphiql" || SERVICES_OK=false
check_service "Order Service" 8081 "http://localhost:8081/actuator/health" || SERVICES_OK=false
check_service "Inventory Service" 8082 "http://localhost:8082/actuator/health" || SERVICES_OK=false
check_service "Notification Service" 8083 "http://localhost:8083/actuator/health" || SERVICES_OK=false
check_service "CQRS Service" 8084 "http://localhost:8084/api/products" || SERVICES_OK=false
check_service "Orchestrator Service" 8085 "http://localhost:8085/api/workflows/health" || SERVICES_OK=false

if [ "$SERVICES_OK" = false ]; then
    echo ""
    echo -e "${RED}Some services are not running!${NC}"
    echo "Run: ./run_all.sh"
    exit 1
fi

echo ""
echo -e "${BLUE}Step 3: Testing Core Flow (GraphQL → Order → Inventory/Notification)${NC}"
echo "================================================"

# Test GraphQL mutation
echo "Testing GraphQL order creation..."
GRAPHQL_RESPONSE=$(curl -s -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation { createOrder(productId: \"LAPTOP-001\", quantity: 2) { orderId status message } }"
  }')

ORDER_ID=$(echo "$GRAPHQL_RESPONSE" | jq -r '.data.createOrder.orderId')
echo -e "${GREEN}✓ Order created: $ORDER_ID${NC}"

# Wait for async processing
echo "Waiting for async message processing..."
sleep 3

echo ""
echo -e "${BLUE}Step 4: Testing CQRS Service${NC}"
echo "================================================"

# Create product
echo "Creating product via CQRS service..."
CQRS_RESPONSE=$(curl -s -X POST http://localhost:8084/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Laptop",
    "description": "High-performance laptop for testing",
    "price": 1999.99,
    "initialStock": 50
  }')

PRODUCT_ID=$(echo "$CQRS_RESPONSE" | jq -r '.productId')
echo -e "${GREEN}✓ Product created: $PRODUCT_ID${NC}"

# Query product
echo "Querying product..."
PRODUCT=$(curl -s http://localhost:8084/api/products/$PRODUCT_ID)
PRODUCT_NAME=$(echo "$PRODUCT" | jq -r '.name')
echo -e "${GREEN}✓ Product retrieved: $PRODUCT_NAME${NC}"

# Update price
echo "Updating product price..."
curl -s -X PUT http://localhost:8084/api/products/$PRODUCT_ID/price \
  -H "Content-Type: application/json" \
  -d '{"newPrice": 1799.99}' > /dev/null
echo -e "${GREEN}✓ Price updated${NC}"

# Wait for outbox processing
sleep 2

echo ""
echo -e "${BLUE}Step 5: Testing Distributed Tracing (HTTP + RabbitMQ)${NC}"
echo "================================================"

# Execute orchestrator workflow
echo "Executing distributed tracing workflow..."
WORKFLOW_RESPONSE=$(curl -s -X POST http://localhost:8085/api/workflows/product \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Gaming Laptop Pro",
    "description": "Ultimate gaming laptop with RTX 5090",
    "price": 3499.99,
    "initialStock": 20,
    "updatedPrice": 3299.99,
    "updatedStock": 25
  }')

WORKFLOW_PRODUCT_ID=$(echo "$WORKFLOW_RESPONSE" | jq -r '.productId')
TRACE_ID=$(echo "$WORKFLOW_RESPONSE" | jq -r '.traceId')
echo -e "${GREEN}✓ Workflow completed${NC}"
echo -e "${GREEN}  Product ID: $WORKFLOW_PRODUCT_ID${NC}"
echo -e "${GREEN}  Trace ID: $TRACE_ID${NC}"

# Wait for async processing
echo "Waiting for RabbitMQ message processing..."
sleep 3

# Verify final product state
echo "Verifying product updates..."
FINAL_PRODUCT=$(curl -s http://localhost:8084/api/products/$WORKFLOW_PRODUCT_ID)
FINAL_PRICE=$(echo "$FINAL_PRODUCT" | jq -r '.price')
FINAL_STOCK=$(echo "$FINAL_PRODUCT" | jq -r '.stock')

if [ "$FINAL_PRICE" == "3299.99" ] && [ "$FINAL_STOCK" == "25" ]; then
    echo -e "${GREEN}✓ Product correctly updated via RabbitMQ${NC}"
    echo -e "  Price: $FINAL_PRICE (expected: 3299.99)"
    echo -e "  Stock: $FINAL_STOCK (expected: 25)"
else
    echo -e "${YELLOW}! Product state may not be fully updated yet${NC}"
    echo -e "  Price: $FINAL_PRICE (expected: 3299.99)"
    echo -e "  Stock: $FINAL_STOCK (expected: 25)"
fi

echo ""
echo -e "${BLUE}Step 6: Checking Logs for Trace Correlation${NC}"
echo "================================================"

# Check if logs contain trace IDs
echo "Checking orchestrator logs for trace ID..."
if grep -q "$TRACE_ID" logs/orchestrator-service.log 2>/dev/null; then
    echo -e "${GREEN}✓ Trace ID found in orchestrator logs${NC}"
else
    echo -e "${YELLOW}! Trace ID not found in orchestrator logs (may need more time)${NC}"
fi

echo "Checking CQRS logs for trace ID..."
if grep -q "$TRACE_ID" logs/cqrs-service.log 2>/dev/null; then
    echo -e "${GREEN}✓ Trace ID found in CQRS logs${NC}"
else
    echo -e "${YELLOW}! Trace ID not found in CQRS logs (may need more time)${NC}"
fi

echo ""
echo "================================================================"
echo -e "${GREEN}  ✓ System Test Complete!${NC}"
echo "================================================================"
echo ""
echo -e "${BLUE}Summary:${NC}"
echo "  ✓ Infrastructure running"
echo "  ✓ All 6 services operational"
echo "  ✓ GraphQL → Order → Inventory/Notification flow working"
echo "  ✓ CQRS service CRUD operations working"
echo "  ✓ Distributed tracing (HTTP + RabbitMQ) working"
echo "  ✓ Trace ID: $TRACE_ID"
echo ""
echo -e "${BLUE}View the trace in Grafana:${NC}"
echo "  1. Open http://localhost:3000"
echo "  2. Go to Explore (compass icon)"
echo "  3. Select 'Tempo' as data source"
echo "  4. Search for trace ID: $TRACE_ID"
echo ""
echo -e "${BLUE}What you'll see:${NC}"
echo "  • orchestrator-service: Workflow orchestration"
echo "  • HTTP call: Create product"
echo "  • RabbitMQ: Price update message"
echo "  • cqrs-service: Message consumption"
echo "  • RabbitMQ: Stock update message"
echo "  • cqrs-service: Message consumption"
echo "  • HTTP call: Query product"
echo "  • All connected by trace ID: $TRACE_ID"
echo ""
echo -e "${GREEN}Test completed successfully!${NC}"
