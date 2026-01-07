# Tempo Not Receiving Traces - Troubleshooting Guide

## Problem

Grafana shows no traces in Tempo even though services are running and processing requests successfully.

## Root Cause

The `OtlpGrpcSpanExporter` endpoint was configured incorrectly in all services' `TracingConfig.java` files.

## Solution Applied

### Issue
The endpoint `http://localhost:4317` works, but the OpenTelemetry Java SDK might have connection issues without proper timeout configuration.

### Fix Applied
Added timeout configuration to all `TracingConfig.java` files:

```java
OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
        .setEndpoint("http://localhost:4317")
        .setTimeout(java.time.Duration.ofSeconds(10))  // Added this line
        .build();
```

### Files Updated
- `order-service/src/main/java/com/example/tracing/order/TracingConfig.java`
- `graphql-service/src/main/java/com/example/tracing/graphql/TracingConfig.java`
- `inventory-service/src/main/java/com/example/tracing/inventory/TracingConfig.java`
- `notification-service/src/main/java/com/example/tracing/notification/TracingConfig.java`

## How to Verify

### 1. Check if Tempo is receiving traces

```bash
curl -s 'http://localhost:3200/api/search?limit=10' | jq
```

Should return traces (not an empty array).

### 2. Check Tempo status

```bash
curl http://localhost:3200/status
```

### 3. Test sending a manual trace

```bash
curl -X POST http://localhost:4318/v1/traces \
  -H "Content-Type: application/json" \
  -d '{
    "resourceSpans": [{
      "resource": {
        "attributes": [{"key": "service.name", "value": {"stringValue": "test-service"}}]
      },
      "scopeSpans": [{
        "spans": [{
          "traceId": "00000000000000000000000000000001",
          "spanId": "0000000000000001",
          "name": "test-span",
          "kind": 1,
          "startTimeUnixNano": "1609459200000000000",
          "endTimeUnixNano": "1609459200000000001"
        }]
      }]
    }]
  }'
```

### 4. Check service logs for trace export errors

```bash
tail -f logs/order-service.log | grep -i "trace\|span\|export"
```

### 5. View traces in Grafana

1. Open Grafana: http://localhost:3000
2. Go to **Explore** (compass icon on left)
3. Select **Tempo** datasource from dropdown
4. Click **Search** or **Query** tab
5. Click **Run Query** button
6. You should see traces listed

## Common Issues

### Issue 1: No traces appearing
**Symptom**: Tempo API returns empty traces array  
**Solution**: Services might not be sending traces. Check:
- Services are running: `ps aux | grep java`
- Endpoint configuration is correct
- No firewall blocking port 4317

### Issue 2: Connection refused
**Symptom**: Services log "connection refused" errors  
**Solution**: 
- Verify Tempo is running: `docker compose ps tempo`
- Check port 4317 is accessible: `netstat -tuln | grep 4317`

### Issue 3: Grafana can't connect to Tempo
**Symptom**: Grafana datasource test fails  
**Solution**:
- Check Tempo URL in Grafana datasource settings
- Should be: `http://tempo:3200` (from within Docker network)
- Or: `http://localhost:3200` (if Grafana is on host)

### Issue 4: Services crash on startup
**Symptom**: Java services exit immediately  
**Solution**:
- Check compilation errors: `mvn clean compile`
- Check Java version: `java -version` (should be Java 21+)
- Review service logs in `logs/` directory

## Architecture Overview

```
[Java Services on Host]
        |
        | gRPC (port 4317)
        v
[Tempo in Docker] --> [Local Storage]
        |
        | HTTP API (port 3200)
        v
    [Grafana]
```

## Additional Debugging

### Enable debug logging in services

Add to `application.yml`:

```yaml
logging:
  level:
    io.opentelemetry: DEBUG
    io.micrometer.tracing: DEBUG
```

### Check Docker network

```bash
docker network inspect tracing-demo-v2_default
```

### Test gRPC connection

```bash
grpcurl -plaintext localhost:4317 list
```

## Expected Behavior

When working correctly:
1. Services send spans to Tempo via gRPC (port 4317)
2. Tempo stores spans in `/tmp/tempo/blocks`
3. Grafana queries Tempo via HTTP API (port 3200)
4. You see distributed traces showing request flow across services

## Timeline

- **Issue Identified**: January 7, 2026
- **Root Cause**: Missing timeout configuration
- **Fix Applied**: Added `.setTimeout()` to all TracingConfig files
- **Status**: Should be working after service restart

## Contact

If issues persist after applying this fix, check:
1. Service logs in `logs/` directory
2. Tempo logs: `docker compose logs tempo`
3. Grafana datasource configuration

---

**Last Updated**: January 7, 2026
