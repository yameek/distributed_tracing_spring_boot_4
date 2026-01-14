#!/bin/bash

# Test script to verify OpenTelemetry tracing is working
# This tests that trace IDs and span IDs appear in logs

set -e

echo "🧪 Testing OpenTelemetry Tracing with Spring Boot 4..."
echo ""

# Check if order-service is running
if ! curl -s http://localhost:8081/actuator/health > /dev/null 2>&1; then
    echo "❌ Order service is not running on port 8081"
    echo "   Start it with: cd order-service && mvn spring-boot:run"
    exit 1
fi

echo "✓ Order service is running"
echo ""

# Make a test request
echo "📤 Creating a test order..."
RESPONSE=$(curl -s -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{"product":"test-laptop","quantity":3}')

ORDER_ID=$(echo "$RESPONSE" | jq -r '.orderId')
echo "✓ Order created: $ORDER_ID"
echo ""

# Wait a moment for log to be written
sleep 1

# Check the log file for trace IDs
echo "🔍 Checking logs for trace context..."
LOG_FILE="order-service/logs/order-service.json.log"

if [ ! -f "$LOG_FILE" ]; then
    echo "❌ Log file not found: $LOG_FILE"
    exit 1
fi

# Extract the last few log entries related to our order
TRACE_LOG=$(tail -20 "$LOG_FILE" | jq -r 'select(.message | contains("OrderController") or contains("OrderPublisher")) | {level, message: .message[0:70], traceId, spanId}' | tail -3)

if [ -z "$TRACE_LOG" ]; then
    echo "❌ No log entries found"
    exit 1
fi

echo "$TRACE_LOG"
echo ""

# Check if trace IDs are present
TRACE_ID=$(echo "$TRACE_LOG" | jq -r '.traceId' | head -1)

if [ "$TRACE_ID" == "null" ] || [ -z "$TRACE_ID" ]; then
    echo "❌ FAILED: No trace ID found in logs!"
    echo ""
    echo "Recent logs:"
    tail -5 "$LOG_FILE" | jq -c '{level, message: .message[0:80], traceId, spanId}'
    exit 1
fi

echo "✅ SUCCESS! Trace ID found: $TRACE_ID"
echo ""

# Show trace context
echo "📊 Trace Context:"
echo "$TRACE_LOG" | jq -r '"   Level: " + .level + " | TraceID: " + (.traceId // "null") + " | SpanID: " + (.spanId // "null")'
echo ""

echo "🎉 OpenTelemetry tracing is working correctly!"
echo ""
echo "Next steps:"
echo "  1. Start all services (graphql, inventory, notification)"
echo "  2. Create an order via GraphQL: http://localhost:8080/graphiql"
echo "  3. Check Grafana/Tempo: http://localhost:3000"
echo "  4. Query logs by trace ID: {traceId=\"$TRACE_ID\"}"
