package com.example.tracing.cqrs.application;

import com.example.tracing.cqrs.domain.Product;
import com.example.tracing.cqrs.domain.ProductRepository;
import com.example.tracing.cqrs.domain.commands.CreateProductCommand;
import com.example.tracing.cqrs.domain.events.ProductCreatedEvent;
import com.example.tracing.cqrs.infrastructure.command.CommandHandler;
import com.example.tracing.cqrs.infrastructure.outbox.OutboxService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for CreateProductCommand.
 * Creates a new product and publishes ProductCreatedEvent via outbox.
 */
@Slf4j
@Component
public class CreateProductCommandHandler implements CommandHandler<CreateProductCommand, String> {
    
    private final ProductRepository productRepository;
    private final OutboxService outboxService;
    private final ObservationRegistry observationRegistry;
    private final Counter productsCreatedCounter;
    
    public CreateProductCommandHandler(
            ProductRepository productRepository,
            OutboxService outboxService,
            ObservationRegistry observationRegistry,
            MeterRegistry meterRegistry) {
        this.productRepository = productRepository;
        this.outboxService = outboxService;
        this.observationRegistry = observationRegistry;
        
        this.productsCreatedCounter = Counter.builder("products.created")
                .description("Number of products created")
                .register(meterRegistry);
    }
    
    @Override
    @Transactional
    public String handle(CreateProductCommand command) {
        log.info("Handling CreateProductCommand: {}", command.getCommandId());
        
        return Observation.createNotStarted("command.handler.create.product", observationRegistry)
                .lowCardinalityKeyValue("command.id", command.getCommandId())
                .observe(() -> {
                    // Validate
                    if (command.getPrice().signum() <= 0) {
                        throw new IllegalArgumentException("Price must be positive");
                    }
                    if (command.getInitialStock() < 0) {
                        throw new IllegalArgumentException("Stock cannot be negative");
                    }
                    
                    // Create product
                    Product product = Product.builder()
                            .name(command.getName())
                            .description(command.getDescription())
                            .price(command.getPrice())
                            .stockQuantity(command.getInitialStock())
                            .build();
                    
                    product = productRepository.save(product);
                    productsCreatedCounter.increment();
                    
                    log.info("Created product with ID: {}", product.getId());
                    
                    // Create and store event in outbox (same transaction)
                    ProductCreatedEvent event = ProductCreatedEvent.builder()
                            .aggregateId(product.getId())
                            .name(product.getName())
                            .description(product.getDescription())
                            .price(product.getPrice())
                            .initialStock(product.getStockQuantity())
                            .build();
                    
                    outboxService.storeEvent(event);
                    
                    return product.getId();
                });
    }
    
    @Override
    public Class<CreateProductCommand> getCommandType() {
        return CreateProductCommand.class;
    }
}
