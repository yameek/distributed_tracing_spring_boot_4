package com.example.tracing.cqrs.application;

import com.example.tracing.cqrs.domain.events.ProductStockUpdatedEvent;
import com.example.tracing.cqrs.infrastructure.event.EventHandler;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handler for ProductStockUpdatedEvent.
 * Example: Could trigger low stock alerts, update inventory systems, etc.
 */
@Slf4j
@Component
public class ProductStockUpdatedEventHandler implements EventHandler<ProductStockUpdatedEvent> {
    
    private final ObservationRegistry observationRegistry;
    private final Counter eventsHandledCounter;
    private final Counter lowStockAlertsCounter;
    
    public ProductStockUpdatedEventHandler(
            ObservationRegistry observationRegistry,
            MeterRegistry meterRegistry) {
        this.observationRegistry = observationRegistry;
        
        this.eventsHandledCounter = Counter.builder("events.product.stock.updated.handled")
                .description("Number of ProductStockUpdatedEvent handled")
                .register(meterRegistry);
        
        this.lowStockAlertsCounter = Counter.builder("alerts.low.stock")
                .description("Number of low stock alerts triggered")
                .register(meterRegistry);
    }
    
    @Override
    public void handle(ProductStockUpdatedEvent event) {
        log.info("Handling ProductStockUpdatedEvent: {} for product: {}", 
                event.getEventId(), event.getAggregateId());
        
        Observation.createNotStarted("event.handler.product.stock.updated", observationRegistry)
                .lowCardinalityKeyValue("event.id", event.getEventId())
                .lowCardinalityKeyValue("aggregate.id", event.getAggregateId())
                .observe(() -> {
                    // Example business logic
                    log.info("Product stock updated: {} - Old: {}, New: {}",
                            event.getAggregateId(),
                            event.getOldQuantity(),
                            event.getNewQuantity());
                    
                    // Check for low stock
                    if (event.getNewQuantity() < 10 && event.getNewQuantity() > 0) {
                        log.warn("Low stock alert for product: {} - Only {} items remaining",
                                event.getAggregateId(), event.getNewQuantity());
                        lowStockAlertsCounter.increment();
                    }
                    
                    // Check for out of stock
                    if (event.getNewQuantity() == 0) {
                        log.warn("Out of stock alert for product: {}", event.getAggregateId());
                    }
                    
                    // Could trigger:
                    // - Send low stock notification
                    // - Trigger reorder process
                    // - Update inventory dashboard
                    // - Sync with warehouse system
                    
                    eventsHandledCounter.increment();
                    
                    log.info("Successfully processed ProductStockUpdatedEvent: {}", event.getEventId());
                });
    }
    
    @Override
    public Class<ProductStockUpdatedEvent> getEventType() {
        return ProductStockUpdatedEvent.class;
    }
}
