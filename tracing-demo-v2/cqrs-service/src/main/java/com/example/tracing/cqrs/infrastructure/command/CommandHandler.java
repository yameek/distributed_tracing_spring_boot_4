package com.example.tracing.cqrs.infrastructure.command;

/**
 * Interface for command handlers.
 * Each command should have exactly one handler.
 *
 * @param <T> The type of command this handler processes
 * @param <R> The return type after handling the command
 */
public interface CommandHandler<T extends Command, R> {
    /**
     * Handles the given command and returns a result.
     *
     * @param command The command to handle
     * @return The result of handling the command
     */
    R handle(T command);
    
    /**
     * Returns the type of command this handler can process.
     */
    Class<T> getCommandType();
}
