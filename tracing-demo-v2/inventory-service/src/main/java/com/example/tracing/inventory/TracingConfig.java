package com.example.tracing.inventory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;

@Configuration
public class TracingConfig {

    @Value("${spring.application.name}")
    private String serviceName;

    /**
     * Configure OpenTelemetry TracerProvider with OTLP exporter.
     * Spring Boot will automatically use this for Micrometer tracing.
     */
    @Bean
    public SdkTracerProvider sdkTracerProvider() {
        Resource resource = Resource.getDefault()
                .merge(Resource.create(io.opentelemetry.api.common.Attributes.of(
                        ResourceAttributes.SERVICE_NAME, serviceName
                )));

        OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint("http://localhost:4317")
                .setTimeout(java.time.Duration.ofSeconds(10))
                .build();

        return SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .setResource(resource)
                .build();
    }
}
