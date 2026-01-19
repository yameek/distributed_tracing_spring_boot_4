package com.example.tracing.cqrs.infrastructure.outbox;

import com.example.tracing.cqrs.infrastructure.event.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing outbox events.
 * Provides transactional event storage with tracing and metrics.
 */
@Slf4j
@Service
public class OutboxService {
    
    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final Counter eventStoredCounter;
    private final Counter eventStorageFailureCounter;
    
    public OutboxService(
            OutboxEventRepository repository,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        
        this.eventStoredCounter = Counter.builder("outbox.event.stored")
                .description("Number of events stored in outbox")
                .register(meterRegistry);
        
        this.eventStorageFailureCounter = Counter.builder("outbox.event.storage.failure")
                .description("Number of failed event storage attempts")
                .register(meterRegistry);
    }
    
    /**
     * Stores a domain event in the outbox table.
     * This method should be called within the same transaction as the domain changes.
     */
    @Transactional
    @Observed(name = "outbox.store", contextualName = "outbox-store-event")
    public void storeEvent(DomainEvent event) {
        String eventType = event.getEventType();
        String eventId = event.getEventId();
        
        log.info("Storing event in outbox: {} with ID: {}", eventType, eventId);
        
        try {
            String payload = objectMapper.writeValueAsString(event);
            
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(eventId)
                    .eventType(eventType)
                    .aggregateId(event.getAggregateId())
                    .payload(payload)
                    .status(OutboxEvent.OutboxStatus.PENDING)
                    .build();
            
            repository.save(outboxEvent);
            eventStoredCounter.increment();
            
            log.info("Successfully stored event in outbox: {} with ID: {}", eventType, eventId);
            
        } catch (Exception e) {
            eventStorageFailureCounter.increment();
            log.error("Failed to store event in outbox: {} with ID: {}", eventType, eventId, e);
            throw new OutboxStorageException("Failed to store event in outbox", e);
        }
    }
    
    /**
     * Marks an event as published.
     */
    @Transactional
    public void markAsPublished(String outboxEventId) {
        repository.findById(outboxEventId).ifPresent(event -> {
            event.setStatus(OutboxEvent.OutboxStatus.PUBLISHED);
            event.setProcessedAt(java.time.Instant.now());
            repository.save(event);
            log.debug("Marked outbox event as published: {}", outboxEventId);
        });
    }
    
    /**
     * Marks an event as failed.
     */
    @Transactional
    public void markAsFailed(String outboxEventId, String errorMessage) {
        repository.findById(outboxEventId).ifPresent(event -> {
            event.setStatus(OutboxEvent.OutboxStatus.FAILED);
            event.setErrorMessage(errorMessage);
            event.setRetryCount(event.getRetryCount() + 1);
            repository.save(event);
            log.warn("Marked outbox event as failed: {} - {}", outboxEventId, errorMessage);
        });
    }
    
    /**
     * Exception thrown when event storage fails.
     */
    public static class OutboxStorageException extends RuntimeException {
        public OutboxStorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
