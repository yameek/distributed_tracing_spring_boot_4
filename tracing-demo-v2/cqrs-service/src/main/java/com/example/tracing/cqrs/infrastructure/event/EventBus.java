package com.example.tracing.cqrs.infrastructure.event;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central event bus that publishes events to all registered handlers.
 * Provides method-level tracing, metrics, and logging.
 * Supports multiple handlers per event type.
 */
@Slf4j
@Component
public class EventBus {
    
    private final Map<Class<? extends DomainEvent>, List<EventHandler<?>>> handlers = new ConcurrentHashMap<>();
    private final ObservationRegistry observationRegistry;
    private final MeterRegistry meterRegistry;
    
    private final Counter eventPublishedCounter;
    private final Counter eventHandledCounter;
    private final Counter eventHandlingFailureCounter;
    private final Timer eventHandlingTimer;
    
    public EventBus(ObservationRegistry observationRegistry, MeterRegistry meterRegistry) {
        this.observationRegistry = observationRegistry;
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.eventPublishedCounter = Counter.builder("event.bus.published")
                .description("Number of events published")
                .register(meterRegistry);
        
        this.eventHandledCounter = Counter.builder("event.bus.handled")
                .description("Number of events successfully handled")
                .register(meterRegistry);
        
        this.eventHandlingFailureCounter = Counter.builder("event.bus.handling.failure")
                .description("Number of event handling failures")
                .register(meterRegistry);
        
        this.eventHandlingTimer = Timer.builder("event.bus.handling.time")
                .description("Time taken to handle events")
                .register(meterRegistry);
    }
    
    /**
     * Registers an event handler with the bus.
     * Multiple handlers can be registered for the same event type.
     */
    public <T extends DomainEvent> void registerHandler(EventHandler<T> handler) {
        Class<T> eventType = handler.getEventType();
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
        log.info("Registered event handler: {} for event: {}", 
                handler.getHandlerName(), eventType.getSimpleName());
    }
    
    /**
     * Publishes an event to all registered handlers with full tracing and metrics.
     */
    @SuppressWarnings("unchecked")
    public <T extends DomainEvent> void publish(T event) {
        String eventType = event.getEventType();
        String eventId = event.getEventId();
        String aggregateId = event.getAggregateId();
        
        log.info("Publishing event: {} with ID: {} for aggregate: {}", eventType, eventId, aggregateId);
        eventPublishedCounter.increment();
        
        // Create observation for tracing
        Observation.createNotStarted("event.bus.publish", observationRegistry)
                .lowCardinalityKeyValue("event.type", eventType)
                .lowCardinalityKeyValue("event.id", eventId)
                .lowCardinalityKeyValue("aggregate.id", aggregateId)
                .observe(() -> {
                    List<EventHandler<?>> eventHandlers = handlers.get(event.getClass());
                    
                    if (eventHandlers == null || eventHandlers.isEmpty()) {
                        log.warn("No handlers registered for event: {}", eventType);
                        return;
                    }
                    
                    log.debug("Found {} handler(s) for event: {}", eventHandlers.size(), eventType);
                    
                    // Dispatch to all handlers
                    for (EventHandler<?> handler : eventHandlers) {
                        dispatchToHandler((EventHandler<T>) handler, event);
                    }
                });
    }
    
    /**
     * Dispatches an event to a specific handler with tracing and error handling.
     */
    private <T extends DomainEvent> void dispatchToHandler(EventHandler<T> handler, T event) {
        String handlerName = handler.getHandlerName();
        String eventType = event.getEventType();
        String eventId = event.getEventId();
        
        // Create child observation for each handler
        Observation.createNotStarted("event.handler.execute", observationRegistry)
                .lowCardinalityKeyValue("handler.name", handlerName)
                .lowCardinalityKeyValue("event.type", eventType)
                .lowCardinalityKeyValue("event.id", eventId)
                .observe(() -> {
                    try {
                        log.debug("Dispatching event: {} to handler: {}", eventType, handlerName);
                        
                        eventHandlingTimer.record(() -> {
                            handler.handle(event);
                        });
                        
                        eventHandledCounter.increment();
                        log.info("Successfully handled event: {} by handler: {}", eventType, handlerName);
                        
                    } catch (Exception e) {
                        eventHandlingFailureCounter.increment();
                        log.error("Failed to handle event: {} with handler: {}", eventType, handlerName, e);
                        // Continue to next handler even if this one fails
                    }
                });
    }
    
    /**
     * Returns the number of handlers registered for a given event type.
     */
    public int getHandlerCount(Class<? extends DomainEvent> eventType) {
        List<EventHandler<?>> eventHandlers = handlers.get(eventType);
        return eventHandlers != null ? eventHandlers.size() : 0;
    }
}
