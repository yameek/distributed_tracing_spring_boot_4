#!/bin/bash

# Test script to verify OpenTelemetry Collector is working correctly

echo "🔍 Testing OpenTelemetry Collector Setup..."
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test 1: Check if collector is running
echo "1️⃣  Checking if OTel Collector is running..."
if docker compose ps otel-collector | grep -q "Up"; then
    echo -e "${GREEN}✓ Collector is running${NC}"
else
    echo -e "${RED}✗ Collector is not running${NC}"
    echo "   Run: docker compose up -d otel-collector"
    exit 1
fi
echo ""

# Test 2: Check collector health endpoint
echo "2️⃣  Checking collector health endpoint..."
if curl -s http://localhost:8888/metrics > /dev/null; then
    echo -e "${GREEN}✓ Collector health endpoint is accessible${NC}"
else
    echo -e "${RED}✗ Collector health endpoint is not accessible${NC}"
    exit 1
fi
echo ""

# Test 3: Check if collector is receiving spans
echo "3️⃣  Checking collector metrics before test..."
SPANS_BEFORE=$(curl -s http://localhost:8888/metrics | grep "otelcol_receiver_accepted_spans" | grep -v "#" | awk '{print $2}' | head -1)
if [ -z "$SPANS_BEFORE" ]; then
    SPANS_BEFORE=0
fi
echo "   Spans received so far: $SPANS_BEFORE"
echo ""

# Test 4: Send a test request through the system
echo "4️⃣  Sending test GraphQL request..."
RESPONSE=$(curl -s -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ orders { id status } }"}')

if echo "$RESPONSE" | grep -q "orders"; then
    echo -e "${GREEN}✓ Request successful${NC}"
else
    echo -e "${RED}✗ Request failed${NC}"
    echo "   Response: $RESPONSE"
    exit 1
fi
echo ""

# Test 5: Wait and check if collector received new spans
echo "5️⃣  Waiting 3 seconds for spans to be processed..."
sleep 3

SPANS_AFTER=$(curl -s http://localhost:8888/metrics | grep "otelcol_receiver_accepted_spans" | grep -v "#" | awk '{print $2}' | head -1)
if [ -z "$SPANS_AFTER" ]; then
    SPANS_AFTER=0
fi

SPANS_DIFF=$((SPANS_AFTER - SPANS_BEFORE))
echo "   Spans received after test: $SPANS_AFTER"
echo "   New spans: $SPANS_DIFF"

if [ "$SPANS_DIFF" -gt 0 ]; then
    echo -e "${GREEN}✓ Collector received $SPANS_DIFF new spans${NC}"
else
    echo -e "${YELLOW}⚠ No new spans detected (might be a timing issue)${NC}"
fi
echo ""

# Test 6: Check if collector is exporting to backend
echo "6️⃣  Checking if collector is exporting spans..."
EXPORTED_SPANS=$(curl -s http://localhost:8888/metrics | grep "otelcol_exporter_sent_spans" | grep -v "#" | awk '{print $2}' | head -1)
if [ -z "$EXPORTED_SPANS" ]; then
    EXPORTED_SPANS=0
fi

if [ "$EXPORTED_SPANS" -gt 0 ]; then
    echo -e "${GREEN}✓ Collector has exported $EXPORTED_SPANS spans to backend${NC}"
else
    echo -e "${YELLOW}⚠ No spans exported yet (check collector logs)${NC}"
fi
echo ""

# Test 7: Check collector logs for errors
echo "7️⃣  Checking collector logs for errors..."
ERROR_COUNT=$(docker compose logs --tail=50 otel-collector 2>&1 | grep -i "error" | wc -l)
if [ "$ERROR_COUNT" -eq 0 ]; then
    echo -e "${GREEN}✓ No errors in collector logs${NC}"
else
    echo -e "${YELLOW}⚠ Found $ERROR_COUNT error(s) in collector logs${NC}"
    echo "   Run: docker compose logs otel-collector | grep -i error"
fi
echo ""

# Test 8: Verify Tempo can be reached from collector
echo "8️⃣  Checking if Tempo is reachable from collector..."
if docker compose exec -T otel-collector sh -c "nc -zv tempo 4317 2>&1" | grep -q "succeeded"; then
    echo -e "${GREEN}✓ Collector can reach Tempo${NC}"
else
    echo -e "${YELLOW}⚠ Cannot verify Tempo connectivity${NC}"
fi
echo ""

# Summary
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 Summary:"
echo "   • Collector Status: Running ✓"
echo "   • Spans Received: $SPANS_AFTER total ($SPANS_DIFF in this test)"
echo "   • Spans Exported: $EXPORTED_SPANS total"
echo "   • Errors: $ERROR_COUNT"
echo ""
echo "🎯 Architecture:"
echo "   Services → Collector (port 4317) → Tempo → Grafana"
echo ""
echo "📖 Next Steps:"
echo "   • View traces in Grafana: http://localhost:3000"
echo "   • Check collector metrics: http://localhost:8888/metrics"
echo "   • View collector logs: docker compose logs -f otel-collector"
echo "   • Read guide: COLLECTOR_GUIDE.md"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
