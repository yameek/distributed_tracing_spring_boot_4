package com.example.tracing.cqrs.domain.events;

import com.example.tracing.cqrs.infrastructure.event.DomainEvent;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event indicating a product was created.
 */
@Data
@Builder
public class ProductCreatedEvent implements DomainEvent {
    
    @Builder.Default
    private final String eventId = UUID.randomUUID().toString();
    
    @Builder.Default
    private final Instant occurredAt = Instant.now();
    
    private final String aggregateId; // Product ID
    private final String name;
    private final String description;
    private final BigDecimal price;
    private final Integer initialStock;
}
