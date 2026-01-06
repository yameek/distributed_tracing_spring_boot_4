package com.example.tracing.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import io.micrometer.tracing.annotation.NewSpan;
import io.micrometer.tracing.annotation.SpanTag;

@Service
public class OrderListener {

    private static final Logger log = LoggerFactory.getLogger(OrderListener.class);

    // Micrometer Tracing automatically creates a child span when a message is received
    @RabbitListener(queues = "orders.queue")
    @NewSpan("inventory.update")
    public void handleOrder(Order order) throws InterruptedException {
        log.info("Received order from RabbitMQ: {}", order.getOrderId());
        
        // Simulate work
        Thread.sleep(100);
        
        log.info("Inventory updated for order: {}", order.getOrderId());
    }
}
