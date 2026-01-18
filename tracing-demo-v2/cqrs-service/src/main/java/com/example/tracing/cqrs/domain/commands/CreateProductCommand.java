package com.example.tracing.cqrs.domain.commands;

import com.example.tracing.cqrs.infrastructure.command.Command;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Command to create a new product.
 */
@Data
@Builder
public class CreateProductCommand implements Command {
    
    @Builder.Default
    private final String commandId = UUID.randomUUID().toString();
    
    private final String name;
    private final String description;
    private final BigDecimal price;
    private final Integer initialStock;
}
