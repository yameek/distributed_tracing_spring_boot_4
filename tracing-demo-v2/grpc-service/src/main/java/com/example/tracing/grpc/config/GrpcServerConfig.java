package com.example.tracing.grpc.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration for gRPC server.
 * 
 * Spring Boot's OpenTelemetry starter automatically adds tracing interceptors
 * to gRPC servers, so we don't need to manually configure them here.
 * The @Observed annotations in the service implementation will create
 * custom spans for business logic.
 * 
 * Spring gRPC automatically discovers services annotated with @GrpcService
 * and registers them with the gRPC server.
 */
@Configuration
public class GrpcServerConfig {
    
    // gRPC server configuration is handled by Spring gRPC starter
    // Tracing is automatically enabled via Spring Boot OpenTelemetry starter
}
