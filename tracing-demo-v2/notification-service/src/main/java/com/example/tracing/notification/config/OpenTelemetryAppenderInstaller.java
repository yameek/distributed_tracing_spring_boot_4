package com.example.tracing.notification.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * Installs the OpenTelemetry Logback appender with the SDK instance.
 * 
 * The OpenTelemetry Logback appender needs to be explicitly connected to the
 * OpenTelemetry SDK to export logs via OTLP. Without this, the appender is
 * configured in logback.xml but doesn't actually send logs anywhere.
 * 
 * Flow: Application logs → Logback → OTEL Appender → OTel Collector → Loki
 */
@Component
public class OpenTelemetryAppenderInstaller implements InitializingBean {
    
    private final OpenTelemetry openTelemetry;
    
    public OpenTelemetryAppenderInstaller(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        // Install the OpenTelemetry instance into the Logback appender
        // This connects the appender to the SDK's LoggerProvider
        OpenTelemetryAppender.install(openTelemetry);
    }
}
