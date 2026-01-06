package com.example.tracing.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
class CreateOrderRequest implements Serializable {
    private String productId;
    private int quantity;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Order implements Serializable {
    private String orderId;
    private String status;
    private String message;
}
