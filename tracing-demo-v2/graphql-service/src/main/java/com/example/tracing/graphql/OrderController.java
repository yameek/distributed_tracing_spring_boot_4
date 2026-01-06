package com.example.tracing.graphql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import io.micrometer.tracing.annotation.NewSpan;
import io.micrometer.tracing.annotation.SpanTag;

@Controller
public class OrderController {
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderClient orderClient;

    public OrderController(OrderClient orderClient) {
        this.orderClient = orderClient;
    }

    @QueryMapping
    public String hello() {
        return "Hello from GraphQL Service";
    }

    @MutationMapping
    @NewSpan("graphql.createOrder")
    public Order createOrder(@SpanTag("product.id") @Argument String productId, 
                             @SpanTag("order.quantity") @Argument int quantity) {
        log.info("Received GraphQL mutation createOrder: {} x {}", quantity, productId);
        return orderClient.createOrder(productId, quantity);
    }
}
