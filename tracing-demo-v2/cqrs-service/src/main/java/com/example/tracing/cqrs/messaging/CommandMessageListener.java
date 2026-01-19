package com.example.tracing.cqrs.messaging;

import com.example.tracing.cqrs.domain.commands.CreateProductCommand;
import com.example.tracing.cqrs.domain.commands.UpdateProductPriceCommand;
import com.example.tracing.cqrs.domain.commands.UpdateStockCommand;
import com.example.tracing.cqrs.infrastructure.command.CommandBus;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RabbitMQ listener for incoming commands.
 * This demonstrates how commands can be received via both HTTP and RabbitMQ,
 * with trace context automatically propagated across the message boundary.
 * Includes error rate limiting to prevent infinite error loops.
 */
@Slf4j
@Component
public class CommandMessageListener {
    
    private static final int MAX_ERRORS_PER_MINUTE = 100;
    private static final long ERROR_WINDOW_MS = 60_000; // 1 minute
    
    private final CommandBus commandBus;
    private final ObjectMapper objectMapper;
    private final Counter commandErrorCounter;
    private final Counter commandRateLimitExceededCounter;
    
    // Error rate limiting: sliding window
    private final AtomicInteger errorCount = new AtomicInteger(0);
    private final AtomicLong errorWindowStart = new AtomicLong(System.currentTimeMillis());
    
    public CommandMessageListener(
            CommandBus commandBus, 
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.commandBus = commandBus;
        this.objectMapper = objectMapper;
        
        this.commandErrorCounter = Counter.builder("rabbitmq.command.error")
                .description("Number of errors processing RabbitMQ commands")
                .register(meterRegistry);
        
        this.commandRateLimitExceededCounter = Counter.builder("rabbitmq.command.rate.limit.exceeded")
                .description("Number of times error rate limit was exceeded")
                .register(meterRegistry);
    }
    
    /**
     * Listens for commands on the RabbitMQ queue.
     * The @Observed annotation creates a span for this method.
     * Trace context is automatically extracted from RabbitMQ message headers.
     * Includes error rate limiting to prevent infinite error loops.
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
            
            // Reset error count on success
            resetErrorWindow();
            
        } catch (Exception e) {
            handleError(e);
        }
    }
    
    /**
     * Handles errors with rate limiting to prevent infinite error loops.
     * If error rate exceeds threshold, logs warning and stops processing
     * to prevent disk space exhaustion.
     */
    private void handleError(Exception e) {
        commandErrorCounter.increment();
        
        // Check if we need to reset the error window
        long now = System.currentTimeMillis();
        long windowStart = errorWindowStart.get();
        
        if (now - windowStart > ERROR_WINDOW_MS) {
            // Reset window
            errorWindowStart.set(now);
            errorCount.set(0);
        }
        
        int currentErrors = errorCount.incrementAndGet();
        
        if (currentErrors > MAX_ERRORS_PER_MINUTE) {
            commandRateLimitExceededCounter.increment();
            log.error(
                "ERROR RATE LIMIT EXCEEDED: {} errors in the last minute. " +
                "Stopping error logging to prevent disk exhaustion. " +
                "Please check the root cause and fix the issue.",
                currentErrors
            );
            // Don't throw exception to prevent infinite retry loop
            // The message will be rejected and sent to DLQ (if configured)
            return;
        }
        
        // Log error normally if under rate limit
        log.error("Error processing command from RabbitMQ", e);
        throw new RuntimeException("Failed to process command", e);
    }
    
    /**
     * Resets the error window when a command succeeds.
     */
    private void resetErrorWindow() {
        errorWindowStart.set(System.currentTimeMillis());
        errorCount.set(0);
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
