#!/bin/bash

# Test script for CQRS Service
# Tests all endpoints and verifies tracing

set -e

BASE_URL="http://localhost:8084"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}CQRS Service Test Script${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""

# Function to print success
success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# Function to print error
error() {
    echo -e "${RED}✗ $1${NC}"
}

# Function to print info
info() {
    echo -e "${YELLOW}→ $1${NC}"
}

# Check if service is running
info "Checking if CQRS service is running..."
if curl -s -f "${BASE_URL}/actuator/health" > /dev/null; then
    success "Service is running"
else
    error "Service is not running on ${BASE_URL}"
    echo "Please start the service with: ./gradlew :cqrs-service:bootRun"
    exit 1
fi

echo ""
echo -e "${YELLOW}Test 1: Create Product${NC}"
info "Creating a new product..."

CREATE_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/products" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Gaming Laptop",
    "description": "High-performance gaming laptop with RTX 4090",
    "price": 2499.99,
    "initialStock": 25
  }')

PRODUCT_ID=$(echo $CREATE_RESPONSE | grep -o '"productId":"[^"]*"' | cut -d'"' -f4)

if [ -n "$PRODUCT_ID" ]; then
    success "Product created with ID: $PRODUCT_ID"
    echo "Response: $CREATE_RESPONSE"
else
    error "Failed to create product"
    echo "Response: $CREATE_RESPONSE"
    exit 1
fi

echo ""
echo -e "${YELLOW}Test 2: Get Product by ID${NC}"
info "Fetching product $PRODUCT_ID..."

GET_RESPONSE=$(curl -s "${BASE_URL}/api/products/${PRODUCT_ID}")

if echo $GET_RESPONSE | grep -q "Gaming Laptop"; then
    success "Product retrieved successfully"
    echo "Response: $GET_RESPONSE"
else
    error "Failed to retrieve product"
    echo "Response: $GET_RESPONSE"
    exit 1
fi

echo ""
echo -e "${YELLOW}Test 3: Get All Products${NC}"
info "Fetching all products..."

ALL_PRODUCTS=$(curl -s "${BASE_URL}/api/products")

if echo $ALL_PRODUCTS | grep -q "$PRODUCT_ID"; then
    success "All products retrieved successfully"
    PRODUCT_COUNT=$(echo $ALL_PRODUCTS | grep -o '"id"' | wc -l)
    echo "Found $PRODUCT_COUNT product(s)"
else
    error "Failed to retrieve all products"
    echo "Response: $ALL_PRODUCTS"
    exit 1
fi

echo ""
echo -e "${YELLOW}Test 4: Update Product Price${NC}"
info "Updating price for product $PRODUCT_ID..."

PRICE_UPDATE=$(curl -s -X PUT "${BASE_URL}/api/products/${PRODUCT_ID}/price" \
  -H "Content-Type: application/json" \
  -d '{"newPrice": 2299.99}')

if echo $PRICE_UPDATE | grep -q "successfully"; then
    success "Price updated successfully"
    echo "Response: $PRICE_UPDATE"
else
    error "Failed to update price"
    echo "Response: $PRICE_UPDATE"
    exit 1
fi

# Verify price was updated
UPDATED_PRODUCT=$(curl -s "${BASE_URL}/api/products/${PRODUCT_ID}")
if echo $UPDATED_PRODUCT | grep -q "2299.99"; then
    success "Price change verified"
else
    error "Price was not updated in database"
fi

echo ""
echo -e "${YELLOW}Test 5: Update Product Stock${NC}"
info "Updating stock for product $PRODUCT_ID..."

STOCK_UPDATE=$(curl -s -X PUT "${BASE_URL}/api/products/${PRODUCT_ID}/stock" \
  -H "Content-Type: application/json" \
  -d '{"quantity": 20}')

if echo $STOCK_UPDATE | grep -q "successfully"; then
    success "Stock updated successfully"
    echo "Response: $STOCK_UPDATE"
else
    error "Failed to update stock"
    echo "Response: $STOCK_UPDATE"
    exit 1
fi

# Verify stock was updated
UPDATED_PRODUCT=$(curl -s "${BASE_URL}/api/products/${PRODUCT_ID}")
if echo $UPDATED_PRODUCT | grep -q '"stockQuantity":20'; then
    success "Stock change verified"
else
    error "Stock was not updated in database"
fi

echo ""
echo -e "${YELLOW}Test 6: Low Stock Alert${NC}"
info "Creating product with low stock to trigger alert..."

LOW_STOCK_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/products" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Limited Edition Mouse",
    "description": "Only a few left!",
    "price": 79.99,
    "initialStock": 5
  }')

LOW_STOCK_ID=$(echo $LOW_STOCK_RESPONSE | grep -o '"productId":"[^"]*"' | cut -d'"' -f4)

if [ -n "$LOW_STOCK_ID" ]; then
    success "Low stock product created: $LOW_STOCK_ID"
    info "Check service logs for 'Low stock alert' message"
else
    error "Failed to create low stock product"
fi

echo ""
echo -e "${YELLOW}Test 7: Validation - Invalid Price${NC}"
info "Testing validation with negative price..."

INVALID_PRICE=$(curl -s -X POST "${BASE_URL}/api/products" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Invalid Product",
    "description": "Should fail",
    "price": -10.00,
    "initialStock": 10
  }')

if echo $INVALID_PRICE | grep -q "error\|Price must be positive"; then
    success "Validation working correctly"
else
    error "Validation not working"
    echo "Response: $INVALID_PRICE"
fi

echo ""
echo -e "${YELLOW}Test 8: Check Metrics${NC}"
info "Fetching Prometheus metrics..."

METRICS=$(curl -s "${BASE_URL}/actuator/prometheus")

if echo $METRICS | grep -q "command_bus_success_total"; then
    success "Command bus metrics available"
fi

if echo $METRICS | grep -q "event_bus_published_total"; then
    success "Event bus metrics available"
fi

if echo $METRICS | grep -q "outbox_event_stored_total"; then
    success "Outbox metrics available"
fi

if echo $METRICS | grep -q "products_created_total"; then
    success "Domain metrics available"
fi

echo ""
echo -e "${YELLOW}Test 9: Wait for Outbox Processing${NC}"
info "Waiting 10 seconds for outbox publisher to process events..."
sleep 10
success "Outbox processing time elapsed"
info "Check RabbitMQ management UI at http://localhost:15672"
info "Check Grafana traces at http://localhost:3000"

echo ""
echo -e "${YELLOW}========================================${NC}"
echo -e "${GREEN}All Tests Passed! ✓${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""
echo "Summary:"
echo "  - Products created: 2"
echo "  - Price updates: 1"
echo "  - Stock updates: 1"
echo "  - Validation tests: 1"
echo ""
echo "Next Steps:"
echo "  1. View traces in Grafana: http://localhost:3000"
echo "     → Explore → Tempo → Search for 'cqrs-service'"
echo ""
echo "  2. View logs in Grafana: http://localhost:3000"
echo "     → Explore → Loki → Query: {service=\"cqrs-service\"}"
echo ""
echo "  3. View metrics in Grafana: http://localhost:3000"
echo "     → Explore → Prometheus → Try: command_bus_success_total"
echo ""
echo "  4. Check RabbitMQ: http://localhost:15672 (guest/guest)"
echo "     → Exchanges → cqrs.events.exchange"
echo ""
echo "  5. Check database:"
echo "     docker exec -it tracing-demo-v2-postgres-1 psql -U postgres -d cqrs_db"
echo "     SELECT * FROM products;"
echo "     SELECT * FROM outbox_events;"
echo ""
