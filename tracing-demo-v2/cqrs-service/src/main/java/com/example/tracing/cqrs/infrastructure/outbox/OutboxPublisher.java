package com.example.tracing.cqrs.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Polls the outbox table and publishes pending events to RabbitMQ.
 * Provides reliable event delivery with retry logic and tracing.
 */
@Slf4j
@Component
public class OutboxPublisher {
    
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_CUTOFF_HOURS = 24;
    private static final String EXCHANGE_NAME = "cqrs.events.exchange";
    
    private final OutboxEventRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final OutboxService outboxService;
    private final Counter eventsPublishedCounter;
    private final Counter eventsFailedCounter;
    
    public OutboxPublisher(
            OutboxEventRepository repository,
            RabbitTemplate rabbitTemplate,
            OutboxService outboxService,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.outboxService = outboxService;
        
        this.eventsPublishedCounter = Counter.builder("outbox.events.published")
                .description("Number of events published from outbox")
                .register(meterRegistry);
        
        this.eventsFailedCounter = Counter.builder("outbox.events.failed")
                .description("Number of events that failed to publish")
                .register(meterRegistry);
    }
    
    /**
     * Polls for pending events and publishes them.
     * Runs every 5 seconds.
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    @Transactional
    @Observed(name = "outbox.poll", contextualName = "outbox-poll-pending")
    public void publishPendingEvents() {
        log.debug("Polling outbox for pending events");
        
        List<OutboxEvent> pendingEvents = repository.findPendingEventsForProcessing();
        
        if (!pendingEvents.isEmpty()) {
            log.info("Found {} pending events to publish", pendingEvents.size());
            
            for (OutboxEvent event : pendingEvents) {
                publishEvent(event);
            }
        }
    }
    
    /**
     * Retries failed events that haven't exceeded max retries.
     * Runs every minute.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    @Transactional
    public void retryFailedEvents() {
        log.debug("Checking for failed events to retry");
        
        Instant cutoffTime = Instant.now().minus(RETRY_CUTOFF_HOURS, ChronoUnit.HOURS);
        List<OutboxEvent> failedEvents = repository.findFailedEventsForRetry(MAX_RETRIES, cutoffTime);
        
        if (!failedEvents.isEmpty()) {
            log.info("Found {} failed events to retry", failedEvents.size());
            
            for (OutboxEvent event : failedEvents) {
                publishEvent(event);
            }
        }
    }
    
    /**
     * Publishes a single event to RabbitMQ.
     */
    @Observed(name = "outbox.publish", contextualName = "outbox-publish-event")
    private void publishEvent(OutboxEvent event) {
        String eventType = event.getEventType();
        String eventId = event.getEventId();
        
        log.info("Publishing event from outbox: {} with ID: {}", eventType, eventId);
        
        try {
            // Mark as processing
            event.setStatus(OutboxEvent.OutboxStatus.PROCESSING);
            repository.save(event);
            
            // Publish to RabbitMQ
            String routingKey = "event." + eventType.toLowerCase();
            rabbitTemplate.convertAndSend(EXCHANGE_NAME, routingKey, event.getPayload());
            
            // Mark as published
            outboxService.markAsPublished(event.getId());
            eventsPublishedCounter.increment();
            
            log.info("Successfully published event from outbox: {} with ID: {}", eventType, eventId);
            
        } catch (Exception e) {
            eventsFailedCounter.increment();
            String errorMessage = "Failed to publish event: " + e.getMessage();
            outboxService.markAsFailed(event.getId(), errorMessage);
            log.error("Failed to publish event from outbox: {} with ID: {}", eventType, eventId, e);
        }
    }
    
    /**
     * Logs outbox statistics.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
    public void logStatistics() {
        long pending = repository.countByStatus(OutboxEvent.OutboxStatus.PENDING);
        long processing = repository.countByStatus(OutboxEvent.OutboxStatus.PROCESSING);
        long published = repository.countByStatus(OutboxEvent.OutboxStatus.PUBLISHED);
        long failed = repository.countByStatus(OutboxEvent.OutboxStatus.FAILED);
        
        log.info("Outbox statistics - Pending: {}, Processing: {}, Published: {}, Failed: {}", 
                pending, processing, published, failed);
    }
}
