package com.example.tracing.cqrs.infrastructure.outbox;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing an event in the outbox table.
 * Events are stored here before being published to the message broker.
 * This ensures transactional consistency between domain changes and event publishing.
 */
@Entity
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_outbox_status_created", columnList = "status,createdAt"),
    @Index(name = "idx_outbox_aggregate", columnList = "aggregateId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String eventId;
    
    @Column(nullable = false)
    private String eventType;
    
    @Column(nullable = false)
    private String aggregateId;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDING;
    
    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    private Instant processedAt;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer retryCount = 0;
    
    private String errorMessage;
    
    @Version
    private Long version;
    
    public enum OutboxStatus {
        PENDING,    // Not yet processed
        PROCESSING, // Currently being processed
        PUBLISHED,  // Successfully published
        FAILED      // Failed after max retries
    }
}
