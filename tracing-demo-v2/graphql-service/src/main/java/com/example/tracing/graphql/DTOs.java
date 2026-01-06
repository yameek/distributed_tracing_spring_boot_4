package com.example.tracing.graphql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

class CreateOrderRequest {
    private String productId;
    private int quantity;
    
    public CreateOrderRequest() {}
    
    public CreateOrderRequest(String productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }
    
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}

class Order {
    private String orderId;
    private String status;
    private String message;
    
    public Order() {}
    
    public Order(String orderId, String status, String message) {
        this.orderId = orderId;
        this.status = status;
        this.message = message;
    }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
