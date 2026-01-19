package com.example.tracing.cqrs;

import com.example.tracing.cqrs.application.*;
import com.example.tracing.cqrs.infrastructure.command.CommandBus;
import com.example.tracing.cqrs.infrastructure.event.EventBus;
import com.example.tracing.cqrs.infrastructure.query.QueryBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for CQRS Service.
 * Demonstrates CQRS pattern with Command Bus, Event Bus, and Outbox pattern.
 */
@Slf4j
@SpringBootApplication
@EnableScheduling
public class CqrsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CqrsServiceApplication.class, args);
    }
    
    /**
     * Registers all command handlers with the command bus.
     */
    @Bean
    public CommandLineRunner registerCommandHandlers(
            CommandBus commandBus,
            CreateProductCommandHandler createProductHandler,
            UpdateProductPriceCommandHandler updatePriceHandler,
            UpdateStockCommandHandler updateStockHandler) {
        
        return args -> {
            log.info("Registering command handlers...");
            commandBus.registerHandler(createProductHandler);
            commandBus.registerHandler(updatePriceHandler);
            commandBus.registerHandler(updateStockHandler);
            log.info("Command handlers registered successfully");
        };
    }
    
    /**
     * Registers all event handlers with the event bus.
     */
    @Bean
    public CommandLineRunner registerEventHandlers(
            EventBus eventBus,
            ProductCreatedEventHandler productCreatedHandler,
            ProductPriceUpdatedEventHandler priceUpdatedHandler,
            ProductStockUpdatedEventHandler stockUpdatedHandler) {
        
        return args -> {
            log.info("Registering event handlers...");
            eventBus.registerHandler(productCreatedHandler);
            eventBus.registerHandler(priceUpdatedHandler);
            eventBus.registerHandler(stockUpdatedHandler);
            log.info("Event handlers registered successfully");
        };
    }
    
    /**
     * Registers all query handlers with the query bus.
     */
    @Bean
    public CommandLineRunner registerQueryHandlers(
            QueryBus queryBus,
            GetProductByIdQueryHandler getProductByIdHandler,
            GetAllProductsQueryHandler getAllProductsHandler) {
        
        return args -> {
            log.info("Registering query handlers...");
            queryBus.registerHandler(getProductByIdHandler);
            queryBus.registerHandler(getAllProductsHandler);
            log.info("Query handlers registered successfully");
        };
    }
}
