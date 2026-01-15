# Implementation Guide: Distributed Tracing with Spring Boot 4

This is the comprehensive technical guide for implementing distributed tracing in microservices. It covers every dependency, configuration setting, and code pattern required to make tracing work across HTTP and RabbitMQ.

## Target Stack

- **Framework**: Spring Boot 4.0.1
- **Language**: Java 21+ (tested with Java 25)
- **Tracing**: Micrometer Tracing + OpenTelemetry
- **Protocol**: OTLP (gRPC)
- **Messaging**: RabbitMQ
- **Observability**: Grafana Tempo (traces), Loki (logs), Grafana (visualization)

## What You'll Learn

- How to add distributed tracing to Spring Boot applications
- How to propagate trace context across HTTP calls
- How to propagate trace context through RabbitMQ
- How to correlate logs with traces
- How to visualize traces in Grafana
- Common troubleshooting patterns

---

## 1. Dependency Management (`pom.xml`)

We use the official **Spring Boot 4 OpenTelemetry Starter**, which replaces the manual boilerplate used in Spring Boot 3.

Add these dependencies to your service's `pom.xml`:

### Core Tracing
```xml
<!-- Spring Boot 4 OpenTelemetry Starter -->
<!-- Automatically configures Tracer, Context Propagation, and OTLP Exporter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-opentelemetry</artifactId>
    <version>4.0.1</version>
</dependency>

<!-- Spring Boot Actuator (Required for metrics/health) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Logging (with Trace Correlation)
To send logs to Loki with JSON formatting and Trace IDs:

```xml
<dependency>
    <groupId>com.github.loki4j</groupId>
    <artifactId>loki-logback-appender</artifactId>
    <version>1.4.2</version>
</dependency>
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>8.0</version>
</dependency>
```

---

## 2. Application Configuration (`application.yml`)

This configuration enables tracing, sets sampling to 100% (for demo purposes), and points the exporter to Tempo.

**Crucial Note**: The code uses **gRPC (port 4317)** for trace export, not HTTP.

```yaml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0  # 1.0 = Keep 100% of traces. Use 0.01-0.1 in production.

  # OpenTelemetry Configuration
  opentelemetry:
    tracing:
      export:
        otlp:
          enabled: true
          transport: grpc                 # Use gRPC for performance
          endpoint: http://localhost:4317 # Tempo's OTLP gRPC port
    
    # Disable OTLP Metrics/Logging export if you don't use them (reduces noise)
    metrics:
      export:
        otlp:
          enabled: false
    logging:
      export:
        otlp:
          enabled: false

  # Additional cleanup to prevent default OTLP metrics registry errors
  otlp:
    metrics:
      export:
        enabled: false

logging:
  pattern:
    # Console pattern including traceId and spanId for local debugging
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

---

## 3. Java Code Implementation

Simply adding dependencies isn't enough. You must configure your beans to ensure context propagation.

### A. HTTP Propagation (Client Side)

**Problem**: `new RestTemplate()` is not instrumented by Spring.
**Solution**: Create a `RestTemplate` bean and inject the `ObservationRegistry`.

**File**: `RestClientConfig.java`
```java
@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate restTemplate(ObservationRegistry observationRegistry) {
        RestTemplate restTemplate = new RestTemplate();
        // CRITICAL: This enables automatic trace propagation (W3C traceparent header)
        restTemplate.setObservationRegistry(observationRegistry);
        return restTemplate;
    }
}
```

**Usage**:
```java
@Service
public class OrderClient {
    private final RestTemplate restTemplate;

    // Inject the instrumented bean
    public OrderClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public void callOrderService() {
        // Trace headers are now automatically added to this request
        restTemplate.postForObject("http://order-service/orders", ...);
    }
}
```

### B. RabbitMQ Propagation (Async)

Trace context must be injected into the AMQP message headers by the producer and extracted by the consumer.

**1. Producer Configuration** (`RabbitMqConfig.java`)

```java
@Bean
public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, 
                                     MessageConverter jsonMessageConverter) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(jsonMessageConverter);
    
    // CRITICAL: Enables trace injection into message headers
    rabbitTemplate.setObservationEnabled(true);
    
    return rabbitTemplate;
}
```

**2. Consumer Configuration** (`RabbitMqConfig.java`)

```java
@Bean
public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
        ConnectionFactory connectionFactory, 
        MessageConverter jsonMessageConverter) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(jsonMessageConverter);
    
    // CRITICAL: Enables trace extraction from message headers
    factory.setObservationEnabled(true);
    
    return factory;
}
```

**3. Listener Implementation**

No special code is needed in the `@RabbitListener` method itself. The `ContainerFactory` handles the context extraction before your method runs.

```java
@RabbitListener(queues = "orders.queue")
public void handleOrderCreated(Order order) {
    // Current thread now has the Trace ID from the producer!
    log.info("Processing order: {}", order.getId());
}
```

---

## 4. Logging Configuration (`logback-spring.xml`)

To correlate logs with traces, we use the standard MDC fields `traceId` and `spanId` which Spring Boot automatically populates.

```xml
<configuration>
    <appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
        <http>
            <url>http://localhost:3100/loki/api/v1/push</url>
        </http>
        <format>
            <label>
                <pattern>app=${appName},host=${HOSTNAME},level=%level</pattern>
            </label>
            <message>
                <pattern>{
                    "level":"%level",
                    "class":"%logger{36}",
                    "message":"%message",
                    "traceId":"%mdc{traceId:-}", 
                    "spanId":"%mdc{spanId:-}"
                }</pattern>
            </message>
        </format>
    </appender>

    <root level="INFO">
        <appender-ref ref="LOKI" />
    </root>
</configuration>
```

**Why this works**:
The OpenTelemetry starter automatically puts the current `traceId` and `spanId` into the SLF4J MDC (Mapped Diagnostic Context). Logback simply reads these values.

---

## 5. Infrastructure (Docker Compose)

You need the observability backend running.

```yaml
services:
  tempo:
    image: grafana/tempo:latest
    ports:
      - "4317:4317"  # OTLP gRPC (used by our app)
      - "3200:3200"  # Tempo HTTP API (used by Grafana)
    command: [ "-config.file=/etc/tempo.yaml" ]

  loki:
    image: grafana/loki:latest
    ports:
      - "3100:3100"

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_AUTH_ANONYMOUS_ENABLED=true
      - GF_AUTH_ANONYMOUS_ORG_ROLE=Admin
```

---

## 6. Verification Checklist

When you implement this in a new service, check these 3 things:

1.  **Logs contain Trace IDs**:
    Run the app and look at the logs. You should see `[service-name, 663f...2a, 12...89]`.
    If you see `[service-name,,]`, tracing is NOT working.

2.  **Propagating between services**:
    Make a request from Service A to Service B. Both should log the **same** Trace ID.

3.  **Visible in Tempo**:
    Go to Grafana -> Explore -> Tempo. Search for that Trace ID. You should see spans from both services connected in a single tree.

---

## 7. Testing Your Implementation

### Quick Test Commands

```bash
# Start the infrastructure
docker compose up -d

# Build and start all services
./run_all.sh

# Send a test request
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{"product":"laptop","quantity":2}'

# Check logs for trace ID
tail -n 5 order-service/logs/order-service.json.log | jq '{traceId, spanId, message}'
```

### Verify Trace Propagation

1. Send a request through the GraphQL service
2. Check that all 4 services log the same Trace ID
3. View the complete trace in Grafana Tempo

```bash
# Get trace ID from GraphQL service
TRACE_ID=$(tail -1 graphql-service/logs/graphql-service.json.log | jq -r '.traceId')

# Verify it appears in all services
for service in graphql order inventory notification; do
  echo "=== $service-service ==="
  grep "$TRACE_ID" ${service}-service/logs/${service}-service.json.log | wc -l
done
```

---

## 8. Common Issues and Solutions

### Issue: Trace IDs are null in logs

**Symptoms**: Logs show `"traceId": null`

**Causes**:
- Tracing not enabled in `application.yml`
- Missing Spring Boot OpenTelemetry starter
- Sampling probability set to 0

**Solutions**:
```yaml
# Ensure these settings in application.yml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0  # 1.0 = 100% sampling
```

### Issue: Different Trace IDs across services

**Symptoms**: Each service logs a different Trace ID for the same request

**Causes**:
- HTTP client not instrumented (e.g., using `new RestTemplate()`)
- RabbitMQ observation not enabled
- W3C propagation not configured

**Solutions**:
```java
// For HTTP: Use a Spring-managed RestTemplate bean
@Bean
public RestTemplate restTemplate(ObservationRegistry observationRegistry) {
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.setObservationRegistry(observationRegistry);
    return restTemplate;
}

// For RabbitMQ: Enable observation
@Bean
public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setObservationEnabled(true);  // Critical!
    return template;
}
```

### Issue: No traces in Tempo

**Symptoms**: Grafana Tempo shows no traces

**Causes**:
- Tempo not running or unreachable
- Wrong OTLP endpoint in configuration
- Network issues

**Solutions**:
```bash
# Check Tempo is running
docker compose ps tempo
curl http://localhost:4317  # Should connect

# Verify configuration
grep "endpoint" */src/main/resources/application.yml
# Should show: http://localhost:4317

# Check application logs for export errors
grep -i "otlp\|exporter" */logs/*.log
```

### Issue: RabbitMQ consumers don't receive trace context

**Symptoms**: Async consumers log different Trace IDs

**Causes**:
- Listener container factory observation not enabled
- Using default container factory instead of custom one

**Solutions**:
```java
@Bean
public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
        ConnectionFactory connectionFactory) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setObservationEnabled(true);  // Must enable!
    return factory;
}
```

---

## 9. Performance Considerations

### Sampling Strategy

In production, don't sample 100% of requests:

```yaml
management:
  tracing:
    sampling:
      probability: 0.01  # Sample 1% of requests
```

### Why?
- Reduces storage requirements in Tempo
- Lowers network traffic
- Minimal performance overhead
- Still captures enough data for debugging

### When to use 100% sampling
- Development/testing environments
- Debugging specific issues
- Low-traffic services (<100 req/sec)

### Expected Overhead

| Metric | Without Tracing | With Tracing (100%) | With Tracing (1%) |
|--------|----------------|---------------------|-------------------|
| Response Time | 50ms | 52ms (+4%) | 50.2ms (+0.4%) |
| Memory | 200MB | 220MB (+10%) | 205MB (+2.5%) |
| CPU | 5% | 5.5% (+10%) | 5.1% (+2%) |

---

## 10. Best Practices

### DO ✅

1. **Use Spring-managed beans** for HTTP clients and RabbitMQ templates
2. **Enable observation** on all communication components
3. **Log consistently** using structured logging (JSON)
4. **Add business context** to spans (e.g., order ID, user ID)
5. **Use meaningful span names** (e.g., `order.create`, not `doSomething`)
6. **Test trace propagation** in integration tests

### DON'T ❌

1. **Don't create HTTP clients with `new`** - use `@Bean` injection
2. **Don't forget to enable observation** on RabbitMQ components
3. **Don't sample 100%** in production
4. **Don't log sensitive data** (passwords, tokens) in trace attributes
5. **Don't create too many custom spans** - use automatic instrumentation
6. **Don't ignore sampling** - configure it appropriately

---

## 11. Next Steps

### For Development
- Explore the working demo in `tracing-demo-v2/`
- View traces in Grafana at http://localhost:3000
- See [`docs/GRAFANA_GUIDE.md`](GRAFANA_GUIDE.md) for visualization tips

### For Production
- Adjust sampling rates based on traffic
- Set up proper Tempo storage (S3, GCS)
- Configure retention policies
- Add alerting on trace errors
- Secure OTLP endpoints with authentication

### Additional Resources
- [`docs/FIX_HISTORY.md`](FIX_HISTORY.md) - Common issues and how they were fixed
- [`docs/ARCHITECTURE.md`](ARCHITECTURE.md) - System architecture overview
- [`docs/tracing-demo/`](tracing-demo/) - Detailed guides and examples

---

## Summary

This implementation guide covered:

1. ✅ Dependencies: Spring Boot OpenTelemetry starter
2. ✅ Configuration: OTLP export to Tempo
3. ✅ HTTP Propagation: Instrumented RestTemplate
4. ✅ RabbitMQ Propagation: Observation-enabled templates
5. ✅ Logging: Trace ID correlation
6. ✅ Verification: Testing and troubleshooting
7. ✅ Production: Performance and best practices

**Key Takeaway**: With Spring Boot 4.0.1, distributed tracing is mostly automatic. The critical parts are:
- Using the OpenTelemetry starter
- Enabling observation on communication components
- Proper configuration of OTLP endpoints

Follow this guide, and you'll have working distributed tracing across your microservices!
