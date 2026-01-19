package com.example.tracing.cqrs.messaging;

import com.example.tracing.cqrs.domain.commands.CreateProductCommand;
import com.example.tracing.cqrs.domain.commands.UpdateProductPriceCommand;
import com.example.tracing.cqrs.domain.commands.UpdateStockCommand;
import com.example.tracing.cqrs.infrastructure.command.CommandBus;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * RabbitMQ listener for incoming commands.
 * This demonstrates how commands can be received via both HTTP and RabbitMQ,
 * with trace context automatically propagated across the message boundary.
 */
@Slf4j
@Component
public class CommandMessageListener {
    
    private final CommandBus commandBus;
    private final ObjectMapper objectMapper;
    
    public CommandMessageListener(CommandBus commandBus, ObjectMapper objectMapper) {
        this.commandBus = commandBus;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Listens for commands on the RabbitMQ queue.
     * The @Observed annotation creates a span for this method.
     * Trace context is automatically extracted from RabbitMQ message headers.
     */
    @RabbitListener(queues = "cqrs.commands.queue")
    @Observed(name = "rabbitmq.command.received", contextualName = "rabbitmq-command-received")
    public void handleCommand(Map<String, Object> message) {
        try {
            String commandType = (String) message.get("commandType");
            Map<String, Object> payload = (Map<String, Object>) message.get("payload");
            
            log.info("Received command via RabbitMQ: type={}", commandType);
            
            switch (commandType) {
                case "CreateProduct":
                    handleCreateProduct(payload);
                    break;
                case "UpdatePrice":
                    handleUpdatePrice(payload);
                    break;
                case "UpdateStock":
                    handleUpdateStock(payload);
                    break;
                default:
                    log.warn("Unknown command type: {}", commandType);
            }
            
            log.info("Command processed successfully via RabbitMQ: type={}", commandType);
            
        } catch (Exception e) {
            log.error("Error processing command from RabbitMQ", e);
            throw new RuntimeException("Failed to process command", e);
        }
    }
    
    @Observed(name = "rabbitmq.create.product", contextualName = "rabbitmq-create-product")
    private void handleCreateProduct(Map<String, Object> payload) {
        CreateProductCommand command = objectMapper.convertValue(payload, CreateProductCommand.class);
        log.info("Processing CreateProduct command: name={}", command.getName());
        commandBus.dispatch(command);
    }
    
    @Observed(name = "rabbitmq.update.price", contextualName = "rabbitmq-update-price")
    private void handleUpdatePrice(Map<String, Object> payload) {
        UpdateProductPriceCommand command = objectMapper.convertValue(payload, UpdateProductPriceCommand.class);
        log.info("Processing UpdatePrice command: productId={}", command.getProductId());
        commandBus.dispatch(command);
    }
    
    @Observed(name = "rabbitmq.update.stock", contextualName = "rabbitmq-update-stock")
    private void handleUpdateStock(Map<String, Object> payload) {
        UpdateStockCommand command = objectMapper.convertValue(payload, UpdateStockCommand.class);
        log.info("Processing UpdateStock command: productId={}", command.getProductId());
        commandBus.dispatch(command);
    }
}
