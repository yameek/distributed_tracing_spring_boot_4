package com.example.tracing.cqrs.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for the outbox publisher and incoming commands.
 */
@Configuration
public class RabbitMqConfig {
    
    // Outbox publisher configuration (existing)
    public static final String EXCHANGE_NAME = "cqrs.events.exchange";
    public static final String QUEUE_NAME = "cqrs.events.queue";
    public static final String ROUTING_KEY = "event.#";
    
    // Incoming commands configuration (new)
    public static final String COMMANDS_EXCHANGE_NAME = "cqrs.commands.exchange";
    public static final String COMMANDS_QUEUE_NAME = "cqrs.commands.queue";
    public static final String COMMANDS_ROUTING_KEY = "command.#";
    
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }
    
    @Bean
    public Queue queue() {
        return new Queue(QUEUE_NAME, true);
    }
    
    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }
    
    // Commands exchange and queue for incoming commands
    @Bean
    public TopicExchange commandsExchange() {
        return new TopicExchange(COMMANDS_EXCHANGE_NAME);
    }
    
    @Bean
    public Queue commandsQueue() {
        return new Queue(COMMANDS_QUEUE_NAME, true);
    }
    
    @Bean
    public Binding commandsBinding(Queue commandsQueue, TopicExchange commandsExchange) {
        return BindingBuilder.bind(commandsQueue).to(commandsExchange).with(COMMANDS_ROUTING_KEY);
    }
    
    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        // Enable observation for trace propagation
        template.setObservationEnabled(true);
        return template;
    }
    
    /**
     * Configure listener container factory with observation enabled for trace propagation.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        // Enable observation for trace propagation
        factory.setObservationEnabled(true);
        return factory;
    }
}
