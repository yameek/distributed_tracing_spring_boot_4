# Comprehensive Distributed Tracing & Logging Implementation Guide

**Version:** 2.0  
**Last Updated:** January 7, 2026  
**Target:** Spring Boot 4.0.1 + Java 25 LTS  
**Author:** Implementation Guide for Microservices Observability

---

## Table of Contents

1. [Introduction & Core Concepts](#1-introduction--core-concepts)
2. [Architecture Overview](#2-architecture-overview)
3. [Prerequisites & Dependencies](#3-prerequisites--dependencies)
4. [Step-by-Step Implementation](#4-step-by-step-implementation)
5. [Advanced Configuration](#5-advanced-configuration)
6. [Best Practices](#6-best-practices)
7. [Troubleshooting](#7-troubleshooting)
8. [Testing & Verification](#8-testing--verification)

---

## 1. Introduction & Core Concepts

### 1.1 What is Distributed Tracing?

**Distributed tracing** is a method to track requests as they flow through multiple services in a microservices architecture. It helps you:

- **Understand request flow**: See how a request travels through your system
- **Identify bottlenecks**: Find which service is slow
- **Debug issues**: Trace errors back to their source
- **Monitor performance**: Measure latency at each step

### 1.2 Key Concepts

#### Trace
A **trace** represents the entire journey of a request through your system.

```
Example: User creates an order
Trace ID: 550e8400-e29b-41d4-a716-446655440000
```

#### Span
A **span** represents a single operation within a trace (e.g., a database query, HTTP call, or message processing).

```
Span 1: GraphQL mutation processing (50ms)
Span 2: HTTP call to Order Service (120ms)
Span 3: Database insert (30ms)
Span 4: RabbitMQ publish (10ms)
Span 5: Inventory update (80ms)
```

#### Trace Context
**Context** is the metadata (Trace ID, Span ID, flags) that gets propagated across service boundaries.

```
Headers:
  traceparent: 00-550e8400e29b41d4a716446655440000-1234567890abcdef-01
  tracestate: vendor1=value1,vendor2=value2
```

### 1.3 Why Implement Tracing & Logging?

| Problem | Without Tracing | With Tracing |
|---------|----------------|--------------|
| **Request fails** | Check logs in all services manually | Click on trace, see exact failure point |
| **Slow response** | Guess which service is slow | See waterfall chart with timings |
| **Error debugging** | Search logs by timestamp (unreliable) | Follow trace ID across all services |
| **Performance analysis** | No visibility into service interactions | Complete request flow visualization |

---

## 2. Architecture Overview

### 2.1 Observability Stack

```
┌─────────────────────────────────────────────────────────────┐
│                     Your Application                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ GraphQL  │  │  Order   │  │Inventory │  │Notification│  │
│  │ Service  │→ │ Service  │→ │ Service  │  │  Service   │  │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └─────┬──────┘  │
│       │             │              │               │          │
│       │ Traces      │ Traces       │ Traces        │ Traces   │
│       │ Logs        │ Logs         │ Logs          │ Logs     │
└───────┼─────────────┼──────────────┼───────────────┼─────────┘
        │             │              │               │
        ▼             ▼              ▼               ▼
   ┌────────────────────────────────────────────────────┐
   │          OpenTelemetry Collector (Optional)        │
   └───────────────────┬────────────────────────────────┘
                       │
              ┌────────┴────────┐
              ▼                 ▼
        ┌──────────┐      ┌──────────┐
        │  Tempo   │      │   Loki   │
        │ (Traces) │      │  (Logs)  │
        └────┬─────┘      └────┬─────┘
             │                 │
             └────────┬────────┘
                      ▼
                ┌──────────┐
                │ Grafana  │
                │(Visualize)│
                └──────────┘
```

### 2.2 Component Roles

| Component | Purpose | Why You Need It |
|-----------|---------|-----------------|
| **Micrometer Tracing** | Abstraction layer for tracing | Vendor-neutral API, easy to switch backends |
| **OpenTelemetry** | Tracing implementation | Industry standard, rich instrumentation |
| **Tempo** | Trace storage | Efficient, scalable trace backend |
| **Loki** | Log aggregation | Correlate logs with traces |
| **Grafana** | Visualization | Single pane of glass for observability |
| **Logback** | Logging framework | Structured logging with JSON |

---

## 3. Prerequisites & Dependencies

### 3.1 System Requirements

```bash
# Java 25 LTS (or Java 21+)
java -version
# openjdk version "25.0.1" 2026-01-21

# Maven 3.8.7+
mvn -version

# Docker & Docker Compose
docker --version
docker compose version
```

### 3.2 Maven Dependencies

#### 3.2.1 Parent POM Configuration

**Why:** Spring Boot 4.0.1 provides dependency management for all Spring and observability libraries.

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.1</version>
    <relativePath/>
</parent>

<properties>
    <java.version>25</java.version>
    <maven.compiler.source>25</maven.compiler.source>
    <maven.compiler.target>25</maven.compiler.target>
</properties>
```

#### 3.2.2 Core Tracing Dependencies

Add these to **every microservice**:

```xml
<dependencies>
    <!-- Spring Boot Actuator: Health checks, metrics endpoints -->
    <!-- WHY: Exposes /actuator/health, /actuator/metrics for monitoring -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Micrometer Tracing Bridge: Connects Micrometer to OpenTelemetry -->
    <!-- WHY: Provides vendor-neutral tracing API -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-tracing-bridge-otel</artifactId>
    </dependency>

    <!-- OpenTelemetry Exporter: Sends traces to Tempo -->
    <!-- WHY: OTLP is the standard protocol for trace export -->
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-exporter-otlp</artifactId>
    </dependency>

    <!-- Loki Logback Appender: Sends logs to Loki -->
    <!-- WHY: Centralized logging with trace correlation -->
    <dependency>
        <groupId>com.github.loki4j</groupId>
        <artifactId>loki-logback-appender</artifactId>
        <version>1.4.2</version>
    </dependency>

    <!-- Logstash Encoder: JSON structured logging -->
    <!-- WHY: Machine-readable logs with trace IDs -->
    <dependency>
        <groupId>net.logstash.logback</groupId>
        <artifactId>logstash-logback-encoder</artifactId>
        <version>8.0</version>
    </dependency>
</dependencies>
```

#### 3.2.3 Service-Specific Dependencies

**For GraphQL Service:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-graphql</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

**For REST Services (Order Service):**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

**For Message Consumers (Inventory, Notification):**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

---

## 4. Step-by-Step Implementation

### 4.1 Infrastructure Setup

#### Step 1: Create Docker Compose File

**File:** `docker-compose.yml`

**Why:** Run observability infrastructure locally without complex setup.

```yaml
version: '3.8'

services:
  # RabbitMQ: Message broker for async communication
  rabbitmq:
    image: rabbitmq:3.13-management
    container_name: tracing-demo-v2-rabbitmq-1
    ports:
      - "5672:5672"    # AMQP protocol
      - "15672:15672"  # Management UI
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    healthcheck:
      test: rabbitmq-diagnostics -q ping
      interval: 10s
      timeout: 5s
      retries: 5

  # Tempo: Distributed tracing backend
  tempo:
    image: grafana/tempo:2.9.0
    container_name: tracing-demo-v2-tempo-1
    command: [ "-config.file=/etc/tempo.yaml" ]
    volumes:
      - ./config/tempo.yaml:/etc/tempo.yaml
      - tempo-data:/tmp/tempo
    ports:
      - "3200:3200"   # Tempo HTTP API
      - "4317:4317"   # OTLP gRPC
      - "4318:4318"   # OTLP HTTP
      - "9411:9411"   # Zipkin compatibility

  # Loki: Log aggregation
  loki:
    image: grafana/loki:3.0.0
    container_name: tracing-demo-v2-loki-1
    command: -config.file=/etc/loki/local-config.yaml
    volumes:
      - ./config/loki.yaml:/etc/loki/local-config.yaml
      - loki-data:/loki
    ports:
      - "3100:3100"

  # Grafana: Visualization
  grafana:
    image: grafana/grafana:12.2.1
    container_name: tracing-demo-v2-grafana-1
    volumes:
      - ./config/datasources.yaml:/etc/grafana/provisioning/datasources/datasources.yaml
      - grafana-data:/var/lib/grafana
    environment:
      - GF_AUTH_ANONYMOUS_ENABLED=true
      - GF_AUTH_ANONYMOUS_ORG_ROLE=Admin
      - GF_SECURITY_ADMIN_PASSWORD=admin
    ports:
      - "3000:3000"
    depends_on:
      - tempo
      - loki

volumes:
  tempo-data:
  loki-data:
  grafana-data:
```

#### Step 2: Configure Tempo

**File:** `config/tempo.yaml`

**Why:** Configure trace storage, retention, and ingestion protocols.

```yaml
server:
  http_listen_port: 3200

distributor:
  receivers:
    otlp:
      protocols:
        http:
          endpoint: 0.0.0.0:4318
        grpc:
          endpoint: 0.0.0.0:4317
    zipkin:
      endpoint: 0.0.0.0:9411

ingester:
  max_block_duration: 5m

compactor:
  compaction:
    block_retention: 1h

storage:
  trace:
    backend: local
    local:
      path: /tmp/tempo/blocks
    wal:
      path: /tmp/tempo/wal

query_frontend:
  search:
    enabled: true
```

**Key Settings Explained:**

- **receivers.otlp**: Accept traces in OpenTelemetry format
- **receivers.zipkin**: Backward compatibility with Zipkin
- **max_block_duration**: How long to buffer traces before writing
- **block_retention**: How long to keep traces (1 hour for dev)
- **search.enabled**: Enable trace search in Grafana

#### Step 3: Configure Loki

**File:** `config/loki.yaml`

**Why:** Configure log ingestion, storage, and retention.

```yaml
auth_enabled: false

server:
  http_listen_port: 3100
  grpc_listen_port: 9096

common:
  instance_addr: 127.0.0.1
  path_prefix: /loki
  storage:
    filesystem:
      chunks_directory: /loki/chunks
      rules_directory: /loki/rules
  replication_factor: 1
  ring:
    kvstore:
      store: inmemory

schema_config:
  configs:
    - from: 2020-10-24
      store: tsdb
      object_store: filesystem
      schema: v13
      index:
        prefix: index_
        period: 24h

limits_config:
  retention_period: 168h  # 7 days
  ingestion_rate_mb: 16
  ingestion_burst_size_mb: 32
```

**Key Settings Explained:**

- **auth_enabled: false**: No authentication (dev only)
- **retention_period: 168h**: Keep logs for 7 days
- **ingestion_rate_mb**: Rate limiting for log ingestion
- **schema: v13**: Latest Loki schema version

#### Step 4: Configure Grafana Datasources

**File:** `config/datasources.yaml`

**Why:** Auto-configure Tempo and Loki datasources in Grafana.

```yaml
apiVersion: 1

datasources:
  - name: Tempo
    type: tempo
    access: proxy
    url: http://tempo:3200
    uid: tempo
    jsonData:
      httpMethod: GET
      tracesToLogs:
        datasourceUid: loki
        tags: ['service.name']
        mappedTags: [{ key: 'service.name', value: 'service' }]
        spanStartTimeShift: '-1h'
        spanEndTimeShift: '1h'
        filterByTraceID: true
        filterBySpanID: false

  - name: Loki
    type: loki
    access: proxy
    url: http://loki:3100
    uid: loki
    jsonData:
      derivedFields:
        - datasourceUid: tempo
          matcherRegex: "traceId=(\\w+)"
          name: TraceID
          url: "$${__value.raw}"
```

**Key Features:**

- **tracesToLogs**: Click on trace span → see related logs
- **derivedFields**: Click on log line → see related trace
- **Bidirectional navigation** between traces and logs

#### Step 5: Start Infrastructure

```bash
docker compose up -d

# Verify all containers are running
docker compose ps

# Check logs if issues
docker compose logs -f
```

---

### 4.2 Application Configuration

#### Step 1: Configure application.yml

**Why:** Configure service name, tracing, and logging for each microservice.

**Template for all services:**

```yaml
spring:
  application:
    name: ${SERVICE_NAME}  # e.g., graphql-service, order-service
  
  # RabbitMQ Configuration (for services that use it)
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

# Actuator: Expose health and metrics endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  endpoint:
    health:
      show-details: always
  
  # Tracing Configuration
  tracing:
    sampling:
      probability: 1.0  # Sample 100% of requests (use 0.1 for 10% in production)
  
  # Metrics Export
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true

# OpenTelemetry Configuration
otel:
  # Service identification
  service:
    name: ${spring.application.name}
  
  # Resource attributes (metadata about your service)
  resource:
    attributes:
      environment: development
      service.version: 1.0.0
      deployment.environment: local
  
  # Trace export configuration
  exporter:
    otlp:
      endpoint: http://localhost:4318/v1/traces
      protocol: http/protobuf
      timeout: 10s
      compression: gzip
  
  # SDK configuration
  traces:
    exporter: otlp
  
  # Propagation format (how trace context is passed)
  propagators: tracecontext,baggage

# Logging Configuration
logging:
  level:
    root: INFO
    com.example.tracing: INFO
    org.springframework.web: INFO
    org.springframework.amqp: INFO
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

**Configuration Explained:**

##### Sampling Probability
```yaml
management.tracing.sampling.probability: 1.0
```
- **1.0**: Trace 100% of requests (development)
- **0.1**: Trace 10% of requests (production)
- **0.01**: Trace 1% of requests (high-traffic production)

**Why:** Reduce overhead in production while maintaining visibility.

##### OTLP Endpoint
```yaml
otel.exporter.otlp.endpoint: http://localhost:4318/v1/traces
```
- **4318**: OTLP HTTP endpoint
- **4317**: OTLP gRPC endpoint (alternative)

**Why:** HTTP is simpler, gRPC is more efficient.

##### Propagators
```yaml
otel.propagators: tracecontext,baggage
```
- **tracecontext**: W3C Trace Context standard
- **baggage**: Propagate custom key-value pairs

**Why:** Ensure trace context is passed across all services.

##### Log Pattern
```yaml
logging.pattern.level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```
Output: `INFO [order-service,550e8400e29b41d4a716446655440000,1234567890abcdef]`

**Why:** Include trace IDs in every log line for correlation.

#### Step 2: Configure Logback for Structured Logging

**File:** `src/main/resources/logback-spring.xml`

**Why:** Send logs to both console (development) and Loki (centralized).

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty scope="context" name="serviceName" source="spring.application.name"/>
    
    <!-- Console Appender: Human-readable logs for development -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service":"${serviceName}"}</customFields>
            <fieldNames>
                <timestamp>@timestamp</timestamp>
                <version>@version</version>
                <message>message</message>
                <logger>logger</logger>
                <thread>thread</thread>
                <level>level</level>
            </fieldNames>
            <!-- Include MDC fields (traceId, spanId) -->
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
        </encoder>
    </appender>
    
    <!-- File Appender: JSON logs to file -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/${serviceName}.json.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/${serviceName}.json.log.%d{yyyy-MM-dd}.gz</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service":"${serviceName}","host":"localhost"}</customFields>
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
        </encoder>
    </appender>
    
    <!-- Loki Appender: Send logs to Loki -->
    <appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
        <http>
            <url>http://localhost:3100/loki/api/v1/push</url>
        </http>
        <format>
            <label>
                <pattern>service=${serviceName},host=localhost,level=%level</pattern>
            </label>
            <message>
                <pattern>
                    {
                    "timestamp":"%d{ISO8601}",
                    "level":"%level",
                    "thread":"%thread",
                    "logger":"%logger{36}",
                    "message":"%message",
                    "traceId":"%X{traceId:-}",
                    "spanId":"%X{spanId:-}"
                    }
                </pattern>
            </message>
        </format>
    </appender>
    
    <!-- Async wrapper for better performance -->
    <appender name="ASYNC_LOKI" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="LOKI"/>
        <queueSize>1000</queueSize>
        <discardingThreshold>0</discardingThreshold>
    </appender>
    
    <!-- Root logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
        <appender-ref ref="ASYNC_LOKI"/>
    </root>
    
    <!-- Reduce noise from Spring internals -->
    <logger name="org.springframework" level="WARN"/>
    <logger name="org.hibernate" level="WARN"/>
    
    <!-- Your application packages -->
    <logger name="com.example.tracing" level="INFO"/>
</configuration>
```

**Appender Purposes:**

| Appender | Purpose | When to Use |
|----------|---------|-------------|
| **CONSOLE** | Human-readable logs | Development, debugging |
| **FILE** | Persistent JSON logs | Local analysis, backup |
| **LOKI** | Centralized logging | Production, multi-service correlation |
| **ASYNC_LOKI** | Non-blocking Loki | Prevent logging from slowing app |

**MDC (Mapped Diagnostic Context):**
- Automatically populated by Micrometer with `traceId` and `spanId`
- Included in every log line
- Enables log-to-trace correlation

---

### 4.3 Code Implementation

#### 4.3.1 GraphQL Service (Entry Point)

**File:** `GraphqlServiceApplication.java`

```java
package com.example.tracing.graphql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class GraphqlServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(GraphqlServiceApplication.class, args);
    }
    
    /**
     * WebClient for making HTTP calls to other services.
     * Automatically instrumented for tracing by Micrometer.
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
```

**File:** `OrderController.java`

```java
package com.example.tracing.graphql;

import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
public class OrderController {
    
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderClient orderClient;
    private final Tracer tracer;  // Optional: For manual span manipulation
    
    public OrderController(OrderClient orderClient, Tracer tracer) {
        this.orderClient = orderClient;
        this.tracer = tracer;
    }
    
    /**
     * GraphQL mutation to create an order.
     * 
     * @Observed: Automatically creates a span for this method
     * - name: Custom span name (default: method name)
     * - contextualName: Business-friendly name for the operation
     */
    @MutationMapping
    @Observed(
        name = "graphql.createOrder",
        contextualName = "create-order-mutation"
    )
    public Order createOrder(
            @Argument String productId,
            @Argument int quantity) {
        
        // Log with automatic trace ID inclusion
        log.info("Received GraphQL mutation createOrder: {} x {}", quantity, productId);
        
        // Optional: Get current trace ID for business logic
        String traceId = tracer.currentSpan() != null 
            ? tracer.currentSpan().context().traceId() 
            : "no-trace";
        
        // Call downstream service (automatically traced)
        return orderClient.createOrder(productId, quantity);
    }
}
```

**Why Use @Observed:**
- ✅ Automatic span creation
- ✅ Captures method parameters as span attributes
- ✅ Records exceptions automatically
- ✅ Measures execution time

**File:** `OrderClient.java`

```java
package com.example.tracing.graphql;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class OrderClient {
    
    private static final Logger log = LoggerFactory.getLogger(OrderClient.class);
    
    private final WebClient webClient;
    
    public OrderClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
            .baseUrl("http://localhost:8081")
            .build();
    }
    
    /**
     * Make HTTP call to Order Service.
     * WebClient is automatically instrumented by Micrometer.
     * Trace context is propagated via HTTP headers.
     */
    @Observed(name = "order.client.create")
    public Order createOrder(String productId, int quantity) {
        log.info("Sending order creation request to order-service for product: {}", productId);
        
        OrderRequest request = new OrderRequest(productId, quantity);
        
        return webClient.post()
            .uri("/orders")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Order.class)
            .block();  // Blocking for simplicity (use reactive in production)
    }
}
```

**Automatic HTTP Header Propagation:**

When `WebClient` makes a call, Micrometer automatically adds:
```
traceparent: 00-550e8400e29b41d4a716446655440000-1234567890abcdef-01
```

This ensures the downstream service continues the same trace.

#### 4.3.2 Order Service (REST + Database)

**File:** `OrderController.java`

```java
package com.example.tracing.order;

import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {
    
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderRepository orderRepository;
    private final OrderPublisher orderPublisher;
    private final Tracer tracer;
    
    public OrderController(OrderRepository orderRepository, 
                          OrderPublisher orderPublisher,
                          Tracer tracer) {
        this.orderRepository = orderRepository;
        this.orderPublisher = orderPublisher;
        this.tracer = tracer;
    }
    
    /**
     * REST endpoint to create an order.
     * Automatically traced by Spring Web instrumentation.
     */
    @PostMapping
    @Observed(name = "order.create", contextualName = "create-order")
    public OrderResponse createOrder(@RequestBody OrderRequest request) {
        
        String orderId = UUID.randomUUID().toString();
        
        log.info("Received REST request to create order: ID={}, Product={}", 
                 orderId, request.getProductId());
        
        // Add custom span attributes for better observability
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            currentSpan.tag("order.id", orderId);
            currentSpan.tag("product.id", request.getProductId());
            currentSpan.tag("order.quantity", String.valueOf(request.getQuantity()));
            currentSpan.event("order.validation.started");
        }
        
        // Database operation (automatically traced by Spring Data JPA)
        OrderEntity entity = new OrderEntity();
        entity.setOrderId(orderId);
        entity.setProductId(request.getProductId());
        entity.setQuantity(request.getQuantity());
        entity.setStatus("CREATED");
        
        orderRepository.save(entity);
        log.info("Saved order to H2 database");
        
        if (currentSpan != null) {
            currentSpan.event("order.saved.to.database");
        }
        
        // Publish to RabbitMQ (trace context propagated automatically)
        orderPublisher.publishOrder(orderId, request.getProductId(), request.getQuantity());
        
        return new OrderResponse(orderId, "CREATED", "Order accepted for " + request.getProductId());
    }
}
```

**Custom Span Attributes:**

```java
currentSpan.tag("order.id", orderId);
currentSpan.tag("product.id", request.getProductId());
```

**Why:** 
- Search traces by order ID or product ID in Grafana
- Filter traces by specific business attributes
- Debug specific orders quickly

**Span Events:**

```java
currentSpan.event("order.validation.started");
currentSpan.event("order.saved.to.database");
```

**Why:**
- Mark important milestones within a span
- See timeline of events in trace visualization
- Identify where time is spent

**File:** `OrderPublisher.java`

```java
package com.example.tracing.order;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderPublisher {
    
    private static final Logger log = LoggerFactory.getLogger(OrderPublisher.class);
    
    private final RabbitTemplate rabbitTemplate;
    
    public OrderPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }
    
    /**
     * Publish order to RabbitMQ.
     * RabbitTemplate is automatically instrumented.
     * Trace context is propagated via AMQP message headers.
     */
    @Observed(name = "order.publish", contextualName = "publish-order-to-rabbitmq")
    public void publishOrder(String orderId, String productId, int quantity) {
        log.info("Publishing order to RabbitMQ: {}", orderId);
        
        OrderMessage message = new OrderMessage(orderId, productId, quantity);
        
        // Trace context automatically added to message headers
        rabbitTemplate.convertAndSend("orders.exchange", "", message);
    }
}
```

**Automatic AMQP Header Propagation:**

RabbitTemplate adds trace headers to the message:
```
headers: {
  "traceparent": "00-550e8400e29b41d4a716446655440000-1234567890abcdef-01",
  "tracestate": "..."
}
```

#### 4.3.3 Inventory Service (Message Consumer)

**File:** `OrderListener.java`

```java
package com.example.tracing.inventory;

import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderListener {
    
    private static final Logger log = LoggerFactory.getLogger(OrderListener.class);
    
    private final Tracer tracer;
    
    public OrderListener(Tracer tracer) {
        this.tracer = tracer;
    }
    
    /**
     * Listen for orders from RabbitMQ.
     * @RabbitListener is automatically instrumented.
     * Trace context is extracted from AMQP message headers.
     */
    @RabbitListener(queues = "orders.queue")
    @Observed(name = "inventory.update", contextualName = "update-inventory-from-order")
    public void handleOrder(OrderMessage message) {
        
        // This log will have the SAME trace ID as the original request!
        log.info("Received order from RabbitMQ: {}", message.getOrderId());
        
        // Add custom attributes
        if (tracer.currentSpan() != null) {
            tracer.currentSpan().tag("order.id", message.getOrderId());
            tracer.currentSpan().tag("product.id", message.getProductId());
        }
        
        // Simulate inventory update
        try {
            Thread.sleep(100);  // Simulate processing time
            log.info("Inventory updated for order: {}", message.getOrderId());
        } catch (InterruptedException e) {
            log.error("Error updating inventory", e);
            Thread.currentThread().interrupt();
        }
    }
}
```

**Trace Context Flow:**

```
1. GraphQL Service creates trace (Trace ID: 550e8400...)
2. Order Service continues trace (same Trace ID)
3. RabbitMQ message includes trace headers
4. Inventory Service extracts trace context
5. Inventory Service continues SAME trace (Trace ID: 550e8400...)
```

**Result:** One continuous trace across all services!

#### 4.3.4 Notification Service (Parallel Consumer)

**File:** `NotificationListener.java`

```java
package com.example.tracing.notification;

import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);
    
    private final Tracer tracer;
    
    public NotificationListener(Tracer tracer) {
        this.tracer = tracer;
    }
    
    /**
     * Listen for orders and send notifications.
     * Runs in parallel with Inventory Service.
     */
    @RabbitListener(queues = "notifications.queue")
    @Observed(name = "notification.send", contextualName = "send-order-notification")
    public void handleOrder(OrderMessage message) {
        
        log.info("📧 Notification Service received order: {}", message.getOrderId());
        
        if (tracer.currentSpan() != null) {
            tracer.currentSpan().tag("order.id", message.getOrderId());
            tracer.currentSpan().tag("notification.type", "email");
        }
        
        // Simulate email sending
        try {
            Thread.sleep(150);
            log.info("✅ Email sent for order: {}", message.getOrderId());
        } catch (InterruptedException e) {
            log.error("Error sending notification", e);
            Thread.currentThread().interrupt();
        }
    }
}
```

**Parallel Processing:**

Both Inventory and Notification services process the same message:
- Both continue the SAME trace
- Both show as parallel spans in Grafana
- You can see fan-out pattern in trace visualization

---

### 4.4 RabbitMQ Configuration

**File:** `RabbitMQConfig.java` (in Order Service)

```java
package com.example.tracing.order;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    public static final String EXCHANGE_NAME = "orders.exchange";
    public static final String INVENTORY_QUEUE = "orders.queue";
    public static final String NOTIFICATION_QUEUE = "notifications.queue";
    
    /**
     * Fan-out exchange: Sends messages to all bound queues.
     * Perfect for event-driven architectures.
     */
    @Bean
    public FanoutExchange ordersExchange() {
        return new FanoutExchange(EXCHANGE_NAME, true, false);
    }
    
    /**
     * Queue for inventory updates.
     */
    @Bean
    public Queue inventoryQueue() {
        return new Queue(INVENTORY_QUEUE, true);
    }
    
    /**
     * Queue for notifications.
     */
    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }
    
    /**
     * Bind inventory queue to exchange.
     */
    @Bean
    public Binding inventoryBinding(Queue inventoryQueue, FanoutExchange ordersExchange) {
        return BindingBuilder.bind(inventoryQueue).to(ordersExchange);
    }
    
    /**
     * Bind notification queue to exchange.
     */
    @Bean
    public Binding notificationBinding(Queue notificationQueue, FanoutExchange ordersExchange) {
        return BindingBuilder.bind(notificationQueue).to(ordersExchange);
    }
}
```

**Why Fan-out Exchange:**
- One message → Multiple consumers
- Demonstrates parallel processing in traces
- Realistic microservices pattern

---

## 5. Advanced Configuration

### 5.1 Custom Span Creation

**When to use:** When automatic instrumentation isn't enough.

```java
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

@Component
public class PaymentService {
    
    private final Tracer tracer;
    
    public PaymentService(Tracer tracer) {
        this.tracer = tracer;
    }
    
    public void processPayment(String orderId, double amount) {
        // Create a custom span
        Span paymentSpan = tracer.nextSpan().name("payment.process");
        
        try (Tracer.SpanInScope ws = tracer.withSpan(paymentSpan.start())) {
            // Add attributes
            paymentSpan.tag("order.id", orderId);
            paymentSpan.tag("payment.amount", String.valueOf(amount));
            paymentSpan.tag("payment.method", "credit_card");
            
            // Your business logic
            callPaymentGateway(orderId, amount);
            
            // Add event
            paymentSpan.event("payment.authorized");
            
        } catch (Exception e) {
            // Record exception
            paymentSpan.error(e);
            throw e;
        } finally {
            // Always end the span
            paymentSpan.end();
        }
    }
}
```

### 5.2 Baggage Propagation

**Use case:** Propagate business context across services.

```java
import io.micrometer.tracing.Baggage;
import io.micrometer.tracing.Tracer;

@Component
public class UserContextService {
    
    private final Tracer tracer;
    
    public UserContextService(Tracer tracer) {
        this.tracer = tracer;
    }
    
    public void setUserContext(String userId, String tenantId) {
        // Add baggage (propagated to all downstream services)
        Baggage.create(tracer.currentTraceContext(), "user.id", userId);
        Baggage.create(tracer.currentTraceContext(), "tenant.id", tenantId);
    }
    
    public String getUserId() {
        return Baggage.current(tracer.currentTraceContext(), "user.id");
    }
}
```

**Why Baggage:**
- Propagate user ID, tenant ID, correlation ID
- Available in all downstream services
- Useful for multi-tenant applications

### 5.3 Sampling Strategies

**Production configuration:**

```yaml
management:
  tracing:
    sampling:
      probability: 0.1  # Sample 10% of requests
```

**Custom sampler:**

```java
import io.micrometer.tracing.SamplerFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfig {
    
    /**
     * Custom sampler: Always sample errors, 10% for success.
     */
    @Bean
    public SamplerFunction<Object> customSampler() {
        return (request) -> {
            // Always sample if error
            if (isError(request)) {
                return true;
            }
            // Sample 10% of normal requests
            return Math.random() < 0.1;
        };
    }
}
```

### 5.4 Performance Optimization

#### Async Logging

```xml
<!-- logback-spring.xml -->
<appender name="ASYNC_LOKI" class="ch.qos.logback.classic.AsyncAppender">
    <appender-ref ref="LOKI"/>
    <queueSize>1000</queueSize>
    <discardingThreshold>0</discardingThreshold>
    <neverBlock>true</neverBlock>
</appender>
```

**Why:**
- Non-blocking logging
- Prevents slow logging from affecting response time
- Essential for high-throughput applications

#### Batch Export

```yaml
otel:
  exporter:
    otlp:
      endpoint: http://localhost:4318/v1/traces
      timeout: 10s
      compression: gzip  # Reduce network bandwidth
  
  # Batch span processor (better performance)
  traces:
    exporter: otlp
    processor:
      batch:
        scheduleDelay: 5000  # Export every 5 seconds
        maxQueueSize: 2048
        maxExportBatchSize: 512
```

---

## 6. Best Practices

### 6.1 Naming Conventions

#### Span Names

**Good:**
```java
@Observed(name = "order.create")
@Observed(name = "payment.process")
@Observed(name = "inventory.update")
```

**Bad:**
```java
@Observed(name = "doSomething")
@Observed(name = "method1")
@Observed(name = "processData")
```

**Rules:**
- Use `resource.action` format
- Lowercase with dots
- Descriptive and consistent

#### Log Messages

**Good:**
```java
log.info("Order created: orderId={}, productId={}, quantity={}", 
         orderId, productId, quantity);
```

**Bad:**
```java
log.info("Order created");  // No context
log.info("Creating order for " + productId);  // String concatenation
```

**Rules:**
- Include relevant IDs
- Use parameterized logging
- Be specific and actionable

### 6.2 What to Trace

#### Always Trace
✅ HTTP requests/responses  
✅ Database queries  
✅ Message publishing/consuming  
✅ External API calls  
✅ Business-critical operations  

#### Consider Tracing
⚠️ Complex calculations  
⚠️ File I/O operations  
⚠️ Cache operations  
⚠️ Background jobs  

#### Don't Trace
❌ Getters/setters  
❌ Simple utility methods  
❌ High-frequency loops  
❌ Internal helper methods  

### 6.3 Span Attributes

**Essential attributes:**

```java
span.tag("order.id", orderId);              // Business ID
span.tag("user.id", userId);                // User context
span.tag("http.status_code", "200");        // Response status
span.tag("db.statement", "INSERT INTO..."); // SQL query
span.tag("error", "true");                  // Error flag
```

**Avoid:**
- Sensitive data (passwords, tokens, PII)
- Large payloads (>1KB)
- High-cardinality values (timestamps, UUIDs as values)

### 6.4 Error Handling

**Always record exceptions:**

```java
try {
    processOrder(orderId);
} catch (Exception e) {
    // Record exception in span
    if (tracer.currentSpan() != null) {
        tracer.currentSpan().error(e);
    }
    // Log with trace context
    log.error("Failed to process order: {}", orderId, e);
    throw e;
}
```

### 6.5 Log Levels

| Level | When to Use | Example |
|-------|-------------|---------|
| **ERROR** | Unexpected errors, exceptions | `log.error("Failed to process payment", e)` |
| **WARN** | Recoverable issues, deprecations | `log.warn("Retry attempt {} failed", attempt)` |
| **INFO** | Important business events | `log.info("Order created: {}", orderId)` |
| **DEBUG** | Detailed diagnostic info | `log.debug("Validating order: {}", order)` |
| **TRACE** | Very detailed, high-volume | `log.trace("Processing item: {}", item)` |

---

## 7. Troubleshooting

### 7.1 Common Issues

#### Issue: Traces not appearing in Tempo

**Symptoms:**
- Services are running
- No traces in Grafana

**Solutions:**

1. **Check OTLP endpoint:**
```bash
# Test if Tempo is accepting traces
curl -v http://localhost:4318/v1/traces
```

2. **Verify sampling:**
```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # Ensure 100% sampling for testing
```

3. **Check application logs:**
```bash
grep -i "trace" logs/order-service.log
grep -i "otlp" logs/order-service.log
```

4. **Verify dependencies:**
```bash
mvn dependency:tree | grep -i "micrometer\|opentelemetry"
```

#### Issue: Trace context not propagating

**Symptoms:**
- Each service has different trace ID
- Broken trace chain

**Solutions:**

1. **Check propagators configuration:**
```yaml
otel:
  propagators: tracecontext,baggage
```

2. **Verify HTTP headers:**
```java
// In downstream service
@GetMapping("/orders")
public String getOrders(@RequestHeader Map<String, String> headers) {
    log.info("Headers: {}", headers);
    // Look for 'traceparent' header
}
```

3. **Check WebClient configuration:**
```java
@Bean
public WebClient.Builder webClientBuilder() {
    return WebClient.builder();  // Must use builder, not create()
}
```

#### Issue: Logs not appearing in Loki

**Symptoms:**
- Services are running
- No logs in Grafana

**Solutions:**

1. **Test Loki endpoint:**
```bash
curl -v http://localhost:3100/ready
```

2. **Check Logback configuration:**
```bash
# Verify logback-spring.xml exists
ls -la src/main/resources/logback-spring.xml

# Check for syntax errors
mvn validate
```

3. **Test log ingestion:**
```bash
curl -X POST http://localhost:3100/loki/api/v1/push \
  -H "Content-Type: application/json" \
  -d '{"streams":[{"stream":{"service":"test"},"values":[["'$(date +%s)000000000'","test message"]]}]}'
```

4. **Check async appender queue:**
```xml
<appender name="ASYNC_LOKI" class="ch.qos.logback.classic.AsyncAppender">
    <queueSize>1000</queueSize>
    <discardingThreshold>0</discardingThreshold>
    <neverBlock>false</neverBlock>  <!-- Set to false for debugging -->
</appender>
```

#### Issue: High memory usage

**Symptoms:**
- Services consuming too much memory
- OutOfMemoryError

**Solutions:**

1. **Reduce sampling:**
```yaml
management:
  tracing:
    sampling:
      probability: 0.1  # Sample only 10%
```

2. **Configure span limits:**
```yaml
otel:
  traces:
    processor:
      batch:
        maxQueueSize: 512  # Reduce from 2048
        maxExportBatchSize: 128  # Reduce from 512
```

3. **Limit log retention:**
```xml
<rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
    <fileNamePattern>logs/${serviceName}.%d{yyyy-MM-dd}.gz</fileNamePattern>
    <maxHistory>3</maxHistory>  <!-- Keep only 3 days -->
</rollingPolicy>
```

### 7.2 Debugging Tips

#### Enable Debug Logging

```yaml
logging:
  level:
    io.micrometer.tracing: DEBUG
    io.opentelemetry: DEBUG
    com.github.loki4j: DEBUG
```

#### Verify Trace Context

```java
@Component
public class TraceDebugger {
    
    private final Tracer tracer;
    
    public TraceDebugger(Tracer tracer) {
        this.tracer = tracer;
    }
    
    public void logTraceContext() {
        Span span = tracer.currentSpan();
        if (span != null) {
            log.info("Trace ID: {}", span.context().traceId());
            log.info("Span ID: {}", span.context().spanId());
            log.info("Sampled: {}", span.context().sampled());
        } else {
            log.warn("No active span!");
        }
    }
}
```

#### Test Trace Export

```java
@SpringBootTest
class TracingTest {
    
    @Autowired
    private Tracer tracer;
    
    @Test
    void testTraceCreation() {
        Span span = tracer.nextSpan().name("test.span");
        try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
            assertNotNull(span.context().traceId());
            assertNotNull(span.context().spanId());
        } finally {
            span.end();
        }
    }
}
```

---

## 8. Testing & Verification

### 8.1 Manual Testing

#### Step 1: Start All Services

```bash
cd tracing-demo-v2
docker compose up -d
bash run_all.sh
```

#### Step 2: Send Test Request

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation { createOrder(productId: \"laptop\", quantity: 2) { orderId status message } }"
  }' | jq .
```

Expected response:
```json
{
  "data": {
    "createOrder": {
      "orderId": "550e8400-e29b-41d4-a716-446655440000",
      "status": "CREATED",
      "message": "Order accepted for laptop"
    }
  }
}
```

#### Step 3: Verify in Grafana

1. Open Grafana: http://localhost:3000
2. Go to **Explore** → **Tempo**
3. Click **Search** → **Run Query**
4. Click on the latest trace

**What to verify:**
- ✅ Trace has 6+ spans
- ✅ GraphQL → Order → Database → RabbitMQ → Inventory + Notification
- ✅ All spans have the same trace ID
- ✅ Timings are reasonable
- ✅ Custom attributes are present

#### Step 4: Verify Log Correlation

1. In Grafana, click on a span
2. Click **Logs for this span**
3. Verify logs appear with matching trace ID

### 8.2 Automated Testing

#### Integration Test

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
class OrderServiceIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private Tracer tracer;
    
    @Test
    void testOrderCreationWithTracing() {
        // Create order
        OrderRequest request = new OrderRequest("laptop", 2);
        
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
            "/orders", 
            request, 
            OrderResponse.class
        );
        
        // Verify response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getOrderId());
        
        // Verify trace was created
        Span span = tracer.currentSpan();
        assertNotNull(span);
        assertNotNull(span.context().traceId());
    }
}
```

#### Load Testing

```bash
# Install Apache Bench
sudo apt-get install apache2-utils

# Send 1000 requests with 10 concurrent connections
ab -n 1000 -c 10 -p order.json -T application/json \
   http://localhost:8081/orders
```

**Verify:**
- ✅ All traces appear in Tempo
- ✅ No errors in service logs
- ✅ Memory usage is stable
- ✅ Response times are acceptable

### 8.3 Production Readiness Checklist

Before deploying to production:

#### Configuration
- [ ] Sampling rate set appropriately (0.01 - 0.1)
- [ ] Log levels set to INFO or WARN
- [ ] Sensitive data excluded from spans
- [ ] Async logging enabled
- [ ] Resource limits configured

#### Infrastructure
- [ ] Tempo has persistent storage
- [ ] Loki has retention policy
- [ ] Grafana has authentication enabled
- [ ] Backups configured
- [ ] Monitoring alerts set up

#### Testing
- [ ] Load testing completed
- [ ] Trace propagation verified
- [ ] Error scenarios tested
- [ ] Failover scenarios tested
- [ ] Performance benchmarks met

#### Documentation
- [ ] Runbooks created
- [ ] Alert procedures documented
- [ ] Team trained on Grafana
- [ ] Incident response plan ready

---

## Appendix A: Complete Example Service

See the `tracing-demo-v2` directory for complete, working examples of:
- GraphQL Service
- Order Service (REST + JPA)
- Inventory Service (RabbitMQ consumer)
- Notification Service (RabbitMQ consumer)

## Appendix B: Useful Commands

```bash
# Start infrastructure
docker compose up -d

# Start all services
bash run_all.sh

# Stop all services
bash stop_all.sh

# View logs
tail -f logs/*.log

# Test GraphQL endpoint
bash test_system.sh

# Check service health
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health

# View Tempo traces
curl http://localhost:3200/api/search | jq .

# View Loki logs
curl -G http://localhost:3100/loki/api/v1/query \
  --data-urlencode 'query={service="order-service"}' | jq .
```

## Appendix C: Further Reading

- [Micrometer Tracing Documentation](https://micrometer.io/docs/tracing)
- [OpenTelemetry Java Documentation](https://opentelemetry.io/docs/languages/java/)
- [Grafana Tempo Documentation](https://grafana.com/docs/tempo/latest/)
- [Grafana Loki Documentation](https://grafana.com/docs/loki/latest/)
- [W3C Trace Context Specification](https://www.w3.org/TR/trace-context/)

---

**End of Implementation Guide**

*This guide is maintained as part of the tracing-demo-v2 project.*  
*Last updated: January 7, 2026*
