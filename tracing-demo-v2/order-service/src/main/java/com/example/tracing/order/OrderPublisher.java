package com.example.tracing.order;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderPublisher.class);
    private final RabbitTemplate rabbitTemplate;
    private final Tracer tracer;

    public OrderPublisher(RabbitTemplate rabbitTemplate, @Autowired(required = false) Tracer tracer) {
        this.rabbitTemplate = rabbitTemplate;
        this.tracer = tracer;
    }

    public void publishOrder(Order order) {
        log.info("Publishing order to RabbitMQ: {}", order.getOrderId());
        // Micrometer Tracing automatically injects trace context into RabbitMQ headers
        rabbitTemplate.convertAndSend(
                OrderServiceApplication.EXCHANGE_NAME,
                OrderServiceApplication.ROUTING_KEY,
                order
        );
    }
}
