package com.example.tracing.graphql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
class CreateOrderRequest {
    private String productId;
    private int quantity;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Order {
    private String orderId;
    private String status;
    private String message;
}
