package com.example.tracing.grpc.service;

import com.example.tracing.grpc.proto.ProductServiceGrpc;
import com.example.tracing.grpc.proto.ProductServiceProto;
import io.grpc.stub.StreamObserver;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.annotation.GrpcService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * gRPC service implementation with proper tracing instrumentation.
 * 
 * Spring Boot's OpenTelemetry starter automatically instruments gRPC calls,
 * but we add @Observed annotations for custom business spans to provide
 * better observability into our business logic.
 */
@Slf4j
@GrpcService
public class ProductServiceImpl extends ProductServiceGrpc.ProductServiceImplBase {
    
    private final Tracer tracer;
    
    // In-memory storage for demo purposes
    private final Map<String, ProductServiceProto.ProductResponse> products = new ConcurrentHashMap<>();
    
    public ProductServiceImpl(Tracer tracer) {
        this.tracer = tracer;
    }
    
    /**
     * Get a product by ID.
     * Trace context is automatically propagated from the gRPC call.
     */
    @Override
    @Observed(name = "grpc.product.get", contextualName = "grpc-product-get")
    public void getProduct(
            ProductServiceProto.GetProductRequest request,
            StreamObserver<ProductServiceProto.ProductResponse> responseObserver) {
        
        String traceId = getCurrentTraceId();
        String productId = request.getProductId();
        
        log.info("Getting product via gRPC: productId={}, traceId={}", productId, traceId);
        
        ProductServiceProto.ProductResponse product = products.get(productId);
        
        if (product == null) {
            log.warn("Product not found: productId={}, traceId={}", productId, traceId);
            responseObserver.onError(
                io.grpc.Status.NOT_FOUND
                    .withDescription("Product not found: " + productId)
                    .asRuntimeException()
            );
            return;
        }
        
        log.info("Product retrieved: productId={}, name={}, traceId={}", 
                productId, product.getName(), traceId);
        
        responseObserver.onNext(product);
        responseObserver.onCompleted();
    }
    
    /**
     * Create a new product.
     * Trace context is automatically propagated from the gRPC call.
     */
    @Override
    @Observed(name = "grpc.product.create", contextualName = "grpc-product-create")
    public void createProduct(
            ProductServiceProto.CreateProductRequest request,
            StreamObserver<ProductServiceProto.ProductResponse> responseObserver) {
        
        String traceId = getCurrentTraceId();
        
        log.info("Creating product via gRPC: name={}, traceId={}", request.getName(), traceId);
        
        String productId = UUID.randomUUID().toString();
        
        ProductServiceProto.ProductResponse product = ProductServiceProto.ProductResponse.newBuilder()
                .setProductId(productId)
                .setName(request.getName())
                .setDescription(request.getDescription())
                .setPrice(request.getPrice())
                .setStock(request.getStock())
                .setStatus("ACTIVE")
                .setCreatedAt(System.currentTimeMillis())
                .build();
        
        products.put(productId, product);
        
        log.info("Product created: productId={}, name={}, traceId={}", 
                productId, product.getName(), traceId);
        
        responseObserver.onNext(product);
        responseObserver.onCompleted();
    }
    
    /**
     * Update product price.
     * Trace context is automatically propagated from the gRPC call.
     */
    @Override
    @Observed(name = "grpc.product.update.price", contextualName = "grpc-product-update-price")
    public void updateProductPrice(
            ProductServiceProto.UpdateProductPriceRequest request,
            StreamObserver<ProductServiceProto.ProductResponse> responseObserver) {
        
        String traceId = getCurrentTraceId();
        String productId = request.getProductId();
        double newPrice = request.getNewPrice();
        
        log.info("Updating product price via gRPC: productId={}, newPrice={}, traceId={}", 
                productId, newPrice, traceId);
        
        ProductServiceProto.ProductResponse existingProduct = products.get(productId);
        
        if (existingProduct == null) {
            log.warn("Product not found for price update: productId={}, traceId={}", 
                    productId, traceId);
            responseObserver.onError(
                io.grpc.Status.NOT_FOUND
                    .withDescription("Product not found: " + productId)
                    .asRuntimeException()
            );
            return;
        }
        
        ProductServiceProto.ProductResponse updatedProduct = ProductServiceProto.ProductResponse.newBuilder(existingProduct)
                .setPrice(newPrice)
                .build();
        
        products.put(productId, updatedProduct);
        
        log.info("Product price updated: productId={}, oldPrice={}, newPrice={}, traceId={}", 
                productId, existingProduct.getPrice(), newPrice, traceId);
        
        responseObserver.onNext(updatedProduct);
        responseObserver.onCompleted();
    }
    
    /**
     * List all products with pagination.
     * Trace context is automatically propagated from the gRPC call.
     */
    @Override
    @Observed(name = "grpc.product.list", contextualName = "grpc-product-list")
    public void listProducts(
            ProductServiceProto.ListProductsRequest request,
            StreamObserver<ProductServiceProto.ListProductsResponse> responseObserver) {
        
        String traceId = getCurrentTraceId();
        int page = request.getPage();
        int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 10;
        
        log.info("Listing products via gRPC: page={}, pageSize={}, traceId={}", 
                page, pageSize, traceId);
        
        List<ProductServiceProto.ProductResponse> productList = new ArrayList<>(products.values());
        int total = productList.size();
        
        // Simple pagination
        int start = page * pageSize;
        int end = Math.min(start + pageSize, total);
        List<ProductServiceProto.ProductResponse> paginatedProducts = 
                start < total ? productList.subList(start, end) : Collections.emptyList();
        
        ProductServiceProto.ListProductsResponse response = ProductServiceProto.ListProductsResponse.newBuilder()
                .addAllProducts(paginatedProducts)
                .setTotal(total)
                .setPage(page)
                .setPageSize(pageSize)
                .build();
        
        log.info("Products listed: total={}, returned={}, page={}, traceId={}", 
                total, paginatedProducts.size(), page, traceId);
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
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
}
