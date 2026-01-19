package com.example.tracing.cqrs.domain.queries;

import com.example.tracing.cqrs.domain.Product;
import com.example.tracing.cqrs.infrastructure.query.Query;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Query to get all products.
 */
@Data
@Builder
public class GetAllProductsQuery implements Query<List<Product>> {
    
    @Builder.Default
    private final String queryId = UUID.randomUUID().toString();
}
