package com.example.tracing.cqrs.domain.queries;

import com.example.tracing.cqrs.domain.Product;
import com.example.tracing.cqrs.infrastructure.query.Query;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Query to get a product by ID.
 */
@Data
@Builder
public class GetProductByIdQuery implements Query<Product> {
    
    @Builder.Default
    private final String queryId = UUID.randomUUID().toString();
    
    private final String productId;
}
