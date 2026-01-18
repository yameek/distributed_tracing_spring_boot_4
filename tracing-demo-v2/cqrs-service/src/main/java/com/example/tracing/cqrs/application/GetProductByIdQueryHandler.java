package com.example.tracing.cqrs.application;

import com.example.tracing.cqrs.domain.Product;
import com.example.tracing.cqrs.domain.ProductRepository;
import com.example.tracing.cqrs.domain.queries.GetProductByIdQuery;
import com.example.tracing.cqrs.infrastructure.query.QueryHandler;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for GetProductByIdQuery.
 * Retrieves a product by its ID.
 */
@Slf4j
@Component
public class GetProductByIdQueryHandler implements QueryHandler<GetProductByIdQuery, Product> {
    
    private final ProductRepository productRepository;
    private final ObservationRegistry observationRegistry;
    
    public GetProductByIdQueryHandler(
            ProductRepository productRepository,
            ObservationRegistry observationRegistry) {
        this.productRepository = productRepository;
        this.observationRegistry = observationRegistry;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Product handle(GetProductByIdQuery query) {
        log.info("Handling GetProductByIdQuery: {} for product: {}", 
                query.getQueryId(), query.getProductId());
        
        return Observation.createNotStarted("query.handler.get.product.by.id", observationRegistry)
                .lowCardinalityKeyValue("query.id", query.getQueryId())
                .lowCardinalityKeyValue("product.id", query.getProductId())
                .observe(() -> {
                    Product product = productRepository.findById(query.getProductId())
                            .orElseThrow(() -> new ProductNotFoundException(
                                    "Product not found: " + query.getProductId()));
                    
                    log.info("Found product: {} - {}", product.getId(), product.getName());
                    
                    return product;
                });
    }
    
    @Override
    public Class<GetProductByIdQuery> getQueryType() {
        return GetProductByIdQuery.class;
    }
    
    public static class ProductNotFoundException extends RuntimeException {
        public ProductNotFoundException(String message) {
            super(message);
        }
    }
}
