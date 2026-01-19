package com.example.tracing.orchestrator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for the product workflow that combines HTTP and RabbitMQ operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductWorkflowRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer initialStock;
    private BigDecimal updatedPrice;
    private Integer updatedStock;
}
