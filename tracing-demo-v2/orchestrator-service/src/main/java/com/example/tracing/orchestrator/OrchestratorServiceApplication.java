package com.example.tracing.orchestrator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Orchestrator Service - Demonstrates distributed tracing across HTTP and
 * RabbitMQ.
 *
 * This service orchestrates business workflows that call the cqrs-service
 * through both HTTP REST API and RabbitMQ messaging, showing how trace context
 * is propagated across different communication protocols within a single
 * distributed transaction.
 */
@Slf4j
@SpringBootApplication
public class OrchestratorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrchestratorServiceApplication.class, args);
        log.info("Orchestrator Service started successfully");
    }
}
