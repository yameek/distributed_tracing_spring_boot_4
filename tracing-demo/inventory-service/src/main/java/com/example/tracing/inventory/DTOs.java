package com.example.tracing.inventory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
class Order implements Serializable {
    private String orderId;
    private String status;
    private String message;
}
