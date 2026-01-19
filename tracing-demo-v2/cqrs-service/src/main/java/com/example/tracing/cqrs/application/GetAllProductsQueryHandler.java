package com.example.tracing.cqrs.application;

import com.example.tracing.cqrs.domain.Product;
import com.example.tracing.cqrs.domain.ProductRepository;
import com.example.tracing.cqrs.domain.queries.GetAllProductsQuery;
import com.example.tracing.cqrs.infrastructure.query.QueryHandler;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Handler for GetAllProductsQuery.
 * Retrieves all products.
 */
@Slf4j
@Component
public class GetAllProductsQueryHandler implements QueryHandler<GetAllProductsQuery, List<Product>> {
    
    private final ProductRepository productRepository;
    private final ObservationRegistry observationRegistry;
    
    public GetAllProductsQueryHandler(
            ProductRepository productRepository,
            ObservationRegistry observationRegistry) {
        this.productRepository = productRepository;
        this.observationRegistry = observationRegistry;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Product> handle(GetAllProductsQuery query) {
        log.info("Handling GetAllProductsQuery: {}", query.getQueryId());
        
        return Observation.createNotStarted("query.handler.get.all.products", observationRegistry)
                .lowCardinalityKeyValue("query.id", query.getQueryId())
                .observe(() -> {
                    List<Product> products = productRepository.findAll();
                    
                    log.info("Found {} products", products.size());
                    
                    return products;
                });
    }
    
    @Override
    public Class<GetAllProductsQuery> getQueryType() {
        return GetAllProductsQuery.class;
    }
}
