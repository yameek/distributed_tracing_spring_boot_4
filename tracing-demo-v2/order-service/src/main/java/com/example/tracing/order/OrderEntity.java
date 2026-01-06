package com.example.tracing.order;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    private String orderId;
    private String status;
    private String message;
    
    public OrderEntity() {}
    
    public OrderEntity(String orderId, String status, String message) {
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
