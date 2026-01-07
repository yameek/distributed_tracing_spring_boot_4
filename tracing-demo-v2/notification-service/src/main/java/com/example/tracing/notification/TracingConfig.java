package com.example.tracing.notification;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.tracing.otel.bridge.OtelBaggageManager;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;

@Configuration
public class TracingConfig {

    @Value("${spring.application.name}")
    private String serviceName;

    @Bean
    public OpenTelemetry openTelemetry() {
        Resource resource = Resource.getDefault()
                .merge(Resource.create(io.opentelemetry.api.common.Attributes.of(
                        ResourceAttributes.SERVICE_NAME, serviceName
                )));

        OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint("http://localhost:4317")
                .build();

        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .setResource(resource)
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .buildAndRegisterGlobal();
    }

    @Bean
    public Tracer otelTracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(serviceName);
    }

    @Bean
    public io.micrometer.tracing.Tracer micrometerTracer(Tracer otelTracer, OpenTelemetry openTelemetry) {
        OtelCurrentTraceContext otelCurrentTraceContext = new OtelCurrentTraceContext();
        
        // Create event publisher
        OtelTracer.EventPublisher eventPublisher = event -> {
            // Default implementation - can be customized for event handling
        };
        
        // Create baggage manager
        OtelBaggageManager baggageManager = new OtelBaggageManager(
            otelCurrentTraceContext, 
            Collections.emptyList(), 
            Collections.emptyList()
        );
        
        return new OtelTracer(otelTracer, otelCurrentTraceContext, eventPublisher, baggageManager);
    }
}
