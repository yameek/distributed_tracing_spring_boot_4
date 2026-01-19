#!/bin/bash

# Quick Test - Tests basic functionality of all services

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "================================================"
echo "  Quick System Test"
echo "================================================"
echo ""

# Function to check if a service is running
check_service() {
    local SERVICE_NAME=$1
    local PORT=$2
    local ENDPOINT=$3
    
    echo -n "Checking $SERVICE_NAME... "
    if curl -s -f "$ENDPOINT" > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC}"
        return 0
    else
        echo -e "${RED}✗${NC}"
        return 1
    fi
}

echo -e "${BLUE}Checking Services:${NC}"
check_service "GraphQL" 8080 "http://localhost:8080/graphiql"
check_service "Order" 8081 "http://localhost:8081/actuator/health"
check_service "Inventory" 8082 "http://localhost:8082/actuator/health"
check_service "Notification" 8083 "http://localhost:8083/actuator/health"
check_service "CQRS" 8084 "http://localhost:8084/api/products"
check_service "Orchestrator" 8085 "http://localhost:8085/api/workflows/health"

echo ""
echo -e "${BLUE}Testing GraphQL Order Creation:${NC}"
RESPONSE=$(curl -s -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { createOrder(productId: \"LAPTOP-001\", quantity: 1) { orderId status } }"}')

ORDER_ID=$(echo "$RESPONSE" | jq -r '.data.createOrder.orderId')
if [ -n "$ORDER_ID" ] && [ "$ORDER_ID" != "null" ]; then
    echo -e "${GREEN}✓ Order created: $ORDER_ID${NC}"
else
    echo -e "${RED}✗ Failed to create order${NC}"
fi

echo ""
echo -e "${BLUE}Testing CQRS Product Creation:${NC}"
RESPONSE=$(curl -s -X POST http://localhost:8084/api/products \
  -H "Content-Type: application/json" \
  -d '{"name": "Quick Test Laptop", "description": "Test", "price": 999.99, "initialStock": 10}')

PRODUCT_ID=$(echo "$RESPONSE" | jq -r '.productId')
if [ -n "$PRODUCT_ID" ] && [ "$PRODUCT_ID" != "null" ]; then
    echo -e "${GREEN}✓ Product created: $PRODUCT_ID${NC}"
else
    echo -e "${RED}✗ Failed to create product${NC}"
fi

echo ""
echo -e "${GREEN}Quick test complete!${NC}"
