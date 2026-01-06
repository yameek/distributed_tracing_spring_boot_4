package com.example.tracing.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import io.micrometer.tracing.annotation.NewSpan;
import io.micrometer.tracing.annotation.SpanTag;

@Service
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    // This listener binds to the SAME exchange as Inventory Service, creating a Fan-Out effect
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "notifications.queue"),
            exchange = @Exchange(name = "orders.exchange", type = ExchangeTypes.TOPIC),
            key = "orders.created"
    ))
    @NewSpan("notification.send")
    public void handleOrderNotification(Order order) throws InterruptedException {
        log.info("📧 Notification Service received order: {}", order.getOrderId());
        
        // Simulate sending an email
        Thread.sleep(150);
        
        log.info("✅ Email sent for order: {}", order.getOrderId());
    }
}
