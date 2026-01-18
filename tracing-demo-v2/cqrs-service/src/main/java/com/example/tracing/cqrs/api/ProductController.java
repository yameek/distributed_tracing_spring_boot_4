package com.example.tracing.cqrs.api;

import com.example.tracing.cqrs.domain.Product;
import com.example.tracing.cqrs.domain.commands.CreateProductCommand;
import com.example.tracing.cqrs.domain.commands.UpdateProductPriceCommand;
import com.example.tracing.cqrs.domain.commands.UpdateStockCommand;
import com.example.tracing.cqrs.domain.queries.GetAllProductsQuery;
import com.example.tracing.cqrs.domain.queries.GetProductByIdQuery;
import com.example.tracing.cqrs.infrastructure.command.CommandBus;
import com.example.tracing.cqrs.infrastructure.query.QueryBus;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Product operations.
 * Uses CQRS pattern with CommandBus and QueryBus.
 */
@Slf4j
@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final ObservationRegistry observationRegistry;
    
    public ProductController(
            CommandBus commandBus,
            QueryBus queryBus,
            ObservationRegistry observationRegistry) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
        this.observationRegistry = observationRegistry;
    }
    
    /**
     * Create a new product.
     */
    @PostMapping
    public ResponseEntity<CreateProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        
        log.info("Received request to create product: {}", request.getName());
        
        return Observation.createNotStarted("api.create.product", observationRegistry)
                .lowCardinalityKeyValue("product.name", request.getName())
                .observe(() -> {
                    CreateProductCommand command = CreateProductCommand.builder()
                            .name(request.getName())
                            .description(request.getDescription())
                            .price(request.getPrice())
                            .initialStock(request.getInitialStock())
                            .build();
                    
                    String productId = commandBus.dispatch(command);
                    
                    log.info("Product created successfully with ID: {}", productId);
                    
                    return ResponseEntity
                            .status(HttpStatus.CREATED)
                            .body(new CreateProductResponse(productId, "Product created successfully"));
                });
    }
    
    /**
     * Update product price.
     */
    @PutMapping("/{productId}/price")
    public ResponseEntity<ApiResponse> updatePrice(
            @PathVariable String productId,
            @Valid @RequestBody UpdatePriceRequest request) {
        
        log.info("Received request to update price for product: {}", productId);
        
        return Observation.createNotStarted("api.update.price", observationRegistry)
                .lowCardinalityKeyValue("product.id", productId)
                .observe(() -> {
                    UpdateProductPriceCommand command = UpdateProductPriceCommand.builder()
                            .productId(productId)
                            .newPrice(request.getNewPrice())
                            .build();
                    
                    commandBus.dispatch(command);
                    
                    log.info("Product price updated successfully for: {}", productId);
                    
                    return ResponseEntity.ok(new ApiResponse("Price updated successfully"));
                });
    }
    
    /**
     * Update product stock.
     */
    @PutMapping("/{productId}/stock")
    public ResponseEntity<ApiResponse> updateStock(
            @PathVariable String productId,
            @Valid @RequestBody UpdateStockRequest request) {
        
        log.info("Received request to update stock for product: {}", productId);
        
        return Observation.createNotStarted("api.update.stock", observationRegistry)
                .lowCardinalityKeyValue("product.id", productId)
                .observe(() -> {
                    UpdateStockCommand command = UpdateStockCommand.builder()
                            .productId(productId)
                            .quantity(request.getQuantity())
                            .build();
                    
                    commandBus.dispatch(command);
                    
                    log.info("Product stock updated successfully for: {}", productId);
                    
                    return ResponseEntity.ok(new ApiResponse("Stock updated successfully"));
                });
    }
    
    /**
     * Get product by ID.
     */
    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProduct(@PathVariable String productId) {
        
        log.info("Received request to get product: {}", productId);
        
        return Observation.createNotStarted("api.get.product", observationRegistry)
                .lowCardinalityKeyValue("product.id", productId)
                .observe(() -> {
                    GetProductByIdQuery query = GetProductByIdQuery.builder()
                            .productId(productId)
                            .build();
                    
                    Product product = queryBus.dispatch(query);
                    
                    return ResponseEntity.ok(product);
                });
    }
    
    /**
     * Get all products.
     */
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        
        log.info("Received request to get all products");
        
        return Observation.createNotStarted("api.get.all.products", observationRegistry)
                .observe(() -> {
                    GetAllProductsQuery query = GetAllProductsQuery.builder().build();
                    
                    List<Product> products = queryBus.dispatch(query);
                    
                    return ResponseEntity.ok(products);
                });
    }
    
    /**
     * Exception handler for the controller.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Error processing request", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(e.getMessage()));
    }
}
