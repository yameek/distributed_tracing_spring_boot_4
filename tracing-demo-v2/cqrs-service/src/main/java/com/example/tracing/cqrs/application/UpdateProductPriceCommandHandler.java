package com.example.tracing.cqrs.application;

import com.example.tracing.cqrs.domain.Product;
import com.example.tracing.cqrs.domain.ProductRepository;
import com.example.tracing.cqrs.domain.commands.UpdateProductPriceCommand;
import com.example.tracing.cqrs.domain.events.ProductPriceUpdatedEvent;
import com.example.tracing.cqrs.infrastructure.command.CommandHandler;
import com.example.tracing.cqrs.infrastructure.outbox.OutboxService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Handler for UpdateProductPriceCommand.
 * Updates product price and publishes ProductPriceUpdatedEvent via outbox.
 */
@Slf4j
@Component
public class UpdateProductPriceCommandHandler implements CommandHandler<UpdateProductPriceCommand, Void> {
    
    private final ProductRepository productRepository;
    private final OutboxService outboxService;
    private final ObservationRegistry observationRegistry;
    private final Counter priceUpdatesCounter;
    
    public UpdateProductPriceCommandHandler(
            ProductRepository productRepository,
            OutboxService outboxService,
            ObservationRegistry observationRegistry,
            MeterRegistry meterRegistry) {
        this.productRepository = productRepository;
        this.outboxService = outboxService;
        this.observationRegistry = observationRegistry;
        
        this.priceUpdatesCounter = Counter.builder("products.price.updated")
                .description("Number of product price updates")
                .register(meterRegistry);
    }
    
    @Override
    @Transactional
    public Void handle(UpdateProductPriceCommand command) {
        log.info("Handling UpdateProductPriceCommand: {}", command.getCommandId());
        
        return Observation.createNotStarted("command.handler.update.price", observationRegistry)
                .lowCardinalityKeyValue("command.id", command.getCommandId())
                .lowCardinalityKeyValue("product.id", command.getProductId())
                .observe(() -> {
                    // Find product
                    Product product = productRepository.findById(command.getProductId())
                            .orElseThrow(() -> new ProductNotFoundException(
                                    "Product not found: " + command.getProductId()));
                    
                    BigDecimal oldPrice = product.getPrice();
                    
                    // Update price (domain logic)
                    product.updatePrice(command.getNewPrice());
                    productRepository.save(product);
                    priceUpdatesCounter.increment();
                    
                    log.info("Updated price for product: {} from {} to {}", 
                            product.getId(), oldPrice, command.getNewPrice());
                    
                    // Create and store event in outbox
                    ProductPriceUpdatedEvent event = ProductPriceUpdatedEvent.builder()
                            .aggregateId(product.getId())
                            .oldPrice(oldPrice)
                            .newPrice(command.getNewPrice())
                            .build();
                    
                    outboxService.storeEvent(event);
                    
                    return null;
                });
    }
    
    @Override
    public Class<UpdateProductPriceCommand> getCommandType() {
        return UpdateProductPriceCommand.class;
    }
    
    public static class ProductNotFoundException extends RuntimeException {
        public ProductNotFoundException(String message) {
            super(message);
        }
    }
}
