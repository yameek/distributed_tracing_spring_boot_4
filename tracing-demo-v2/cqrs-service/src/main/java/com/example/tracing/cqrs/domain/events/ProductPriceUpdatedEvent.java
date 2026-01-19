package com.example.tracing.cqrs.domain.events;

import com.example.tracing.cqrs.infrastructure.event.DomainEvent;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event indicating a product price was updated.
 */
@Data
@Builder
public class ProductPriceUpdatedEvent implements DomainEvent {
    
    @Builder.Default
    private final String eventId = UUID.randomUUID().toString();
    
    @Builder.Default
    private final Instant occurredAt = Instant.now();
    
    private final String aggregateId; // Product ID
    private final BigDecimal oldPrice;
    private final BigDecimal newPrice;
}
