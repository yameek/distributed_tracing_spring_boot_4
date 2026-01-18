package com.example.tracing.cqrs.infrastructure.query;

/**
 * Interface for query handlers.
 * Each query should have exactly one handler.
 *
 * @param <T> The type of query this handler processes
 * @param <R> The return type of the query result
 */
public interface QueryHandler<T extends Query<R>, R> {
    /**
     * Handles the given query and returns a result.
     *
     * @param query The query to handle
     * @return The query result
     */
    R handle(T query);
    
    /**
     * Returns the type of query this handler can process.
     */
    Class<T> getQueryType();
}
