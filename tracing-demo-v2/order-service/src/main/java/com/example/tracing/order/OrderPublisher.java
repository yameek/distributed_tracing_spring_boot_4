package com.example.tracing.order;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public OrderPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Observed(name = "order.publish", contextualName = "order-publish-rabbitmq")
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
