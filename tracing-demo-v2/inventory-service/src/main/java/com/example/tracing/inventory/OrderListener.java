package com.example.tracing.inventory;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class OrderListener {

    private static final Logger log = LoggerFactory.getLogger(OrderListener.class);

    /**
     * Listen for order events from RabbitMQ. Using @QueueBinding to
     * auto-declare queue, exchange, and binding. This ensures the queue exists
     * even if order-service hasn't started yet.
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "orders.queue"),
            exchange = @Exchange(name = "orders.exchange", type = ExchangeTypes.TOPIC),
            key = "orders.created"
    ))
    @Observed(name = "inventory.update", contextualName = "inventory-update-order")
    public void handleOrder(Order order) throws InterruptedException {
        log.info("Received order from RabbitMQ: {}", order.getOrderId());

        // Simulate work
        Thread.sleep(100);

        log.info("Inventory updated for order: {}", order.getOrderId());
    }
}
