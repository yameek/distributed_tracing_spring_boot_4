package com.example.tracing.cqrs.infrastructure.command;

/**
 * Marker interface for all commands in the system.
 * Commands represent intentions to change state.
 */
public interface Command {
    /**
     * Returns a unique identifier for this command instance.
     * Used for tracing and correlation.
     */
    String getCommandId();
}
