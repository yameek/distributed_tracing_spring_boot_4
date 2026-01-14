#!/bin/bash

echo "========================================"
echo "🧪 Tracing Verification - Spring Boot 4"
echo "========================================"
echo ""

# Create an order
echo "1. Creating order..."
curl -s -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{"product":"laptop","quantity":5}' | jq '.'

echo ""
echo "2. Waiting for logs..."
sleep 2

echo ""
echo "3. Recent logs with trace IDs:"
echo "========================================"
tail -5 order-service/logs/order-service.json.log | \
  jq -c 'select(.traceId) | {level, logger: .logger[30:55], message: .message[0:50], traceId, spanId}'

echo ""
echo "✅ If you see traceId and spanId above, tracing is WORKING!"
echo ""
echo "Next: Open Grafana at http://localhost:3000"
echo "       - Go to Explore > Tempo"
echo "       - You can search for traces by service name"
echo ""
