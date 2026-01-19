package com.example.tracing.cqrs.domain.commands;

import com.example.tracing.cqrs.infrastructure.command.Command;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    
    @JsonCreator
    public UpdateStockCommand(
            @JsonProperty("commandId") String commandId,
            @JsonProperty("productId") String productId,
            @JsonProperty("quantity") Integer quantity) {
        this.commandId = commandId != null ? commandId : UUID.randomUUID().toString();
        this.productId = productId;
        this.quantity = quantity;
    }
}
