package com.example.tracing.cqrs.infrastructure.query;

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
 * Central query bus that routes queries to their handlers.
 * Provides method-level tracing and metrics.
 */
@Slf4j
@Component
public class QueryBus {
    
    private final Map<Class<? extends Query<?>>, QueryHandler<?, ?>> handlers = new ConcurrentHashMap<>();
    private final ObservationRegistry observationRegistry;
    private final MeterRegistry meterRegistry;
    
    private final Counter querySuccessCounter;
    private final Counter queryFailureCounter;
    private final Timer queryExecutionTimer;
    
    public QueryBus(ObservationRegistry observationRegistry, MeterRegistry meterRegistry) {
        this.observationRegistry = observationRegistry;
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.querySuccessCounter = Counter.builder("query.bus.success")
                .description("Number of successfully executed queries")
                .register(meterRegistry);
        
        this.queryFailureCounter = Counter.builder("query.bus.failure")
                .description("Number of failed query executions")
                .register(meterRegistry);
        
        this.queryExecutionTimer = Timer.builder("query.bus.execution.time")
                .description("Time taken to execute queries")
                .register(meterRegistry);
    }
    
    /**
     * Registers a query handler with the bus.
     */
    public <T extends Query<R>, R> void registerHandler(QueryHandler<T, R> handler) {
        Class<T> queryType = handler.getQueryType();
        if (handlers.containsKey(queryType)) {
            throw new IllegalStateException("Handler already registered for query: " + queryType.getName());
        }
        handlers.put(queryType, handler);
        log.info("Registered query handler: {} for query: {}", 
                handler.getClass().getSimpleName(), queryType.getSimpleName());
    }
    
    /**
     * Dispatches a query to its handler with full tracing and metrics.
     */
    @SuppressWarnings("unchecked")
    public <T extends Query<R>, R> R dispatch(T query) {
        String queryName = query.getClass().getSimpleName();
        String queryId = query.getQueryId();
        
        log.info("Dispatching query: {} with ID: {}", queryName, queryId);
        
        // Create observation for tracing
        return Observation.createNotStarted("query.bus.dispatch", observationRegistry)
                .lowCardinalityKeyValue("query.type", queryName)
                .lowCardinalityKeyValue("query.id", queryId)
                .observe(() -> {
                    try {
                        // Find handler
                        QueryHandler<T, R> handler = (QueryHandler<T, R>) handlers.get(query.getClass());
                        
                        if (handler == null) {
                            String errorMsg = "No handler registered for query: " + queryName;
                            log.error(errorMsg);
                            queryFailureCounter.increment();
                            throw new IllegalStateException(errorMsg);
                        }
                        
                        // Execute handler with timing
                        R result = queryExecutionTimer.record(() -> {
                            log.debug("Executing query handler: {} for query: {}", 
                                    handler.getClass().getSimpleName(), queryName);
                            return handler.handle(query);
                        });
                        
                        querySuccessCounter.increment();
                        log.info("Successfully executed query: {} with ID: {}", queryName, queryId);
                        
                        return result;
                        
                    } catch (Exception e) {
                        queryFailureCounter.increment();
                        log.error("Failed to execute query: {} with ID: {}", queryName, queryId, e);
                        throw new QueryExecutionException("Failed to execute query: " + queryName, e);
                    }
                });
    }
    
    /**
     * Exception thrown when query execution fails.
     */
    public static class QueryExecutionException extends RuntimeException {
        public QueryExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
