package com.example.tracing.cqrs.domain.commands;

import com.example.tracing.cqrs.infrastructure.command.Command;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Command to update product price.
 */
@Data
@Builder
public class UpdateProductPriceCommand implements Command {
    
    @Builder.Default
    private final String commandId = UUID.randomUUID().toString();
    
    private final String productId;
    private final BigDecimal newPrice;
}
