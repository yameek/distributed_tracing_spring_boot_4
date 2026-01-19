# Grafana Tempo Troubleshooting - GraphQL Service Traces Not Appearing

## Problem

You're seeing CQRS service traces in Grafana Tempo, but **NOT seeing GraphQL service traces**, even though:
- ✅ GraphQL service is generating trace IDs (visible in logs)
- ✅ OpenTelemetry Collector is running and accessible
- ✅ Configuration looks correct

## Root Cause

The GraphQL service is **generating traces but NOT exporting them** to the OpenTelemetry Collector. This is likely due to Spring Boot 4's OpenTelemetry auto-configuration not being fully initialized or the exporter not being properly configured.

## Solution Steps

### Step 1: Verify OpenTelemetry Exporter is Initialized

Check if the GraphQL service is actually trying to export traces:

```bash
# Look for OpenTelemetry initialization messages
cd "/home/yaziz/workspace/self_task/tracing basics/tracing-demo-v2"
grep -i "otlp\|opentelemetry\|exporter" logs/graphql-service.log | head -20
```

If you see NO messages about OTLP exporter, the exporter isn't being initialized.

### Step 2: Add Explicit OpenTelemetry Configuration

The issue is that Spring Boot 4's auto-configuration might not be picking up the OTLP exporter settings properly. Let's add explicit configuration.

#### Option A: Add Environment Variables (Quick Fix)

Edit `graphql-service/src/main/resources/application.yml` and add:

```yaml
spring:
  application:
    name: graphql-service
  # ... existing config ...

# Add these OpenTelemetry environment properties
otel:
  sdk:
    disabled: false
  traces:
    exporter: otlp
  exporter:
    otlp:
      endpoint: http://localhost:4317
      protocol: grpc
  service:
    name: graphql-service
  resource:
    attributes:
      service.name: graphql-service
      deployment.environment: production

management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0
  opentelemetry:
    tracing:
      export:
        otlp:
          enabled: true
          transport: grpc
          endpoint: http://localhost:4317
```

#### Option B: Add Java System Properties (Alternative)

Add to `graphql-service/build.gradle`:

```groovy
bootRun {
    systemProperties = [
        'otel.traces.exporter': 'otlp',
        'otel.exporter.otlp.endpoint': 'http://localhost:4317',
        'otel.exporter.otlp.protocol': 'grpc',
        'otel.service.name': 'graphql-service',
        'otel.sdk.disabled': 'false'
    ]
}
```

### Step 3: Verify Collector is Receiving Traces

After making changes, restart the GraphQL service and test:

```bash
# Restart GraphQL service
# (Stop it with Ctrl+C and restart with ./run_all.sh or individually)

# Monitor collector logs in real-time
docker logs -f otel-collector &

# Send a test request
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"mutation { createOrder(productId: \"test-trace-debug\", quantity: 1) { orderId status message } }"}'

# You should see output in collector logs like:
# "ResourceSpans" or "Span" or trace activity

# Stop monitoring
pkill -f "docker logs"
```

### Step 4: Check Why CQRS Works But GraphQL Doesn't

Let's compare the two services:

```bash
# Compare dependencies
diff graphql-service/build.gradle cqrs-service/build.gradle

# Compare application.yml
diff graphql-service/src/main/resources/application.yml cqrs-service/src/main/resources/application.yml
```

The CQRS service might have additional configuration or dependencies that make it work.

### Step 5: Enable Debug Logging

Add to `graphql-service/src/main/resources/application.yml`:

```yaml
logging:
  level:
    io.opentelemetry: DEBUG
    io.micrometer.tracing: DEBUG
    org.springframework.boot.actuate.autoconfigure.tracing: DEBUG
```

This will show you exactly what's happening with OpenTelemetry initialization.

### Step 6: Verify the Issue with a Simple Test

```bash
# 1. Send a GraphQL request and capture the trace ID
RESPONSE=$(curl -s -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"mutation { createOrder(productId: \"test-123\", quantity: 1) { orderId status message } }"}')

echo "$RESPONSE"

# 2. Get the trace ID from logs
TRACE_ID=$(tail -5 logs/graphql-service.log | grep -oE 'traceId":"[a-f0-9]{32}' | head -1 | cut -d'"' -f3)
echo "Trace ID: $TRACE_ID"

# 3. Wait a few seconds for export
sleep 5

# 4. Search in Grafana Tempo
# Go to http://localhost:3000
# Explore → Tempo → Search for trace ID: $TRACE_ID
# If it doesn't appear, traces aren't being exported
```

## Quick Workaround: Use HTTP Endpoint Instead of gRPC

If gRPC isn't working, try HTTP:

```yaml
management:
  opentelemetry:
    tracing:
      export:
        otlp:
          enabled: true
          transport: http  # Changed from grpc
          endpoint: http://localhost:4318  # Changed port from 4317
```

## Verification Checklist

- [ ] GraphQL service generates trace IDs in logs
- [ ] OpenTelemetry Collector is running (`docker ps | grep otel`)
- [ ] Collector ports are accessible (`netstat -tuln | grep 4317`)
- [ ] OTLP exporter is enabled in application.yml
- [ ] Service name is set correctly
- [ ] Collector logs show incoming spans (`docker logs otel-collector`)
- [ ] Traces appear in Grafana Tempo

## Expected Behavior

When working correctly, you should see:

1. **In GraphQL logs**: Trace IDs like `"traceId":"80f27aafa6e0b5eb38c240adeed61f80"`
2. **In Collector logs**: Messages about receiving and exporting spans
3. **In Grafana Tempo**: Traces from graphql-service with spans showing:
   - GraphQL mutation
   - HTTP call to order-service
   - Any other operations

## Why CQRS Works But GraphQL Doesn't

Possible reasons:

1. **Different Spring Boot starters**: CQRS might have additional dependencies
2. **Configuration differences**: CQRS might have explicit OTLP configuration
3. **Initialization order**: CQRS might initialize OpenTelemetry earlier in the startup
4. **Dependency versions**: Slight differences in transitive dependencies

## Next Steps

1. Apply Option A (add explicit OTLP configuration)
2. Restart GraphQL service
3. Test with Step 6 verification
4. Check collector logs for incoming spans
5. Search for traces in Grafana Tempo

If traces still don't appear after these steps, the issue might be:
- Network connectivity between service and collector
- OpenTelemetry SDK not being initialized properly
- Exporter being disabled by some other configuration

## Additional Debugging

```bash
# Check if GraphQL service can reach the collector
curl -v http://localhost:4317 2>&1 | head -10

# Check collector metrics
curl -s http://localhost:8888/metrics | grep -E "receiver_accepted_spans|exporter_sent_spans"

# Enable trace export logging in collector
# Edit config/otel-collector-config.yaml and change:
# debug:
#   verbosity: detailed  # Changed from 'normal'
```

---

**Created**: January 19, 2026  
**Issue**: GraphQL service traces not appearing in Grafana Tempo  
**Status**: Troubleshooting in progress
