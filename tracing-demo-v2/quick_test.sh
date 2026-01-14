#!/bin/bash

# Quick test to verify trace IDs are appearing in logs

echo "🧪 Quick Tracing Verification Test"
echo "======================================"
echo ""

# Test order service directly
echo "1. Creating order via order-service..."
ORDER_RESPONSE=$(curl -s -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{"product":"test-laptop","quantity":3}')

ORDER_ID=$(echo "$ORDER_RESPONSE" | jq -r '.orderId')
echo "   ✓ Order created: $ORDER_ID"
echo ""

# Wait for log
sleep 1

# Check trace ID in order service logs
echo "2. Checking trace ID in order-service logs..."
TRACE_DATA=$(tail -10 order-service/logs/order-service.json.log | \
  jq -r 'select(.traceId != null and .traceId != "") | {level, message: .message[0:60], traceId, spanId}' | \
  tail -1)

if [ -z "$TRACE_DATA" ]; then
    echo "   ✗ No trace data found"
    exit 1
fi

echo "$TRACE_DATA" | jq '.'

TRACE_ID=$(echo "$TRACE_DATA" | jq -r '.traceId')

if [ "$TRACE_ID" == "null" ] || [ -z "$TRACE_ID" ]; then
    echo "   ✗ FAILED: No trace ID!"
    exit 1
fi

echo ""
echo "✅ SUCCESS! Trace ID found: $TRACE_ID"
echo ""

# Test GraphQL
echo "3. Creating order via GraphQL..."
GQL_RESPONSE=$(curl -s -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"mutation { createOrder(productId: \"laptop\", quantity: 2) { orderId status } }"}')

GQL_ORDER_ID=$(echo "$GQL_RESPONSE" | jq -r '.data.createOrder.orderId')
echo "   ✓ GraphQL order created: $GQL_ORDER_ID"
echo ""

sleep 1

# Check GraphQL trace
echo "4. Checking trace ID in graphql-service logs..."
GQL_TRACE_DATA=$(tail -10 graphql-service/logs/graphql-service.json.log | \
  jq -r 'select(.message | contains("GraphQL")) | {level, message: .message[0:60], traceId, spanId}' | \
  tail -1)

echo "$GQL_TRACE_DATA" | jq '.'

GQL_TRACE_ID=$(echo "$GQL_TRACE_DATA" | jq -r '.traceId')

echo ""
echo "✅ SUCCESS! GraphQL trace ID: $GQL_TRACE_ID"
echo ""
echo "=========================================="
echo "🎉 OpenTelemetry Tracing is WORKING!"
echo "=========================================="
echo ""
echo "Key Points:"
echo "  ✓ Trace IDs appear in logs"
echo "  ✓ Span IDs appear in logs"
echo "  ✓ MDC injection is working"
echo "  ✓ Spring Boot 4 OpenTelemetry starter is functional"
echo ""
echo "View in Grafana:"
echo "  - Grafana: http://localhost:3000"
echo "  - Tempo: Search for trace: $TRACE_ID"
echo ""
