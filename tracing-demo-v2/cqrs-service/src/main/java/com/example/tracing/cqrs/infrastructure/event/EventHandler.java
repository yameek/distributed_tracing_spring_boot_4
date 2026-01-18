package com.example.tracing.cqrs.infrastructure.event;

/**
 * Interface for event handlers.
 * Multiple handlers can subscribe to the same event type.
 *
 * @param <T> The type of event this handler processes
 */
public interface EventHandler<T extends DomainEvent> {
    /**
     * Handles the given event.
     *
     * @param event The event to handle
     */
    void handle(T event);
    
    /**
     * Returns the type of event this handler can process.
     */
    Class<T> getEventType();
    
    /**
     * Returns the name of this handler for logging/tracing purposes.
     */
    default String getHandlerName() {
        return this.getClass().getSimpleName();
    }
}
