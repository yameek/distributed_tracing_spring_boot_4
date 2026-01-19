package com.example.tracing.cqrs.application;

import com.example.tracing.cqrs.domain.events.ProductPriceUpdatedEvent;
import com.example.tracing.cqrs.infrastructure.event.EventHandler;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handler for ProductPriceUpdatedEvent.
 * Example: Could trigger price alerts, update caches, etc.
 */
@Slf4j
@Component
public class ProductPriceUpdatedEventHandler implements EventHandler<ProductPriceUpdatedEvent> {
    
    private final ObservationRegistry observationRegistry;
    private final Counter eventsHandledCounter;
    
    public ProductPriceUpdatedEventHandler(
            ObservationRegistry observationRegistry,
            MeterRegistry meterRegistry) {
        this.observationRegistry = observationRegistry;
        
        this.eventsHandledCounter = Counter.builder("events.product.price.updated.handled")
                .description("Number of ProductPriceUpdatedEvent handled")
                .register(meterRegistry);
    }
    
    @Override
    public void handle(ProductPriceUpdatedEvent event) {
        log.info("Handling ProductPriceUpdatedEvent: {} for product: {}", 
                event.getEventId(), event.getAggregateId());
        
        Observation.createNotStarted("event.handler.product.price.updated", observationRegistry)
                .lowCardinalityKeyValue("event.id", event.getEventId())
                .lowCardinalityKeyValue("aggregate.id", event.getAggregateId())
                .observe(() -> {
                    // Example business logic
                    log.info("Product price updated: {} - Old: {}, New: {}",
                            event.getAggregateId(),
                            event.getOldPrice(),
                            event.getNewPrice());
                    
                    // Could trigger:
                    // - Send price alert to subscribers
                    // - Update price history
                    // - Invalidate cache
                    // - Notify pricing service
                    
                    eventsHandledCounter.increment();
                    
                    log.info("Successfully processed ProductPriceUpdatedEvent: {}", event.getEventId());
                });
    }
    
    @Override
    public Class<ProductPriceUpdatedEvent> getEventType() {
        return ProductPriceUpdatedEvent.class;
    }
}
