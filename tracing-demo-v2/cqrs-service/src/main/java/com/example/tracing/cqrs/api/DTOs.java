package com.example.tracing.cqrs.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTOs for API requests and responses.
 */
public class DTOs {
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class CreateProductRequest {
    @NotBlank(message = "Product name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;
    
    @NotNull(message = "Initial stock is required")
    @PositiveOrZero(message = "Stock cannot be negative")
    private Integer initialStock;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class UpdatePriceRequest {
    @NotNull(message = "New price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal newPrice;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class UpdateStockRequest {
    @NotNull(message = "Quantity is required")
    @PositiveOrZero(message = "Quantity cannot be negative")
    private Integer quantity;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class CreateProductResponse {
    private String productId;
    private String message;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class ApiResponse {
    private String message;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class ErrorResponse {
    private String error;
}
