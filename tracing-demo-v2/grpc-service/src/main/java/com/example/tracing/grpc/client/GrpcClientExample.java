package com.example.tracing.grpc.client;

import com.example.tracing.grpc.proto.ProductServiceGrpc;
import com.example.tracing.grpc.proto.ProductServiceProto;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Example gRPC client demonstrating how to call the gRPC service
 * with proper trace propagation.
 * 
 * This is an example component - in a real application, you would
 * inject this as a service dependency.
 */
@Slf4j
@Component
public class GrpcClientExample {
    
    private final Tracer tracer;
    private final ManagedChannel channel;
    private final ProductServiceGrpc.ProductServiceBlockingStub stub;
    
    public GrpcClientExample(Tracer tracer) {
        this.tracer = tracer;
        // Create a channel to the gRPC server
        this.channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext() // For local development only - use TLS in production
                .build();
        this.stub = ProductServiceGrpc.newBlockingStub(channel);
    }
    
    /**
     * Example: Create a product via gRPC with tracing.
     * Trace context is automatically propagated by Spring Boot's OpenTelemetry.
     */
    @Observed(name = "grpc.client.product.create", contextualName = "grpc-client-product-create")
    public ProductServiceProto.ProductResponse createProduct(
            String name, String description, double price, int stock) {
        
        String traceId = getCurrentTraceId();
        log.info("Creating product via gRPC client: name={}, traceId={}", name, traceId);
        
        ProductServiceProto.CreateProductRequest request = ProductServiceProto.CreateProductRequest.newBuilder()
                .setName(name)
                .setDescription(description)
                .setPrice(price)
                .setStock(stock)
                .build();
        
        ProductServiceProto.ProductResponse response = stub.createProduct(request);
        
        log.info("Product created via gRPC: productId={}, name={}, traceId={}", 
                response.getProductId(), response.getName(), traceId);
        
        return response;
    }
    
    /**
     * Example: Get a product via gRPC with tracing.
     */
    @Observed(name = "grpc.client.product.get", contextualName = "grpc-client-product-get")
    public ProductServiceProto.ProductResponse getProduct(String productId) {
        
        String traceId = getCurrentTraceId();
        log.info("Getting product via gRPC client: productId={}, traceId={}", productId, traceId);
        
        ProductServiceProto.GetProductRequest request = ProductServiceProto.GetProductRequest.newBuilder()
                .setProductId(productId)
                .build();
        
        ProductServiceProto.ProductResponse response = stub.getProduct(request);
        
        log.info("Product retrieved via gRPC: productId={}, name={}, traceId={}", 
                productId, response.getName(), traceId);
        
        return response;
    }
    
    /**
     * Helper method to get the current trace ID for logging.
     */
    private String getCurrentTraceId() {
        if (tracer != null && tracer.currentSpan() != null) {
            return tracer.currentSpan().context().traceId();
        }
        return "unknown";
    }
    
    /**
     * Cleanup method to close the channel.
     * In a real application, use @PreDestroy or manage lifecycle properly.
     */
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }
}
