package com.example.tracing.cqrs.infrastructure.outbox;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

/**
 * Repository for managing outbox events.
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    
    /**
     * Finds pending events that need to be processed.
     * Uses pessimistic locking to prevent concurrent processing.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'PENDING' " +
           "ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingEventsForProcessing();
    
    /**
     * Finds events that failed but can be retried.
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'FAILED' " +
           "AND e.retryCount < :maxRetries " +
           "AND e.createdAt > :cutoffTime " +
           "ORDER BY e.createdAt ASC")
    List<OutboxEvent> findFailedEventsForRetry(int maxRetries, Instant cutoffTime);
    
    /**
     * Counts events by status.
     */
    long countByStatus(OutboxEvent.OutboxStatus status);
    
    /**
     * Finds events by aggregate ID.
     */
    List<OutboxEvent> findByAggregateIdOrderByCreatedAtDesc(String aggregateId);
}
