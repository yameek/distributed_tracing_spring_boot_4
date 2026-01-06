package com.example.tracing.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import io.micrometer.tracing.annotation.NewSpan;
import io.micrometer.tracing.annotation.SpanTag;

import java.util.UUID;

@RestController
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderPublisher orderPublisher;
    private final OrderRepository orderRepository;

    public OrderController(OrderPublisher orderPublisher, OrderRepository orderRepository) {
        this.orderPublisher = orderPublisher;
        this.orderRepository = orderRepository;
    }

    @PostMapping("/orders")
    @NewSpan("order.process")
    public Order createOrder(@RequestBody CreateOrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        log.info("Received REST request to create order: ID={}, Product={}", orderId, request.getProductId());

        // Save to DB (Traced Operation)
        OrderEntity entity = new OrderEntity(orderId, "CREATED", "Order accepted for " + request.getProductId());
        orderRepository.save(entity);
        log.info("Saved order to H2 database");

        Order order = new Order(orderId, "CREATED", "Order accepted for " + request.getProductId());
        orderPublisher.publishOrder(order);

        return order;
    }
}
