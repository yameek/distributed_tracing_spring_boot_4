package com.example.tracing.cqrs.application;

import com.example.tracing.cqrs.domain.Product;
import com.example.tracing.cqrs.domain.ProductRepository;
import com.example.tracing.cqrs.domain.commands.UpdateStockCommand;
import com.example.tracing.cqrs.domain.events.ProductStockUpdatedEvent;
import com.example.tracing.cqrs.infrastructure.command.CommandHandler;
import com.example.tracing.cqrs.infrastructure.outbox.OutboxService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for UpdateStockCommand.
 * Updates product stock and publishes ProductStockUpdatedEvent via outbox.
 */
@Slf4j
@Component
public class UpdateStockCommandHandler implements CommandHandler<UpdateStockCommand, Void> {
    
    private final ProductRepository productRepository;
    private final OutboxService outboxService;
    private final ObservationRegistry observationRegistry;
    private final Counter stockUpdatesCounter;
    
    public UpdateStockCommandHandler(
            ProductRepository productRepository,
            OutboxService outboxService,
            ObservationRegistry observationRegistry,
            MeterRegistry meterRegistry) {
        this.productRepository = productRepository;
        this.outboxService = outboxService;
        this.observationRegistry = observationRegistry;
        
        this.stockUpdatesCounter = Counter.builder("products.stock.updated")
                .description("Number of product stock updates")
                .register(meterRegistry);
    }
    
    @Override
    @Transactional
    public Void handle(UpdateStockCommand command) {
        log.info("Handling UpdateStockCommand: {}", command.getCommandId());
        
        return Observation.createNotStarted("command.handler.update.stock", observationRegistry)
                .lowCardinalityKeyValue("command.id", command.getCommandId())
                .lowCardinalityKeyValue("product.id", command.getProductId())
                .observe(() -> {
                    // Find product
                    Product product = productRepository.findById(command.getProductId())
                            .orElseThrow(() -> new ProductNotFoundException(
                                    "Product not found: " + command.getProductId()));
                    
                    Integer oldQuantity = product.getStockQuantity();
                    
                    // Update stock (domain logic)
                    product.updateStock(command.getQuantity());
                    productRepository.save(product);
                    stockUpdatesCounter.increment();
                    
                    log.info("Updated stock for product: {} from {} to {}", 
                            product.getId(), oldQuantity, command.getQuantity());
                    
                    // Create and store event in outbox
                    ProductStockUpdatedEvent event = ProductStockUpdatedEvent.builder()
                            .aggregateId(product.getId())
                            .oldQuantity(oldQuantity)
                            .newQuantity(command.getQuantity())
                            .build();
                    
                    outboxService.storeEvent(event);
                    
                    return null;
                });
    }
    
    @Override
    public Class<UpdateStockCommand> getCommandType() {
        return UpdateStockCommand.class;
    }
    
    public static class ProductNotFoundException extends RuntimeException {
        public ProductNotFoundException(String message) {
            super(message);
        }
    }
}
