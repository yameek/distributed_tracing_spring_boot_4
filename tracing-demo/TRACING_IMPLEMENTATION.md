# Tracing Implementation Guide

This document explains the implementation of Distributed Tracing in this project, using **Micrometer Tracing**, **OpenTelemetry**, and **Loki**.

## 1. The Dependencies ("The Bridge")
We use the **Facade Pattern**. We don't write code for OpenTelemetry directly. Instead, we use Micrometer.

**Dependencies (build.gradle):**
```groovy
// 1. The Facade (Micrometer Tracing API)
implementation 'io.micrometer:micrometer-tracing-bridge-otel'

// 2. The Exporter (Sends data to Tempo/Zipkin)
implementation 'io.opentelemetry:opentelemetry-exporter-otlp'

// 3. The Log Correlator (Injects TraceID into Logs)
implementation 'com.github.loki4j:loki-logback-appender:1.4.2'
```

*   **`micrometer-tracing-bridge-otel`**: Translates generic Micrometer calls into OpenTelemetry spans.
*   **`opentelemetry-exporter-otlp`**: Pushes the spans to the OTLP endpoint (defined in config).

---

## 2. Configuration ("The Wiring")
Tracing is mostly auto-configured by Spring Boot 4.

**`application.yml`**:
```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # RECORD EVERYTHING (For Demo Only)
  otlp:
    tracing:
      endpoint: http://tempo:4318/v1/traces # The Destination
```

*   **Sampling**: In production, set this to `0.1` (10%) or lower to save costs.
*   **Endpoint**: Points to the **Tempo** container running in Docker.

---

## 3. Propagation ("The Handover")
How does the `TraceID` jump from Service A to Service B?

*   **HTTP (One-to-One)**:
    *   Micrometer automatically instruments `RestTemplate` and `WebClient`.
    *   It adds `traceparent` headers to outgoing requests.
    *   The receiving service (Spring Web) reads these headers and continues the span.

*   **RabbitMQ (One-to-Many/Async)**:
    *   Micrometer instruments `RabbitTemplate`.
    *   It adds trace headers to the AMQP Message Properties.
    *   **Crucial**: The consumer using `@RabbitListener` automatically reads these properties to start a child span.

---

## 4. Log Correlation ("The Missing Link")
To debug effectively, you need to see logs belonging to a specific trace.

**`logback-spring.xml`**:
```xml
<pattern>app=${appName},host=${HOSTNAME},traceID=%X{traceId}</pattern>
```
*   `%X{traceId}`: Micrometer puts the active trace ID into the MDC (Mapped Diagnostic Context).
*   **Result**: Every log line includes `traceID=abc-123`, allowing Grafana/Loki to filter efficiently.

---

## 5. Future Improvement: Notification Service (Fan-Out)
To see a complex "Fan-Out" trace, you could add a **Notification Service**.

1.  **Change**: When `order-service` publishes an event, have **TWO** consumers.
    *   `inventory-service` (Current)
    *   `notification-service` (New)
2.  **Result**:
    *   The trace will split into two parallel branches.
    *   This demonstrates how async messaging allows creating complex, non-blocking workflows that are still fully observable.
