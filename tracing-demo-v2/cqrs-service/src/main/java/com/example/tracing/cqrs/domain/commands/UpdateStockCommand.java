package com.example.tracing.cqrs.domain.commands;

import com.example.tracing.cqrs.infrastructure.command.Command;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Command to update product stock.
 */
@Data
@Builder
public class UpdateStockCommand implements Command {
    
    @Builder.Default
    private final String commandId = UUID.randomUUID().toString();
    
    private final String productId;
    private final Integer quantity;
}
