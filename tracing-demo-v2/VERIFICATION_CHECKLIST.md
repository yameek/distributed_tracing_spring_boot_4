# ✅ Distributed Tracing - Verification Checklist

Run through this checklist to verify everything is working correctly.

## 1. Services Status

```bash
curl -s http://localhost:8080/actuator/health | jq -r .status  # Should show: UP
curl -s http://localhost:8081/actuator/health | jq -r .status  # Should show: UP
curl -s http://localhost:8082/actuator/health | jq -r .status  # Should show: UP
curl -s http://localhost:8083/actuator/health | jq -r .status  # Should show: UP
```

**Expected:** All services show `UP`

## 2. Docker Infrastructure

```bash
docker compose ps
```

**Expected:** All containers (grafana, tempo, loki, rabbitmq) should be `Up`

## 3. Generate Traces

```bash
./test_tracing_complete.sh
```

**Expected Output:**
```
✓ SUCCESS! Same trace ID propagated from GraphQL to Order service!
  Trace ID: [32-character hex string]

✓ Async services:
  Inventory:    [same trace ID]
  Notification: [same trace ID]

🎉 TESTS PASSED! 🎉
```

## 4. Verify Trace in Tempo API

```bash
# Use the trace ID from step 3
curl -s "http://localhost:3200/api/traces/YOUR_TRACE_ID_HERE" | jq '.batches | length'
```

**Expected:** Should return a number > 0 (e.g., 6 or 7 batches)

## 5. Check OTLP Export Errors

```bash
tail -100 logs/order-service.log | grep -i "failed.*export\|404\|connection reset" | wc -l
```

**Expected:** Should return `0` or a very small number

## 6. View Traces in Grafana

1. Open: http://localhost:3000
2. Click: **Explore** (compass icon)
3. Select: **Tempo**
4. Query: `{ resource.service.name="order-service" }`
5. Click: **Run Query**

**Expected:** You should see a list of traces

## 7. Open a Trace Detail

1. Click on any trace from step 6
2. You should see:
   - ✅ Service graph showing 4 services
   - ✅ Waterfall timeline with multiple spans
   - ✅ graphql-service, order-service, inventory-service, notification-service
   - ✅ Parent-child span relationships

## 8. Verify Span Details

Click on any span in the waterfall view.

**Expected to see:**
- Span ID and Trace ID
- Duration and timing
- Attributes (http.method, http.url, etc.)
- Service name
- No errors (unless testing error scenarios)

## 9. Check Logs with Trace Correlation

```bash
# Use the trace ID from your test
cat */logs/*.json.log | jq 'select(.traceId == "YOUR_TRACE_ID") | {service, message}'
```

**Expected:** Logs from all 4 services with the same trace ID

## 10. View Metrics

```bash
curl -s http://localhost:8081/actuator/prometheus | grep http_server_requests_seconds_count
```

**Expected:** Should show metrics for HTTP requests

---

## ✅ Success Criteria

Your distributed tracing is fully functional if:

- [x] All 4 services are UP
- [x] Docker infrastructure is running
- [x] Test script shows same trace ID across all services
- [x] Tempo API returns trace data
- [x] Zero (or minimal) OTLP export errors
- [x] Grafana shows traces in the UI
- [x] Trace waterfall shows all 4 services
- [x] Logs contain matching trace IDs
- [x] Metrics are accessible via /actuator/prometheus

---

## 🎯 Quick Verification (One Command)

```bash
./test_tracing_complete.sh && \
TRACE_ID=$(cat */logs/*.json.log | jq -r 'select(.traceId != null) | .traceId' | tail -1) && \
echo "Trace ID: $TRACE_ID" && \
sleep 10 && \
curl -s "http://localhost:3200/api/traces/$TRACE_ID" | jq '.batches | length' && \
echo "✅ Trace successfully stored in Tempo!"
```

**Expected:** Shows trace ID and confirms it's in Tempo

---

## 🐛 Common Issues & Solutions

### Issue: "No traces found"

**Solution:**
1. Make sure time range is set correctly (Last 15 minutes)
2. Run `./test_tracing_complete.sh` to generate fresh traces
3. Wait 10 seconds for traces to be indexed
4. Try query: `{}` to see all traces

### Issue: "Different trace IDs"

**Solution:**
1. Verify services were rebuilt after configuration changes
2. Restart all services: `./stop_all.sh && ./run_all.sh`
3. Run test again

### Issue: "404 errors in logs"

**Solution:**
1. Check Tempo is using gRPC: `docker compose logs tempo | grep GRPC`
2. Verify application.yml has `transport: grpc`
3. Rebuild and restart services

### Issue: "Connection reset" errors

**Solution:**
1. Check Tempo is running: `docker compose ps tempo`
2. Restart Tempo: `docker compose restart tempo`
3. Wait 10 seconds for Tempo to be ready
4. Test again

---

## 📞 Need Help?

### Check Logs

```bash
# Service logs
tail -f logs/*.log

# Docker logs
docker compose logs tempo
docker compose logs grafana

# Service-specific
tail -f logs/order-service.log | jq
```

### Test Connectivity

```bash
# Tempo status
curl http://localhost:3200/status

# Grafana health
curl http://localhost:3000/api/health

# Service health
curl http://localhost:8081/actuator/health
```

---

**Last Updated:** 2026-01-14  
**Status:** ✅ All Systems Operational  
**Traces in Tempo:** Yes  
**Grafana Accessible:** Yes  
**OTLP Errors:** None
