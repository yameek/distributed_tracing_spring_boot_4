# gRPC Service with Distributed Tracing

This service demonstrates how to implement a gRPC service with proper distributed tracing using Spring Boot and OpenTelemetry.

## Features

- ✅ **gRPC Service**: Product management service with CRUD operations
- ✅ **Automatic Tracing**: OpenTelemetry automatically instruments gRPC calls
- ✅ **Custom Spans**: `@Observed` annotations for business logic spans
- ✅ **Trace Propagation**: Trace context automatically propagated through gRPC calls
- ✅ **Structured Logging**: JSON logs with trace IDs integrated with Loki

## Service Endpoints

The service exposes the following gRPC methods:

- `GetProduct` - Get a product by ID
- `CreateProduct` - Create a new product
- `UpdateProductPrice` - Update product price
- `ListProducts` - List all products with pagination

## Configuration

**gRPC Port**: 9090  
**HTTP Port**: 8086 (for actuator endpoints)

## Running the Service

```bash
cd tracing-demo-v2
./gradlew :grpc-service:bootRun
```

## Testing the Service

### Using grpcurl (Recommended)

Install grpcurl:
```bash
# macOS
brew install grpcurl

# Linux
# Download from https://github.com/fullstorydev/grpcurl/releases
```

#### 1. Create a Product
```bash
grpcurl -plaintext -d '{
  "name": "Test Product",
  "description": "A test product",
  "price": 29.99,
  "stock": 100
}' localhost:9090 com.example.tracing.grpc.ProductService/CreateProduct
```

#### 2. Get a Product
```bash
grpcurl -plaintext -d '{
  "product_id": "YOUR_PRODUCT_ID"
}' localhost:9090 com.example.tracing.grpc.ProductService/GetProduct
```

#### 3. Update Product Price
```bash
grpcurl -plaintext -d '{
  "product_id": "YOUR_PRODUCT_ID",
  "new_price": 39.99
}' localhost:9090 com.example.tracing.grpc.ProductService/UpdateProductPrice
```

#### 4. List Products
```bash
grpcurl -plaintext -d '{
  "page": 0,
  "page_size": 10
}' localhost:9090 com.example.tracing.grpc.ProductService/ListProducts
```

### Using a gRPC Client (Java Example)

See `GrpcClientExample.java` for a complete example of calling the service from another Java application.

## Tracing

### Automatic Instrumentation

Spring Boot's OpenTelemetry starter automatically:
- Creates spans for incoming gRPC requests
- Propagates trace context through gRPC metadata
- Exports traces to the OpenTelemetry Collector (port 4317)

### Custom Spans

The service uses `@Observed` annotations to create custom spans:
- `grpc.product.get` - Get product operation
- `grpc.product.create` - Create product operation
- `grpc.product.update.price` - Update price operation
- `grpc.product.list` - List products operation

### Viewing Traces

1. Make a gRPC call to the service
2. Open Grafana: http://localhost:3000
3. Go to Explore → Select Tempo
4. Search for traces from `grpc-service`
5. View the complete trace showing:
   - gRPC server span (automatic)
   - Business logic span (`@Observed` annotation)
   - All with the same trace ID!

## Trace Context Propagation

When calling this gRPC service from another service:

1. **Automatic**: If using Spring Boot's gRPC client with OpenTelemetry, trace context is automatically propagated
2. **Manual**: You can manually add trace context to gRPC metadata:

```java
Metadata metadata = new Metadata();
metadata.put(Metadata.Key.of("traceparent", Metadata.ASCII_STRING_MARSHALLER), traceParentHeader);
```

## Architecture

```
Client (gRPC)
    ↓
gRPC Service (port 9090)
    ↓
OpenTelemetry Instrumentation (automatic)
    ↓
@Observed Annotations (custom spans)
    ↓
OpenTelemetry Collector (port 4317)
    ↓
Tempo (trace storage)
    ↓
Grafana (visualization)
```

## Dependencies

- `spring-grpc-starter` (1.0.0) - Official Spring gRPC integration (compatible with Spring Boot 4.x)
- `spring-boot-starter-opentelemetry` - OpenTelemetry tracing
- `protobuf` - Protocol Buffers for gRPC

## Logs

Logs are written in JSON format with trace IDs:
- Console output (structured JSON)
- File: `logs/grpc-service.json.log` (with rotation)
- Loki: http://localhost:3100 (log aggregation)

## Health Check

```bash
curl http://localhost:8086/actuator/health
```
