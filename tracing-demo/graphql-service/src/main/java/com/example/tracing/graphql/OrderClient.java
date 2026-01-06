package com.example.tracing.graphql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OrderClient {
    private static final Logger log = LoggerFactory.getLogger(OrderClient.class);
    private final RestTemplate restTemplate;

    public OrderClient(RestTemplateBuilder builder) {
        // Builder automatically configures tracing
        this.restTemplate = builder.build();
    }

    public Order createOrder(String productId, int quantity) {
        log.info("Sending order creation request to order-service for product: {}", productId);
        String url = "http://order-service:8081/orders";
        var request = new CreateOrderRequest(productId, quantity);
        return restTemplate.postForObject(url, request, Order.class);
    }
}
