package com.example.tracing.cqrs.infrastructure.query;

/**
 * Marker interface for all queries in the system.
 * Queries represent requests for data without side effects.
 */
public interface Query<R> {
    /**
     * Returns a unique identifier for this query instance.
     * Used for tracing and correlation.
     */
    String getQueryId();
}
