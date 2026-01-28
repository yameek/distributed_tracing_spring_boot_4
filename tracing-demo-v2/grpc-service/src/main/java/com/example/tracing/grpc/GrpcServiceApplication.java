package com.example.tracing.grpc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * gRPC Service - Demonstrates distributed tracing with gRPC.
 *
 * This service exposes gRPC endpoints for product management and shows how
 * trace context is propagated through gRPC calls, maintaining the same trace ID
 * across service boundaries.
 */
@Slf4j
@SpringBootApplication
public class GrpcServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GrpcServiceApplication.class, args);
        log.info("gRPC Service started successfully");
    }
}
