# Logging Implementation Flow - Complete Guide

## 📋 Table of Contents
1. [Overview](#overview)
2. [Architecture Diagram](#architecture-diagram)
3. [Dependencies](#dependencies)
4. [Configuration Flow](#configuration-flow)
5. [Runtime Flow](#runtime-flow)
6. [Code Examples](#code-examples)
7. [Log Destinations](#log-destinations)
8. [Trace ID Propagation](#trace-id-propagation)

---

## Overview

The logging system uses **structured JSON logging** with **automatic trace ID injection** powered by:
- **SLF4J** - Standard logging facade
- **Logback** - Logging implementation
- **Logstash Encoder** - JSON formatting
- **OpenTelemetry** - Automatic trace ID injection into MDC
- **Loki4j** - Log aggregation

### Key Features
✅ **JSON structured logs** - Easy to parse and query  
✅ **Automatic trace correlation** - TraceId and SpanId in every log  
✅ **Multi-destination** - Console, File, and Loki  
✅ **Zero-code instrumentation** - No manual trace ID handling  
✅ **Production-ready** - Log rotation, compression, retention policies  

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Application Code                              │
│  (OrderController.java, CqrsService.java, etc.)                     │
│                                                                       │
│  log.info("Processing order");  ← Uses SLF4J Logger                │
└────────────────────────┬────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│               Spring Boot OpenTelemetry Auto-Config                  │
│  (spring-boot-starter-opentelemetry)                                │
│                                                                       │
│  • Creates spans automatically for HTTP, DB, RabbitMQ               │
│  • Injects TraceId & SpanId into MDC (Mapped Diagnostic Context)   │
│  • Propagates trace context across services                          │
└────────────────────────┬────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   SLF4J MDC (Thread-Local Storage)                  │
│                                                                       │
│  MDC Context:                                                        │
│  ├─ traceId: "34a9d8065ac26ee97a1b969b8666a570"                    │
│  ├─ spanId: "007d867110823eb5"                                      │
│  └─ parentSpanId: "8f5d12e4c7a3b6d9"                               │
└────────────────────────┬────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                  Logback (logback-spring.xml)                       │
│  • Reads MDC values (traceId, spanId)                               │
│  • Formats log entries using LogstashEncoder                        │
│  • Routes to multiple appenders                                      │
└────────────┬──────────────┬──────────────┬────────────────────────┘
             │              │              │
             ▼              ▼              ▼
    ┌────────────┐  ┌────────────┐  ┌────────────┐
    │  CONSOLE   │  │    FILE    │  │    LOKI    │
    │  Appender  │  │  Appender  │  │  Appender  │
    └──────┬─────┘  └──────┬─────┘  └──────┬─────┘
           │                │                │
           ▼                ▼                ▼
    ┌────────────┐  ┌────────────┐  ┌────────────┐
    │  stdout    │  │  logs/     │  │   Loki     │
    │  (Docker)  │  │  *.log     │  │ (Grafana)  │
    └────────────┘  └────────────┘  └────────────┘
```

---

## Dependencies

### 1. **build.gradle** - Required Dependencies

```gradle
dependencies {
    // OpenTelemetry - Auto-instruments and injects trace IDs into MDC
    implementation 'org.springframework.boot:spring-boot-starter-opentelemetry'
    
    // Logstash Encoder - Converts logs to structured JSON
    implementation 'net.logstash.logback:logstash-logback-encoder:8.0'
    
    // Loki Appender - Sends logs to Grafana Loki
    implementation 'com.github.loki4j:loki-logback-appender:1.4.2'
}
```

### 2. **What Each Dependency Does**

| Dependency | Purpose | Output |
|------------|---------|--------|
| `spring-boot-starter-opentelemetry` | Auto-instruments spans, injects traceId/spanId into MDC | MDC populated with trace context |
| `logstash-logback-encoder` | Formats logs as JSON | `{"@timestamp":"...", "level":"INFO", ...}` |
| `loki-logback-appender` | Sends logs to Loki | Centralized log aggregation |

---

## Configuration Flow

### Step 1: Application Properties (`application.yml`)

```yaml
spring:
  application:
    name: order-service  # Used in logs as service identifier

management:
  tracing:
    enabled: true        # Enable OpenTelemetry tracing
    sampling:
      probability: 1.0   # Sample 100% of traces (use 0.1 for 10% in production)
  
  opentelemetry:
    tracing:
      export:
        otlp:
          enabled: true
          transport: grpc
          endpoint: http://localhost:4317  # OpenTelemetry Collector

logging:
  pattern:
    # Pattern for console logging (used by Spring Boot default logger)
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

**What happens:**
1. Spring Boot reads `management.tracing.enabled=true`
2. OpenTelemetry auto-configuration kicks in
3. Trace context is **automatically injected into SLF4J MDC**
4. MDC keys: `traceId`, `spanId`, `parentSpanId`

### Step 2: Logback Configuration (`logback-spring.xml`)

#### **Console Appender - JSON Output**

```xml
<appender name="CONSOLE_JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
        <providers>
            <!-- Timestamp in ISO format -->
            <timestamp>
                <fieldName>@timestamp</fieldName>
                <pattern>yyyy-MM-dd'T'HH:mm:ss.SSSXXX</pattern>
            </timestamp>
            
            <!-- Log version -->
            <version/>
            
            <!-- Log level (INFO, WARN, ERROR) -->
            <logLevel>
                <fieldName>level</fieldName>
            </logLevel>
            
            <!-- Logger class name -->
            <loggerName>
                <fieldName>logger</fieldName>
            </loggerName>
            
            <!-- Log message -->
            <message>
                <fieldName>message</fieldName>
            </message>
            
            <!-- ⭐ MDC - Extracts traceId and spanId from thread-local storage -->
            <mdc>
                <includeMdcKeyName>traceId</includeMdcKeyName>
                <includeMdcKeyName>spanId</includeMdcKeyName>
                <includeMdcKeyName>parentSpanId</includeMdcKeyName>
            </mdc>
            
            <!-- Static fields -->
            <pattern>
                <pattern>
                    {
                        "service": "${appName}",
                        "host": "${hostname}",
                        "thread": "%thread"
                    }
                </pattern>
            </pattern>
            
            <!-- Stack trace for exceptions -->
            <stackTrace>
                <fieldName>stack_trace</fieldName>
            </stackTrace>
        </providers>
    </encoder>
</appender>
```

#### **File Appender - JSON with Rotation**

```xml
<appender name="FILE_JSON" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/${appName}.json.log</file>
    <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
        <!-- Same providers as console -->
    </encoder>
    
    <!-- Rolling policy - manages file size and retention -->
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>logs/${appName}.json.log.%d{yyyy-MM-dd}.%i.gz</fileNamePattern>
        <maxFileSize>100MB</maxFileSize>      <!-- Max size per file -->
        <maxHistory>30</maxHistory>           <!-- Keep 30 days -->
        <totalSizeCap>10GB</totalSizeCap>     <!-- Max total size -->
        <cleanHistoryOnStart>true</cleanHistoryOnStart>
    </rollingPolicy>
</appender>
```

#### **Loki Appender - Centralized Logging**

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <http>
        <url>http://localhost:3100/loki/api/v1/push</url>
    </http>
    <format>
        <!-- Labels for filtering in Grafana -->
        <label>
            <pattern>service=${appName},host=${hostname},level=%level</pattern>
        </label>
        
        <!-- Log message in JSON format -->
        <message>
            <pattern>
                {"@timestamp":"%date{yyyy-MM-dd'T'HH:mm:ss.SSSXXX}",
                 "level":"%level",
                 "logger":"%logger{36}",
                 "message":"%message",
                 "traceId":"%mdc{traceId}",  ← Extract from MDC
                 "spanId":"%mdc{spanId}",    ← Extract from MDC
                 "service":"${appName}",
                 "host":"${hostname}"}%nopex
            </pattern>
        </message>
        <sortByTime>true</sortByTime>
    </format>
</appender>
```

#### **Root Logger - Connect Everything**

```xml
<root level="INFO">
    <appender-ref ref="CONSOLE_JSON"/>  <!-- Logs to stdout -->
    <appender-ref ref="FILE_JSON"/>     <!-- Logs to file -->
    <appender-ref ref="LOKI"/>          <!-- Logs to Loki -->
</root>
```

---

## Runtime Flow

### **Flow Sequence Diagram**

```
┌──────────┐     ┌──────────────┐     ┌──────────┐     ┌──────────┐
│  Client  │     │    Spring    │     │   SLF4J  │     │ Logback  │
│ (GraphQL)│     │ OpenTelemetry│     │   MDC    │     │Appenders │
└────┬─────┘     └──────┬───────┘     └────┬─────┘     └────┬─────┘
     │                  │                   │                │
     │ POST /orders     │                   │                │
     ├─────────────────>│                   │                │
     │                  │                   │                │
     │                  │ 1. Create Span    │                │
     │                  │   (traceId=34a9d...)               │
     │                  │                   │                │
     │                  │ 2. Inject to MDC  │                │
     │                  ├──────────────────>│                │
     │                  │   MDC.put("traceId", "34a9d...")   │
     │                  │   MDC.put("spanId", "007d...")     │
     │                  │                   │                │
     │                  │ 3. Call Controller│                │
     │   Controller     │                   │                │
     │   log.info(...)  │                   │                │
     │  ─ ─ ─ ─ ─ ─ ─ ─>│                   │                │
     │                  │                   │                │
     │                  │ 4. Read MDC       │                │
     │                  │<──────────────────│                │
     │                  │   traceId=34a9d...│                │
     │                  │   spanId=007d...  │                │
     │                  │                   │                │
     │                  │ 5. Format JSON + Send              │
     │                  ├────────────────────────────────────>│
     │                  │   {                                 │
     │                  │     "traceId": "34a9d...",         │
     │                  │     "spanId": "007d...",           │
     │                  │     "message": "Processing..."     │
     │                  │   }                                 │
     │                  │                   │                │
     │                  │ 6. Write to destinations           │
     │                  │                   │                ├─> Console
     │                  │                   │                ├─> File
     │                  │                   │                └─> Loki
     │                  │                   │                │
     │                  │ 7. Cleanup MDC    │                │
     │                  ├──────────────────>│                │
     │                  │   MDC.clear()     │                │
     │<─────────────────┤                   │                │
     │  Response        │                   │                │
     │                  │                   │                │
```

### **Step-by-Step Explanation**

#### **Step 1: Request Arrives**
```java
// Client sends HTTP request
POST http://localhost:8081/orders
```

#### **Step 2: OpenTelemetry Creates Span**
- Spring Boot's OpenTelemetry starter **automatically intercepts** the request
- Creates a new span with:
  - `traceId`: Unique ID for the entire request flow
  - `spanId`: Unique ID for this specific operation
  - `parentSpanId`: ID of the calling span (if propagated)

#### **Step 3: Inject into MDC**
```java
// Happens automatically - no code needed!
MDC.put("traceId", "34a9d8065ac26ee97a1b969b8666a570");
MDC.put("spanId", "007d867110823eb5");
MDC.put("parentSpanId", "8f5d12e4c7a3b6d9");
```

MDC (Mapped Diagnostic Context) is **thread-local storage** - each thread has its own context.

#### **Step 4: Application Code Logs**
```java
@RestController
public class OrderController {
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    
    @PostMapping("/orders")
    public Order createOrder(@RequestBody CreateOrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        
        // Standard SLF4J logging - no trace ID handling needed!
        log.info("Received REST request to create order: ID={}, Product={}", 
                 orderId, request.getProductId());
        
        // More logging...
        log.info("Saved order to H2 database");
        
        return order;
    }
}
```

**Key Point:** The developer **never** manually handles trace IDs. Just use `log.info()` as usual!

#### **Step 5: Logback Reads MDC**
When `log.info()` is called:
1. Logback intercepts the log event
2. Reads MDC values: `traceId`, `spanId`, `parentSpanId`
3. Passes to encoders

#### **Step 6: Logstash Encoder Formats JSON**
```java
// Encoder reads MDC and creates JSON
{
  "@timestamp": "2026-01-27T11:20:09.340+06:00",
  "@version": "1",
  "level": "INFO",
  "logger": "com.example.tracing.order.OrderController",
  "message": "Received REST request to create order: ID=104b2a72-6d31-48fa-afe5-60898aff373d, Product=test-laptop",
  "traceId": "34a9d8065ac26ee97a1b969b8666a570",  ← From MDC
  "spanId": "007d867110823eb5",                    ← From MDC
  "service": "order-service",
  "host": "localhost",
  "thread": "http-nio-8081-exec-4"
}
```

#### **Step 7: Send to Destinations**
- **Console Appender** → stdout (captured by Docker)
- **File Appender** → `logs/order-service.json.log`
- **Loki Appender** → Grafana Loki (http://localhost:3100)

#### **Step 8: Cleanup**
After request completes, OpenTelemetry **automatically clears MDC**:
```java
// Automatic cleanup
MDC.clear();
```

---

## Code Examples

### Example 1: Simple Logging
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    
    public Product createProduct(ProductRequest request) {
        // Trace ID is automatically included!
        log.info("Creating product: {}", request.getName());
        
        try {
            Product product = productRepository.save(request.toEntity());
            log.info("Product created successfully: {}", product.getId());
            return product;
        } catch (Exception e) {
            log.error("Failed to create product: {}", request.getName(), e);
            throw e;
        }
    }
}
```

**Output:**
```json
{
  "timestamp": "2026-01-27T11:20:15.123Z",
  "level": "INFO",
  "logger": "com.example.tracing.product.ProductService",
  "message": "Creating product: Gaming Laptop",
  "traceId": "5db71e2ab2a7fb903606844282074579",
  "spanId": "4b9d69daedec5fd2",
  "service": "cqrs-service"
}
```

### Example 2: Structured Logging with Context
```java
log.info("Order processed: orderId={}, status={}, amount={}", 
         order.getId(), order.getStatus(), order.getAmount());
```

**Output:**
```json
{
  "message": "Order processed: orderId=104b2a72, status=COMPLETED, amount=1299.99",
  "traceId": "34a9d8065ac26ee97a1b969b8666a570",
  "spanId": "007d867110823eb5"
}
```

### Example 3: Error Logging with Stack Trace
```java
try {
    processPayment(order);
} catch (PaymentException e) {
    log.error("Payment failed for order: {}", order.getId(), e);
    throw e;
}
```

**Output:**
```json
{
  "level": "ERROR",
  "message": "Payment failed for order: 104b2a72",
  "traceId": "34a9d8065ac26ee97a1b969b8666a570",
  "stack_trace": "com.example.PaymentException: Insufficient funds\n\tat com.example.PaymentService.process(...)\n\t..."
}
```

---

## Log Destinations

### 1. **Console (stdout)**
- **Format:** JSON
- **Location:** Docker container stdout
- **View:** `docker logs <container-id>` or `tail -f /home/yaziz/.cursor/.../terminals/X.txt`

### 2. **File (Local)**
- **Format:** JSON
- **Location:** `logs/order-service.json.log`
- **Rotation:** 
  - Max size: 100MB per file
  - Max history: 30 days
  - Total cap: 10GB
- **Compressed:** `.gz` files after rotation
- **View:** `tail -f logs/order-service.json.log`

### 3. **Loki (Centralized)**
- **Format:** JSON
- **Location:** Grafana Loki (http://localhost:3100)
- **Query in Grafana:**
  ```logql
  {service="order-service"} | json
  ```
- **Filter by trace ID:**
  ```logql
  {service_name=~".+"} | json | traceId="34a9d8065ac26ee97a1b969b8666a570"
  ```

---

## Trace ID Propagation

### **HTTP Request Propagation**

#### **Outgoing Request (Client Side)**
```java
@Service
public class OrderService {
    private final WebClient webClient;
    
    public Product getProduct(String productId) {
        // OpenTelemetry automatically adds headers:
        // traceparent: 00-34a9d8065ac26ee97a1b969b8666a570-007d867110823eb5-01
        return webClient.get()
            .uri("/products/{id}", productId)
            .retrieve()
            .bodyToMono(Product.class)
            .block();
    }
}
```

#### **Incoming Request (Server Side)**
```java
@RestController
public class ProductController {
    private static final Logger log = LoggerFactory.getLogger(ProductController.class);
    
    @GetMapping("/products/{id}")
    public Product getProduct(@PathVariable String id) {
        // OpenTelemetry extracts traceparent header
        // and populates MDC automatically
        
        log.info("Fetching product: {}", id);
        // Log will contain same traceId as caller!
        
        return productService.findById(id);
    }
}
```

### **RabbitMQ Message Propagation**

#### **Publishing (Producer)**
```java
@Service
public class OrderPublisher {
    private static final Logger log = LoggerFactory.getLogger(OrderPublisher.class);
    private final RabbitTemplate rabbitTemplate;
    
    public void publishOrder(Order order) {
        log.info("Publishing order to RabbitMQ: {}", order.getOrderId());
        
        // OpenTelemetry automatically injects trace context into message headers
        rabbitTemplate.convertAndSend("orders.exchange", "orders.created", order);
    }
}
```

**Message Headers Added Automatically:**
```
traceparent: 00-34a9d8065ac26ee97a1b969b8666a570-007d867110823eb5-01
```

#### **Consuming (Consumer)**
```java
@Component
public class InventoryListener {
    private static final Logger log = LoggerFactory.getLogger(InventoryListener.class);
    
    @RabbitListener(queues = "inventory-queue")
    public void handleOrder(Order order) {
        // OpenTelemetry extracts trace context from message headers
        // and populates MDC automatically
        
        log.info("Processing order in inventory: {}", order.getOrderId());
        // Log will contain same traceId as publisher!
    }
}
```

---

## Querying Logs

### **1. Query by Service**
```logql
{service="order-service"} | json
```

### **2. Query by Trace ID**
```logql
{service_name=~".+"} | json | traceId="34a9d8065ac26ee97a1b969b8666a570"
```

### **3. Query by Log Level**
```logql
{service="order-service", level="ERROR"} | json
```

### **4. Query by Time Range + Trace ID**
```logql
{service_name=~".+"} 
| json 
| traceId="34a9d8065ac26ee97a1b969b8666a570"
| line_format "{{.timestamp}} {{.level}} {{.message}}"
```

### **5. Count Errors per Service**
```logql
sum by (service) (count_over_time({level="ERROR"}[5m]))
```

---

## Summary

### **Key Takeaways**

1. ✅ **Zero-Code Tracing**: OpenTelemetry automatically injects trace IDs into MDC
2. ✅ **Standard Logging**: Developers use regular `log.info()` - no special code needed
3. ✅ **Automatic Propagation**: Trace context propagates across HTTP and RabbitMQ
4. ✅ **Structured JSON**: All logs are JSON for easy parsing and querying
5. ✅ **Multi-Destination**: Logs go to console, file, and Loki simultaneously
6. ✅ **Production-Ready**: Log rotation, compression, and retention policies

### **Dependencies Required**
```gradle
implementation 'org.springframework.boot:spring-boot-starter-opentelemetry'
implementation 'net.logstash.logback:logstash-logback-encoder:8.0'
implementation 'com.github.loki4j:loki-logback-appender:1.4.2'
```

### **Configuration Required**
1. `application.yml` - Enable tracing and set sampling rate
2. `logback-spring.xml` - Configure appenders and JSON format
3. **No code changes needed!** Just use SLF4J logging as usual

### **The Magic**
The entire logging system works through **three automatic mechanisms**:

1. **OpenTelemetry Auto-Instrumentation**
   - Creates spans for HTTP, DB, RabbitMQ operations
   - Injects trace context into MDC

2. **MDC (Mapped Diagnostic Context)**
   - Thread-local storage for trace IDs
   - Automatically populated and cleaned up

3. **Logback MDC Integration**
   - Reads trace IDs from MDC
   - Includes them in every log entry

**Result:** Developers write simple logging code, get distributed tracing for free! 🎉

---

## Testing

### **Verify Trace IDs in Logs**
```bash
# Check if logs contain trace IDs
tail -100 logs/order-service.json.log | grep traceId

# Extract trace ID from log
cat logs/order-service.json.log | jq -r '.traceId' | head -1

# View all logs for a specific trace
cat logs/order-service.json.log | jq 'select(.traceId=="34a9d8065ac26ee97a1b969b8666a570")'
```

### **Verify in Grafana**
1. Open Grafana: http://localhost:3000
2. Go to Explore → Select "Loki"
3. Query: `{service="order-service"} | json | traceId="34a9d8065ac26ee97a1b969b8666a570"`
4. Should see all logs for that trace across all services!

---

## Troubleshooting

### **Problem: No trace IDs in logs**
**Solution:**
1. Check `management.tracing.enabled=true` in application.yml
2. Verify OpenTelemetry dependency is present
3. Check MDC configuration in logback-spring.xml

### **Problem: Trace IDs not propagating**
**Solution:**
1. For HTTP: Verify WebClient is used (not RestTemplate)
2. For RabbitMQ: Check RabbitTemplate configuration
3. Ensure OpenTelemetry is configured for both producer and consumer

### **Problem: Logs not appearing in Loki**
**Solution:**
1. Check Loki is running: `docker ps | grep loki`
2. Verify Loki URL in logback-spring.xml: `http://localhost:3100`
3. Check Loki appender logs for errors

---

**End of Document** 📝
