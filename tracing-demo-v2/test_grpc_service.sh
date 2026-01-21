#!/bin/bash

# Test script for gRPC Service
# This script demonstrates how to call the gRPC service and verify tracing

set -e

echo "=========================================="
echo "Testing gRPC Service with Tracing"
echo "=========================================="
echo ""

# Check if grpcurl is installed
if ! command -v grpcurl &> /dev/null; then
    echo "❌ grpcurl is not installed"
    echo "Install it with: brew install grpcurl (macOS) or download from https://github.com/fullstorydev/grpcurl"
    exit 1
fi

GRPC_HOST="localhost:9090"
SERVICE_NAME="com.example.tracing.grpc.ProductService"

echo "1. Creating a product..."
echo "----------------------------------------"
PRODUCT_RESPONSE=$(grpcurl -plaintext -d '{
  "name": "Test Product",
  "description": "A test product for tracing demo",
  "price": 29.99,
  "stock": 100
}' "$GRPC_HOST" "$SERVICE_NAME/CreateProduct")

echo "$PRODUCT_RESPONSE" | jq '.'
PRODUCT_ID=$(echo "$PRODUCT_RESPONSE" | jq -r '.productId')
echo ""
echo "✅ Product created with ID: $PRODUCT_ID"
echo ""

# Wait a bit for trace to be exported
sleep 1

echo "2. Getting the product..."
echo "----------------------------------------"
grpcurl -plaintext -d "{
  \"product_id\": \"$PRODUCT_ID\"
}" "$GRPC_HOST" "$SERVICE_NAME/GetProduct" | jq '.'
echo ""

sleep 1

echo "3. Updating product price..."
echo "----------------------------------------"
grpcurl -plaintext -d "{
  \"product_id\": \"$PRODUCT_ID\",
  \"new_price\": 39.99
}" "$GRPC_HOST" "$SERVICE_NAME/UpdateProductPrice" | jq '.'
echo ""

sleep 1

echo "4. Listing products..."
echo "----------------------------------------"
grpcurl -plaintext -d '{
  "page": 0,
  "page_size": 10
}' "$GRPC_HOST" "$SERVICE_NAME/ListProducts" | jq '.'
echo ""

echo "=========================================="
echo "✅ gRPC Service Test Complete"
echo "=========================================="
echo ""
echo "To view traces:"
echo "1. Open Grafana: http://localhost:3000"
echo "2. Go to Explore → Select Tempo"
echo "3. Search for service: grpc-service"
echo "4. View the complete trace showing all gRPC operations"
echo ""
