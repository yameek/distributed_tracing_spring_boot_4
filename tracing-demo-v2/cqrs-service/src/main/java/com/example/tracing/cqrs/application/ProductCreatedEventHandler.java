package com.example.tracing.cqrs.application;

import com.example.tracing.cqrs.domain.events.ProductCreatedEvent;
import com.example.tracing.cqrs.infrastructure.event.EventHandler;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handler for ProductCreatedEvent.
 * Example: Could send notifications, update search indexes, etc.
 */
@Slf4j
@Component
public class ProductCreatedEventHandler implements EventHandler<ProductCreatedEvent> {
    
    private final ObservationRegistry observationRegistry;
    private final Counter eventsHandledCounter;
    
    public ProductCreatedEventHandler(
            ObservationRegistry observationRegistry,
            MeterRegistry meterRegistry) {
        this.observationRegistry = observationRegistry;
        
        this.eventsHandledCounter = Counter.builder("events.product.created.handled")
                .description("Number of ProductCreatedEvent handled")
                .register(meterRegistry);
    }
    
    @Override
    public void handle(ProductCreatedEvent event) {
        log.info("Handling ProductCreatedEvent: {} for product: {}", 
                event.getEventId(), event.getAggregateId());
        
        Observation.createNotStarted("event.handler.product.created", observationRegistry)
                .lowCardinalityKeyValue("event.id", event.getEventId())
                .lowCardinalityKeyValue("aggregate.id", event.getAggregateId())
                .observe(() -> {
                    // Example business logic
                    log.info("Product created: {} - {} (Price: {}, Stock: {})",
                            event.getAggregateId(),
                            event.getName(),
                            event.getPrice(),
                            event.getInitialStock());
                    
                    // Could trigger:
                    // - Send notification to admin
                    // - Update search index
                    // - Sync with external systems
                    // - Update analytics
                    
                    eventsHandledCounter.increment();
                    
                    log.info("Successfully processed ProductCreatedEvent: {}", event.getEventId());
                });
    }
    
    @Override
    public Class<ProductCreatedEvent> getEventType() {
        return ProductCreatedEvent.class;
    }
}
