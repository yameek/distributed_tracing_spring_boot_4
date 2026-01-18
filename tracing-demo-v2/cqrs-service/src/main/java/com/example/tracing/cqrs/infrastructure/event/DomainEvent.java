package com.example.tracing.cqrs.infrastructure.event;

import java.time.Instant;

/**
 * Base interface for all domain events.
 * Events represent things that have happened in the system.
 */
public interface DomainEvent {
    /**
     * Returns a unique identifier for this event instance.
     */
    String getEventId();
    
    /**
     * Returns the timestamp when this event occurred.
     */
    Instant getOccurredAt();
    
    /**
     * Returns the aggregate ID this event relates to.
     */
    String getAggregateId();
    
    /**
     * Returns the type name of this event.
     */
    default String getEventType() {
        return this.getClass().getSimpleName();
    }
}
