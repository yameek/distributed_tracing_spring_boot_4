# gRPC Service Tracing Setup

This document explains how tracing is configured for the gRPC service.

## Automatic Tracing

Spring Boot's OpenTelemetry starter (`spring-boot-starter-opentelemetry`) automatically instruments gRPC calls:

1. **Server-side**: Creates spans for incoming gRPC requests
2. **Context Propagation**: Automatically propagates trace context through gRPC metadata
3. **Export**: Sends traces to OpenTelemetry Collector (port 4317)

## Custom Business Spans

The service uses `@Observed` annotations to create custom spans for business logic:

```java
@Observed(name = "grpc.product.get", contextualName = "grpc-product-get")
public void getProduct(...) {
    // Business logic
}
```

This creates a span named `grpc.product.get` that appears in traces, providing better observability into business operations.

## Trace Flow

```
gRPC Client Request
    ↓
gRPC Server (Spring gRPC)
    ↓
OpenTelemetry Server Interceptor (automatic)
    ↓ Creates server span
@Observed Annotation
    ↓ Creates business span
Business Logic
    ↓
Response
    ↓
OpenTelemetry Collector (port 4317)
    ↓
Tempo
    ↓
Grafana
```

## Configuration

### application.yml

```yaml
management:
  opentelemetry:
    tracing:
      export:
        otlp:
          enabled: true
          transport: grpc
          endpoint: http://localhost:4317
```

### Service Implementation

```java
@GrpcService  // Spring gRPC annotation
public class ProductServiceImpl extends ProductServiceGrpc.ProductServiceImplBase {
    
    @Observed(name = "grpc.product.get")
    public void getProduct(...) {
        // Implementation
    }
}
```

## Viewing Traces

1. Start the gRPC service: `./gradlew :grpc-service:bootRun`
2. Make a gRPC call (use `test_grpc_service.sh` or `grpcurl`)
3. Open Grafana: http://localhost:3000
4. Go to Explore → Select Tempo
5. Search for `service:grpc-service` or `name:grpc.product.get`
6. View the complete trace showing:
   - gRPC server span (automatic)
   - Business logic span (`@Observed`)
   - All operations with the same trace ID

## Trace Context Propagation

When calling this service from another service:

### Automatic (Recommended)

If using Spring Boot's gRPC client with OpenTelemetry, trace context is automatically propagated:

```java
@GrpcClient("grpc-service")
ProductServiceGrpc.ProductServiceBlockingStub productService;

// Trace context automatically propagated
ProductResponse response = productService.getProduct(request);
```

### Manual

You can manually add trace context to gRPC metadata:

```java
Metadata metadata = new Metadata();
String traceParent = getTraceParentHeader(); // Get from current span
metadata.put(Metadata.Key.of("traceparent", Metadata.ASCII_STRING_MARSHALLER), traceParent);

stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
    .getProduct(request);
```

## Logging

Logs include trace IDs via MDC (Mapped Diagnostic Context):

```java
log.info("Getting product: productId={}, traceId={}", productId, traceId);
```

The trace ID is automatically populated from the current span context.

## Testing

Use the provided test script:

```bash
./test_grpc_service.sh
```

Or use grpcurl directly:

```bash
grpcurl -plaintext -d '{"product_id": "123"}' \
  localhost:9090 com.example.tracing.grpc.ProductService/GetProduct
```
