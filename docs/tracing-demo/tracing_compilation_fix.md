# Tracing Demo Compilation Fix

## Issue

The tracing demo services were failing to compile with the following error:

```
[ERROR] constructor EventPublishingContextWrapper in class io.micrometer.tracing.otel.bridge.EventPublishingContextWrapper cannot be applied to given types;
  required: io.micrometer.tracing.otel.bridge.OtelTracer.EventPublisher
  found:    no arguments
```

## Root Cause

The `EventPublishingContextWrapper` constructor signature changed in Micrometer Tracing 1.6.1 to require an `EventPublisher` parameter. Additionally, the `OtelTracer` constructor now requires a `BaggageManager` instead of just a context wrapper.

## Solution

Updated all `TracingConfig.java` files across all services to properly instantiate the tracing components:

### Changes Made

1. **Added proper imports**:
   ```java
   import io.micrometer.tracing.otel.bridge.*;
   import java.util.Collections;
   ```

2. **Updated micrometerTracer bean**:
   ```java
   @Bean
   public io.micrometer.tracing.Tracer micrometerTracer(Tracer otelTracer, OpenTelemetry openTelemetry) {
       OtelCurrentTraceContext otelCurrentTraceContext = new OtelCurrentTraceContext();
       
       // Create event publisher
       OtelTracer.EventPublisher eventPublisher = event -> {
           // Default implementation - can be customized for event handling
       };
       
       // Create baggage manager
       OtelBaggageManager baggageManager = new OtelBaggageManager(
           otelCurrentTraceContext, 
           Collections.emptyList(), 
           Collections.emptyList()
       );
       
       return new OtelTracer(otelTracer, otelCurrentTraceContext, eventPublisher, baggageManager);
   }
   ```

### Services Fixed

- ✅ `order-service/src/main/java/com/example/tracing/order/TracingConfig.java`
- ✅ `graphql-service/src/main/java/com/example/tracing/graphql/TracingConfig.java`
- ✅ `inventory-service/src/main/java/com/example/tracing/inventory/TracingConfig.java`
- ✅ `notification-service/src/main/java/com/example/tracing/notification/TracingConfig.java`

## Verification

All services now compile successfully:

```bash
cd order-service && mvn compile        # ✅ SUCCESS
cd graphql-service && mvn compile      # ✅ SUCCESS
cd inventory-service && mvn compile    # ✅ SUCCESS
cd notification-service && mvn compile # ✅ SUCCESS
```

Services can now be started using:

```bash
./run_all.sh
```

## API Changes Summary

| Component | Old (Broken) | New (Fixed) |
|-----------|-------------|-------------|
| `EventPublishingContextWrapper` | `new EventPublishingContextWrapper()` | `new EventPublishingContextWrapper(eventPublisher)` |
| `OtelTracer` constructor | 3 params (tracer, context, wrapper) | 4 params (tracer, context, eventPublisher, baggageManager) |
| Baggage Manager | Not required | Required `OtelBaggageManager` instance |

## Related Documentation

- Micrometer Tracing 1.6.1 Release Notes
- Spring Boot 4.0.1 Tracing Documentation
- [Java 25 Migration Guide](../migration/tracing_java_25_migration.md)

## Date Fixed

January 7, 2026
