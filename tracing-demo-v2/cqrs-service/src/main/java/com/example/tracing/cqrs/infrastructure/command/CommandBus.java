package com.example.tracing.cqrs.infrastructure.command;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central command bus that routes commands to their handlers.
 * Provides method-level tracing, metrics, and logging.
 */
@Slf4j
@Component
public class CommandBus {
    
    private final Map<Class<? extends Command>, CommandHandler<?, ?>> handlers = new ConcurrentHashMap<>();
    private final ObservationRegistry observationRegistry;
    private final MeterRegistry meterRegistry;
    
    private final Counter commandSuccessCounter;
    private final Counter commandFailureCounter;
    private final Timer commandExecutionTimer;
    
    public CommandBus(ObservationRegistry observationRegistry, MeterRegistry meterRegistry) {
        this.observationRegistry = observationRegistry;
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.commandSuccessCounter = Counter.builder("command.bus.success")
                .description("Number of successfully executed commands")
                .register(meterRegistry);
        
        this.commandFailureCounter = Counter.builder("command.bus.failure")
                .description("Number of failed command executions")
                .register(meterRegistry);
        
        this.commandExecutionTimer = Timer.builder("command.bus.execution.time")
                .description("Time taken to execute commands")
                .register(meterRegistry);
    }
    
    /**
     * Registers a command handler with the bus.
     */
    public <T extends Command, R> void registerHandler(CommandHandler<T, R> handler) {
        Class<T> commandType = handler.getCommandType();
        if (handlers.containsKey(commandType)) {
            throw new IllegalStateException("Handler already registered for command: " + commandType.getName());
        }
        handlers.put(commandType, handler);
        log.info("Registered command handler: {} for command: {}", 
                handler.getClass().getSimpleName(), commandType.getSimpleName());
    }
    
    /**
     * Dispatches a command to its handler with full tracing and metrics.
     */
    @SuppressWarnings("unchecked")
    public <T extends Command, R> R dispatch(T command) {
        String commandName = command.getClass().getSimpleName();
        String commandId = command.getCommandId();
        
        log.info("Dispatching command: {} with ID: {}", commandName, commandId);
        
        // Create observation for tracing
        return Observation.createNotStarted("command.bus.dispatch", observationRegistry)
                .lowCardinalityKeyValue("command.type", commandName)
                .lowCardinalityKeyValue("command.id", commandId)
                .observe(() -> {
                    try {
                        // Find handler
                        CommandHandler<T, R> handler = (CommandHandler<T, R>) handlers.get(command.getClass());
                        
                        if (handler == null) {
                            String errorMsg = "No handler registered for command: " + commandName;
                            log.error(errorMsg);
                            commandFailureCounter.increment();
                            throw new IllegalStateException(errorMsg);
                        }
                        
                        // Execute handler with timing
                        R result = commandExecutionTimer.record(() -> {
                            log.debug("Executing command handler: {} for command: {}", 
                                    handler.getClass().getSimpleName(), commandName);
                            return handler.handle(command);
                        });
                        
                        commandSuccessCounter.increment();
                        log.info("Successfully executed command: {} with ID: {}", commandName, commandId);
                        
                        return result;
                        
                    } catch (Exception e) {
                        commandFailureCounter.increment();
                        log.error("Failed to execute command: {} with ID: {}", commandName, commandId, e);
                        throw new CommandExecutionException("Failed to execute command: " + commandName, e);
                    }
                });
    }
    
    /**
     * Exception thrown when command execution fails.
     */
    public static class CommandExecutionException extends RuntimeException {
        public CommandExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
