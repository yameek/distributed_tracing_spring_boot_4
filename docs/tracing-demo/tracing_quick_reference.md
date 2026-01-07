# Quick Reference Guide - Distributed Tracing & Logging

**For:** Spring Boot 4.0.1 + Micrometer Tracing + OpenTelemetry  
**Quick Start:** Essential patterns and code snippets

---

## Table of Contents

1. [Quick Start Checklist](#quick-start-checklist)
2. [Essential Dependencies](#essential-dependencies)
3. [Configuration Snippets](#configuration-snippets)
4. [Code Patterns](#code-patterns)
5. [Common Commands](#common-commands)
6. [Troubleshooting Quick Fixes](#troubleshooting-quick-fixes)

---

## Quick Start Checklist

### For Each New Service

```bash
☐ 1. Add dependencies to pom.xml
☐ 2. Create application.yml with tracing config
☐ 3. Create logback-spring.xml
☐ 4. Add @Observed to key methods
☐ 5. Test trace propagation
☐ 6. Verify logs in Loki
☐ 7. Check traces in Grafana
```

---

## Essential Dependencies

### Minimal pom.xml

```xml
<dependencies>
    <!-- Core Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Observability -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    
    <!-- Tracing -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-tracing-bridge-otel</artifactId>
    </dependency>
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-exporter-otlp</artifactId>
    </dependency>
    
    <!-- Logging -->
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
</dependencies>
```

---

## Configuration Snippets

### application.yml (Copy-Paste Template)

```yaml
spring:
  application:
    name: YOUR-SERVICE-NAME  # ← CHANGE THIS

management:
  endpoints:
    web:
      exposure:
        include: health,metrics
  tracing:
    sampling:
      probability: 1.0  # 100% for dev, 0.1 for prod

otel:
  service:
    name: ${spring.application.name}
  exporter:
    otlp:
      endpoint: http://localhost:4318/v1/traces
  propagators: tracecontext,baggage

logging:
  level:
    root: INFO
    com.example.tracing: INFO
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

### logback-spring.xml (Minimal)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty scope="context" name="serviceName" source="spring.application.name"/>
    
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service":"${serviceName}"}</customFields>
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
        </encoder>
    </appender>
    
    <appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
        <http>
            <url>http://localhost:3100/loki/api/v1/push</url>
        </http>
        <format>
            <label>
                <pattern>service=${serviceName},level=%level</pattern>
            </label>
            <message>
                <pattern>{"level":"%level","message":"%message","traceId":"%X{traceId:-}"}</pattern>
            </message>
        </format>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="LOKI"/>
    </root>
</configuration>
```

---

## Code Patterns

### 1. REST Controller with Tracing

```java
@RestController
@RequestMapping("/api")
public class MyController {
    
    private static final Logger log = LoggerFactory.getLogger(MyController.class);
    private final Tracer tracer;
    
    @PostMapping("/resource")
    @Observed(name = "resource.create", contextualName = "create-resource")
    public ResponseEntity<Resource> createResource(@RequestBody ResourceRequest request) {
        log.info("Creating resource: {}", request.getName());
        
        // Add custom attributes
        if (tracer.currentSpan() != null) {
            tracer.currentSpan().tag("resource.name", request.getName());
            tracer.currentSpan().tag("resource.type", request.getType());
        }
        
        // Your business logic
        Resource resource = service.create(request);
        
        return ResponseEntity.ok(resource);
    }
}
```

### 2. HTTP Client Call (WebClient)

```java
@Component
public class ExternalServiceClient {
    
    private final WebClient webClient;
    
    public ExternalServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
            .baseUrl("http://external-service:8080")
            .build();
    }
    
    @Observed(name = "external.call")
    public ExternalResponse callExternalService(String id) {
        return webClient.get()
            .uri("/api/resource/{id}", id)
            .retrieve()
            .bodyToMono(ExternalResponse.class)
            .block();
    }
}
```

### 3. RabbitMQ Publisher

```java
@Component
public class EventPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    
    @Observed(name = "event.publish")
    public void publishEvent(Event event) {
        log.info("Publishing event: {}", event.getId());
        rabbitTemplate.convertAndSend("events.exchange", "", event);
    }
}
```

### 4. RabbitMQ Consumer

```java
@Component
public class EventListener {
    
    private static final Logger log = LoggerFactory.getLogger(EventListener.class);
    
    @RabbitListener(queues = "events.queue")
    @Observed(name = "event.process")
    public void handleEvent(Event event) {
        log.info("Received event: {}", event.getId());
        // Process event
    }
}
```

### 5. Database Operation (JPA)

```java
@Service
public class OrderService {
    
    private final OrderRepository repository;
    
    @Observed(name = "order.save")
    public Order saveOrder(Order order) {
        log.info("Saving order: {}", order.getId());
        return repository.save(order);  // Automatically traced
    }
}
```

### 6. Custom Span with Attributes

```java
@Service
public class PaymentService {
    
    private final Tracer tracer;
    
    public void processPayment(String orderId, BigDecimal amount) {
        Span span = tracer.nextSpan().name("payment.process");
        
        try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
            span.tag("order.id", orderId);
            span.tag("payment.amount", amount.toString());
            span.tag("payment.currency", "USD");
            
            // Business logic
            callPaymentGateway(orderId, amount);
            
            span.event("payment.authorized");
            
        } catch (Exception e) {
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### 7. Async Operation

```java
@Service
public class AsyncService {
    
    private final Tracer tracer;
    
    @Async
    @Observed(name = "async.operation")
    public CompletableFuture<Result> processAsync(String id) {
        // Trace context is automatically propagated
        log.info("Processing async: {}", id);
        
        return CompletableFuture.completedFuture(new Result(id));
    }
}
```

### 8. Error Handling with Tracing

```java
@Service
public class ResilientService {
    
    private final Tracer tracer;
    
    public void processWithRetry(String id) {
        int attempt = 0;
        
        while (attempt < 3) {
            try {
                process(id);
                return;
            } catch (Exception e) {
                attempt++;
                
                if (tracer.currentSpan() != null) {
                    tracer.currentSpan().tag("retry.attempt", String.valueOf(attempt));
                    tracer.currentSpan().event("retry.failed");
                }
                
                log.warn("Retry attempt {} failed for id: {}", attempt, id, e);
                
                if (attempt >= 3) {
                    tracer.currentSpan().error(e);
                    throw e;
                }
            }
        }
    }
}
```

---

## Common Commands

### Infrastructure

```bash
# Start observability stack
docker compose up -d

# Check container status
docker compose ps

# View container logs
docker compose logs -f tempo
docker compose logs -f loki
docker compose logs -f grafana

# Stop everything
docker compose down

# Clean up volumes
docker compose down -v
```

### Service Management

```bash
# Start all services
bash run_all.sh

# Stop all services
bash stop_all.sh

# View service logs
tail -f logs/graphql-service.log
tail -f logs/order-service.log

# Check service health
curl http://localhost:8080/actuator/health | jq .
curl http://localhost:8081/actuator/health | jq .
```

### Testing

```bash
# Test GraphQL endpoint
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"mutation { createOrder(productId: \"test\", quantity: 1) { orderId status } }"}' \
  | jq .

# Test REST endpoint
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{"productId":"test","quantity":1}' \
  | jq .

# Load test
ab -n 100 -c 10 -p order.json -T application/json \
   http://localhost:8081/orders
```

### Debugging

```bash
# Check if Tempo is receiving traces
curl http://localhost:3200/api/search | jq .

# Query Loki logs
curl -G http://localhost:3100/loki/api/v1/query \
  --data-urlencode 'query={service="order-service"}' \
  --data-urlencode 'limit=10' | jq .

# Check trace by ID
curl "http://localhost:3200/api/traces/TRACE_ID_HERE" | jq .

# View service metrics
curl http://localhost:8081/actuator/metrics | jq .
curl http://localhost:8081/actuator/metrics/http.server.requests | jq .
```

---

## Troubleshooting Quick Fixes

### Problem: No traces in Grafana

```bash
# 1. Check Tempo is running
curl http://localhost:3200/ready

# 2. Check sampling rate
grep "probability" */src/main/resources/application.yml

# 3. Verify OTLP endpoint
grep "otlp" */src/main/resources/application.yml

# 4. Check application logs for errors
grep -i "trace\|otlp" logs/*.log
```

### Problem: Trace context not propagating

```yaml
# Verify in application.yml:
otel:
  propagators: tracecontext,baggage  # Must be set!
```

```java
// Verify WebClient is injected correctly:
@Bean
public WebClient.Builder webClientBuilder() {
    return WebClient.builder();  // Use builder!
}
```

### Problem: No logs in Loki

```bash
# 1. Check Loki is running
curl http://localhost:3100/ready

# 2. Test log ingestion
curl -X POST http://localhost:3100/loki/api/v1/push \
  -H "Content-Type: application/json" \
  -d '{"streams":[{"stream":{"service":"test"},"values":[["'$(date +%s)000000000'","test"]]}]}'

# 3. Check logback configuration
ls -la */src/main/resources/logback-spring.xml

# 4. Verify Loki appender in logs
grep -i "loki" logs/*.log
```

### Problem: High memory usage

```yaml
# Reduce sampling in application.yml:
management:
  tracing:
    sampling:
      probability: 0.1  # Sample only 10%

# Reduce batch size:
otel:
  traces:
    processor:
      batch:
        maxQueueSize: 512
        maxExportBatchSize: 128
```

### Problem: Slow application performance

```xml
<!-- Use async logging in logback-spring.xml: -->
<appender name="ASYNC_LOKI" class="ch.qos.logback.classic.AsyncAppender">
    <appender-ref ref="LOKI"/>
    <queueSize>1000</queueSize>
    <neverBlock>true</neverBlock>
</appender>
```

```yaml
# Enable compression in application.yml:
otel:
  exporter:
    otlp:
      compression: gzip
```

---

## Grafana Quick Tips

### View Traces
1. Go to http://localhost:3000
2. Click **Explore** (compass icon)
3. Select **Tempo** datasource
4. Click **Search** → **Run Query**
5. Click on any trace to see details

### View Logs
1. In **Explore**, select **Loki** datasource
2. Use query: `{service="order-service"}`
3. Add filters: `{service="order-service"} |= "error"`
4. Time range: Last 1 hour

### Correlate Traces and Logs
1. Open a trace in Tempo
2. Click on any span
3. Click **Logs for this span** button
4. See all logs for that specific operation

### Search by Trace ID
1. In Tempo, select **TraceQL** query type
2. Enter: `{ span.traceId = "YOUR_TRACE_ID" }`
3. Run query

### Search by Attributes
```
{ span.http.status_code = 500 }
{ span.order.id = "12345" }
{ duration > 1s }
```

---

## Performance Benchmarks

### Expected Overhead

| Metric | Without Tracing | With Tracing | Overhead |
|--------|----------------|--------------|----------|
| Response Time | 50ms | 52ms | +4% |
| Memory | 200MB | 220MB | +10% |
| CPU | 5% | 5.5% | +10% |

### Production Settings

```yaml
# Recommended for production:
management:
  tracing:
    sampling:
      probability: 0.1  # 10% sampling

otel:
  exporter:
    otlp:
      compression: gzip
      timeout: 5s
  traces:
    processor:
      batch:
        scheduleDelay: 5000
        maxQueueSize: 2048
```

---

## Cheat Sheet

### Span Naming Convention
```
resource.action
  ✓ order.create
  ✓ payment.process
  ✓ inventory.update
  ✗ doSomething
  ✗ processData
```

### Log Message Format
```java
// Good
log.info("Order created: orderId={}, total={}", orderId, total);

// Bad
log.info("Order created");  // No context
log.info("Order " + orderId + " created");  // String concat
```

### Essential Span Attributes
```java
span.tag("order.id", orderId);          // Business ID
span.tag("user.id", userId);            // User context
span.tag("http.status_code", "200");    // HTTP status
span.tag("error", "true");              // Error flag
```

### When to Use @Observed
```
✓ REST endpoints
✓ GraphQL resolvers
✓ Service methods
✓ External API calls
✓ Message publishers/consumers
✗ Getters/setters
✗ Utility methods
✗ Internal helpers
```

---

## Quick Links

- **Grafana:** http://localhost:3000
- **RabbitMQ Management:** http://localhost:15672
- **Tempo API:** http://localhost:3200
- **Loki API:** http://localhost:3100

**Default Credentials:**
- Grafana: admin/admin
- RabbitMQ: guest/guest

---

## Next Steps

1. ✅ Read [COMPREHENSIVE_IMPLEMENTATION_GUIDE.md](COMPREHENSIVE_IMPLEMENTATION_GUIDE.md) for detailed explanations
2. ✅ Check [TRACE_IDS_EXPLANATION.md](TRACE_IDS_EXPLANATION.md) for tracing concepts
3. ✅ Review [VISUALIZATION_GUIDE.md](VISUALIZATION_GUIDE.md) for Grafana tips
4. ✅ See working examples in service directories

---

*Last updated: January 7, 2026*
