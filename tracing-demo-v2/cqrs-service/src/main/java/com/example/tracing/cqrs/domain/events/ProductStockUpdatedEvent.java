package com.example.tracing.cqrs.domain.events;

import com.example.tracing.cqrs.infrastructure.event.DomainEvent;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Event indicating product stock was updated.
 */
@Data
@Builder
public class ProductStockUpdatedEvent implements DomainEvent {
    
    @Builder.Default
    private final String eventId = UUID.randomUUID().toString();
    
    @Builder.Default
    private final Instant occurredAt = Instant.now();
    
    private final String aggregateId; // Product ID
    private final Integer oldQuantity;
    private final Integer newQuantity;
}
