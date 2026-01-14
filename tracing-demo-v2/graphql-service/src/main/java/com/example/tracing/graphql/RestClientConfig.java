package com.example.tracing.graphql;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.observation.ClientRequestObservationConvention;
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention;
import org.springframework.web.client.RestTemplate;

import io.micrometer.observation.ObservationRegistry;

@Configuration
public class RestClientConfig {

    /**
     * RestTemplate bean with automatic OpenTelemetry instrumentation.
     * Spring Boot 4's OpenTelemetry starter automatically instruments RestTemplate beans
     * to propagate trace context via HTTP headers (traceparent).
     */
    @Bean
    public RestTemplate restTemplate(ObservationRegistry observationRegistry) {
        RestTemplate restTemplate = new RestTemplate();
        // Enable observation (tracing) for RestTemplate
        restTemplate.setObservationRegistry(observationRegistry);
        return restTemplate;
    }

    /**
     * Provide a custom observation convention for client requests if needed.
     */
    @Bean
    public ClientRequestObservationConvention clientRequestObservationConvention() {
        return new DefaultClientRequestObservationConvention();
    }
}
