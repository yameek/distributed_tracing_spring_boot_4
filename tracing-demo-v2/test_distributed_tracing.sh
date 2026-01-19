#!/bin/bash

# Test script for demonstrating distributed tracing across HTTP and RabbitMQ
# This script calls the orchestrator service which demonstrates how a single trace ID
# flows through multiple services and communication protocols

set -e

echo "=================================================="
echo "Distributed Tracing Test: HTTP + RabbitMQ"
echo "=================================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if orchestrator service is running
echo -e "${BLUE}Checking if orchestrator service is running...${NC}"
if ! curl -s http://localhost:8085/api/workflows/health > /dev/null; then
    echo -e "${YELLOW}Orchestrator service is not running on port 8085${NC}"
    echo "Please start it with: ./gradlew :orchestrator-service:bootRun"
    exit 1
fi
echo -e "${GREEN}✓ Orchestrator service is running${NC}"
echo ""

# Check if cqrs service is running
echo -e "${BLUE}Checking if CQRS service is running...${NC}"
if ! curl -s http://localhost:8084/api/products > /dev/null; then
    echo -e "${YELLOW}CQRS service is not running on port 8084${NC}"
    echo "Please start it with: ./gradlew :cqrs-service:bootRun"
    exit 1
fi
echo -e "${GREEN}✓ CQRS service is running${NC}"
echo ""

# Execute the workflow
echo -e "${BLUE}Executing product workflow...${NC}"
echo "This workflow will:"
echo "  1. Create a product via HTTP REST API"
echo "  2. Update price via RabbitMQ message"
echo "  3. Update stock via RabbitMQ message"
echo "  4. Query product via HTTP REST API"
echo ""

RESPONSE=$(curl -s -X POST http://localhost:8085/api/workflows/product \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Gaming Laptop",
    "description": "High-performance gaming laptop with RTX 4090",
    "price": 2499.99,
    "initialStock": 10,
    "updatedPrice": 2299.99,
    "updatedStock": 15
  }')

echo -e "${GREEN}Response:${NC}"
echo "$RESPONSE" | jq '.'
echo ""

# Extract trace ID
TRACE_ID=$(echo "$RESPONSE" | jq -r '.traceId')
PRODUCT_ID=$(echo "$RESPONSE" | jq -r '.productId')

echo -e "${GREEN}=================================================="
echo "Workflow Completed Successfully!"
echo "==================================================${NC}"
echo ""
echo -e "${YELLOW}Trace ID:${NC} $TRACE_ID"
echo -e "${YELLOW}Product ID:${NC} $PRODUCT_ID"
echo ""
echo -e "${BLUE}How to observe the trace:${NC}"
echo ""
echo "1. Open Grafana: http://localhost:3000"
echo "2. Go to 'Explore' (compass icon on the left)"
echo "3. Select 'Tempo' as the data source"
echo "4. Search for trace ID: $TRACE_ID"
echo ""
echo -e "${GREEN}What you'll see in the trace:${NC}"
echo "  • orchestrator-service: Initial HTTP request"
echo "  • orchestrator-service: Workflow orchestration span"
echo "  • cqrs-service: HTTP product creation (via REST API)"
echo "  • cqrs-service: Command bus processing"
echo "  • RabbitMQ: Message publishing to outbox"
echo "  • orchestrator-service: RabbitMQ price update message"
echo "  • cqrs-service: RabbitMQ message consumption (price update)"
echo "  • orchestrator-service: RabbitMQ stock update message"
echo "  • cqrs-service: RabbitMQ message consumption (stock update)"
echo "  • cqrs-service: HTTP product query (via REST API)"
echo ""
echo -e "${BLUE}The trace shows how a single request lifecycle spans:${NC}"
echo "  ✓ Multiple services (orchestrator-service, cqrs-service)"
echo "  ✓ Multiple protocols (HTTP REST, RabbitMQ messaging)"
echo "  ✓ Multiple operations (create, update, query)"
echo "  ✓ All connected by a single trace ID!"
echo ""
echo -e "${YELLOW}Verify the product was updated:${NC}"
curl -s http://localhost:8084/api/products/$PRODUCT_ID | jq '.'
echo ""
echo -e "${GREEN}Test completed!${NC}"
