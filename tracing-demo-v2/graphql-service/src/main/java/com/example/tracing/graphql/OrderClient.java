package com.example.tracing.graphql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OrderClient {
    private static final Logger log = LoggerFactory.getLogger(OrderClient.class);
    private final RestTemplate restTemplate;

    public OrderClient() {
        // RestTemplate automatically configures tracing in Spring Boot 4.0.1
        this.restTemplate = new RestTemplate();
    }

    public Order createOrder(String productId, int quantity) {
        log.info("Sending order creation request to order-service for product: {}", productId);
        String url = "http://localhost:8081/orders";
        CreateOrderRequest request = new CreateOrderRequest();
        request.setProductId(productId);
        request.setQuantity(quantity);
        return restTemplate.postForObject(url, request, Order.class);
    }
}
