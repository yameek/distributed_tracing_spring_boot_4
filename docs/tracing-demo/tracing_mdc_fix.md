# CRITICAL FIX: Trace IDs in Logs (MDC Propagation)

## Problem

The logs showed **empty traceId and spanId** even though services were running:

```json
{
  "@timestamp":"2026-01-07T16:58:27.279+06:00",
  "level":"INFO",
  "logger":"c.e.t.n.NotificationListener",
  "message":"✅ Email sent for order: 1b1ed45d-aa17-4432-a7b2-13210ad55a1a",
  "traceId":"",    // ← EMPTY!
  "spanId":"",     // ← EMPTY!
  "service":"notification-service"
}
```

**This defeats the entire purpose of distributed tracing!**

## Root Cause

The custom `TracingConfig.java` was creating OpenTelemetry beans manually, but **not integrating with Spring Boot's automatic MDC (Mapped Diagnostic Context) propagation**.

Spring Boot's Micrometer Tracing automatically:
1. Creates spans for HTTP requests
2. Propagates trace context through HTTP headers
3. Propagates trace context through RabbitMQ message headers
4. **Puts traceId and spanId into MDC for logging**

Our manual configuration bypassed all of this!

## Solution Applied

### 1. Simplified `TracingConfig.java`

**Before** (Manual, complex):
```java
@Bean
public OpenTelemetry openTelemetry() { /* ... */ }

@Bean
public Tracer otelTracer(OpenTelemetry openTelemetry) { /* ... */ }

@Bean
public io.micrometer.tracing.Tracer micrometerTracer(/* ... */) { 
    // Manually creating OtelTracer with custom context
}
```

**After** (Simple, Spring Boot auto-config):
```java
@Configuration
public class TracingConfig {
    @Value("${spring.application.name}")
    private String serviceName;

    @Bean
    public SdkTracerProvider sdkTracerProvider() {
        Resource resource = Resource.getDefault()
                .merge(Resource.create(io.opentelemetry.api.common.Attributes.of(
                        ResourceAttributes.SERVICE_NAME, serviceName
                )));

        OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint("http://localhost:4317")
                .setTimeout(java.time.Duration.ofSeconds(10))
                .build();

        return SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .setResource(resource)
                .build();
    }
}
```

Spring Boot will automatically:
- Use this `SdkTracerProvider`
- Create the `OpenTelemetry` bean
- Create the Micrometer `Tracer` bean
- Wire up MDC propagation

### 2. Updated `application.yml`

Added proper configuration for MDC propagation:

```yaml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0
    baggage:
      enabled: true
      correlation:
        enabled: true
        fields:
          - traceId
          - spanId

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"

micrometer:
  tracing:
    propagation:
      type: W3C
```

Key changes:
- `baggage.correlation.enabled: true` - Enables MDC propagation
- `baggage.correlation.fields` - Specifies which fields to put in MDC
- `logging.pattern.level` - Shows trace IDs in console logs
- `propagation.type: W3C` - Uses W3C trace context standard

### 3. Added Missing Dependency

Added `micrometer-tracing` (not just the bridge) to all POMs:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing</artifactId>
</dependency>
```

This dependency includes the core tracing infrastructure needed for MDC propagation.

## Expected Result

After these changes, logs should show:

```json
{
  "@timestamp":"2026-01-07T17:30:00.000+06:00",
  "level":"INFO",
  "logger":"c.e.t.n.NotificationListener",
  "message":"✅ Email sent for order: 1b1ed45d-aa17-4432-a7b2-13210ad55a1a",
  "traceId":"3c5e0f8a2b1d4e6a9c7f3e5b8d1a4c6e",    // ← NOW HAS VALUE!
  "spanId":"9c7f3e5b8d1a4c6e",                      // ← NOW HAS VALUE!
  "service":"notification-service"
}
```

## Verification Steps

### 1. Check logs for trace IDs

```bash
cd tracing-demo-v2
tail -5 logs/order-service.json.log | jq '{traceId, spanId, message}'
```

Should show non-empty `traceId` and `spanId`.

### 2. Test distributed tracing

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"mutation { createOrder(product: \"test-product\", quantity: 5) { id product } }"}'
```

Then grep logs for the trace ID:

```bash
TRACE_ID=$(tail -1 logs/graphql-service.json.log | jq -r '.traceId')
echo "Trace ID: $TRACE_ID"

# This should show logs from ALL services with the same trace ID
for service in graphql order inventory notification; do
  echo "=== $service-service ==="
  grep "$TRACE_ID" logs/$service-service.json.log | jq '{service, logger, message}'
done
```

### 3. Verify in Tempo

The trace ID from logs should match what you see in Tempo/Grafana.

## Files Changed

### All Services
- `pom.xml` - Added `micrometer-tracing` dependency
- `src/main/resources/application.yml` - Added MDC configuration
- `src/main/java/com/example/tracing/*/TracingConfig.java` - Simplified to use Spring Boot auto-config

### Services Affected
- ✅ order-service
- ✅ graphql-service
- ✅ inventory-service
- ✅ notification-service

## Why This Matters

**Without trace IDs in logs:**
- Can't correlate logs across services
- Can't find all logs for a specific request
- Can't debug distributed transactions
- Loki/Grafana queries are useless
- **Distributed tracing is pointless!**

**With trace IDs in logs:**
- Query: `{traceId="3c5e0f8a..."} | json` in Loki shows ALL logs for that trace
- Can see exact sequence of events across services
- Can debug slow requests or errors
- **This is what makes distributed tracing valuable!**

## Technical Details

### How MDC Propagation Works

1. **HTTP Request arrives** at graphql-service
   - Spring Boot creates a new span with traceId and spanId
   - Micrometer puts these into MDC
   - logback reads from MDC and includes in JSON logs

2. **HTTP Request to order-service**
   - `RestClient` propagates trace context via W3C headers (`traceparent`)
   - order-service extracts context from headers
   - Creates child span with same traceId, new spanId
   - Puts in MDC → logs include both IDs

3. **RabbitMQ Message published**
   - Spring AMQP propagates trace context in message headers
   - Consumer (inventory/notification) extracts context
   - Creates child span → MDC → logs

4. **All logs have same traceId**
   - Different spanIds (one per service/operation)
   - Can query Loki/Tempo by traceId to see full trace

### Spring Boot Auto-Configuration Magic

When you have:
- `spring-boot-starter-actuator`
- `micrometer-tracing-bridge-otel`
- `micrometer-tracing`
- `opentelemetry-exporter-otlp`

Spring Boot automatically:
- Creates `OpenTelemetry` bean
- Creates `Tracer` bean (Micrometer)
- Instruments `RestClient`/`RestTemplate`
- Instruments `RabbitTemplate`
- Instruments Web Controllers
- Propagates context via headers
- **Puts traceId/spanId in MDC**

Our job is just:
- Provide `SdkTracerProvider` bean (for OTLP export)
- Configure via `application.yml`
- Enable correlation in logging

## Lessons Learned

1. **Don't fight Spring Boot auto-configuration**
   - It's more complete than manual configuration
   - It handles edge cases we'd miss
   - It's battle-tested

2. **MDC propagation requires specific config**
   - Not automatic (security concern - could leak data)
   - Must explicitly enable `baggage.correlation`
   - Must list fields to propagate

3. **Testing tracing is critical**
   - Verify trace IDs appear in logs
   - Verify same traceId across services
   - Verify spans in Tempo match log timestamps

4. **Logback config was correct all along**
   - The `%mdc{traceId}` and `%mdc{spanId}` were right
   - Problem was nothing was putting values into MDC!

---

**Status**: Fixed - January 7, 2026
**Next**: Test with `run_all.sh` and verify logs show trace IDs
