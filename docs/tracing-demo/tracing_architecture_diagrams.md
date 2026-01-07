# Architecture Diagrams - Distributed Tracing System

**Visual Guide to Understanding the Tracing Architecture**

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Trace Flow](#2-trace-flow)
3. [Component Interactions](#3-component-interactions)
4. [Data Flow](#4-data-flow)
5. [Deployment Architecture](#5-deployment-architecture)

---

## 1. System Overview

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CLIENT / USER                                │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                │ GraphQL Mutation
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      APPLICATION LAYER                               │
│                                                                       │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐            │
│  │  GraphQL    │───▶│   Order     │───▶│  RabbitMQ   │            │
│  │  Service    │HTTP│  Service    │AMQP│   Broker    │            │
│  │   :8080     │    │   :8081     │    │   :5672     │            │
│  └─────────────┘    └─────────────┘    └──────┬──────┘            │
│         │                   │                   │                    │
│         │                   │                   │ Fan-out            │
│         │                   │                   │                    │
│         │                   │            ┌──────┴──────┐            │
│         │                   │            │             │             │
│         │                   │            ▼             ▼             │
│         │                   │    ┌─────────────┐ ┌─────────────┐   │
│         │                   │    │ Inventory   │ │Notification │   │
│         │                   │    │  Service    │ │  Service    │   │
│         │                   │    │   :8082     │ │   :8083     │   │
│         │                   │    └─────────────┘ └─────────────┘   │
│         │                   │                                        │
│         │                   ▼                                        │
│         │            ┌─────────────┐                                │
│         │            │     H2      │                                │
│         │            │  Database   │                                │
│         │            └─────────────┘                                │
└─────────┼────────────────────┼──────────────────────────────────────┘
          │                    │
          │ Traces + Logs      │ Traces + Logs
          │                    │
          ▼                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    OBSERVABILITY LAYER                               │
│                                                                       │
│  ┌─────────────┐         ┌─────────────┐         ┌─────────────┐  │
│  │    Tempo    │         │    Loki     │         │   Grafana   │  │
│  │   (Traces)  │◀────────│   (Logs)    │────────▶│ (Visualize) │  │
│  │   :3200     │         │   :3100     │         │   :3000     │  │
│  └─────────────┘         └─────────────┘         └─────────────┘  │
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘
```

---

## 2. Trace Flow

### Request Journey with Trace Propagation

```
┌──────────────────────────────────────────────────────────────────────┐
│ Step 1: Client Request                                               │
│                                                                        │
│  POST /graphql                                                        │
│  mutation { createOrder(productId: "laptop", quantity: 2) }          │
└────────────────────────────┬───────────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────────┐
│ Step 2: GraphQL Service (Trace Starts Here)                         │
│                                                                        │
│  ┌────────────────────────────────────────────────────────────┐     │
│  │ Trace ID: 550e8400-e29b-41d4-a716-446655440000             │     │
│  │ Span ID:  1234567890abcdef                                  │     │
│  │ Span Name: graphql.createOrder                              │     │
│  │ Duration: 250ms                                              │     │
│  └────────────────────────────────────────────────────────────┘     │
│                                                                        │
│  Actions:                                                             │
│  ✓ Create root span                                                  │
│  ✓ Log: "Received GraphQL mutation"                                 │
│  ✓ Call Order Service via HTTP                                      │
└────────────────────────────┬───────────────────────────────────────────┘
                             │
                             │ HTTP Headers:
                             │ traceparent: 00-550e8400...-1234567890...-01
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────────┐
│ Step 3: Order Service (Continues Trace)                             │
│                                                                        │
│  ┌────────────────────────────────────────────────────────────┐     │
│  │ Trace ID: 550e8400-e29b-41d4-a716-446655440000  (SAME!)    │     │
│  │ Span ID:  abcdef1234567890                                  │     │
│  │ Parent:   1234567890abcdef                                  │     │
│  │ Span Name: order.create                                     │     │
│  │ Duration: 180ms                                              │     │
│  └────────────────────────────────────────────────────────────┘     │
│                                                                        │
│  Actions:                                                             │
│  ✓ Extract trace context from headers                               │
│  ✓ Create child span                                                │
│  ✓ Save to database (creates DB span)                               │
│  ✓ Publish to RabbitMQ                                              │
└────────────────────────────┬───────────────────────────────────────────┘
                             │
                             │ AMQP Message Headers:
                             │ traceparent: 00-550e8400...-abcdef1234...-01
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────────┐
│ Step 4: RabbitMQ (Trace Context in Message)                         │
│                                                                        │
│  ┌────────────────────────────────────────────────────────────┐     │
│  │ Exchange: orders.exchange                                   │     │
│  │ Type: fanout                                                 │     │
│  │ Message Headers:                                             │     │
│  │   - traceparent: 00-550e8400...-abcdef1234...-01           │     │
│  │   - tracestate: ...                                          │     │
│  └────────────────────────────────────────────────────────────┘     │
│                                                                        │
│  Actions:                                                             │
│  ✓ Route message to bound queues                                    │
│  ✓ Preserve trace headers                                           │
└────────────────┬───────────────────────────┬─────────────────────────┘
                 │                           │
                 │ Fan-out to 2 queues       │
                 │                           │
        ┌────────▼────────┐         ┌───────▼────────┐
        │ orders.queue    │         │notifications.  │
        │                 │         │queue           │
        └────────┬────────┘         └───────┬────────┘
                 │                           │
                 ▼                           ▼
┌────────────────────────────┐  ┌───────────────────────────┐
│ Step 5a: Inventory Service │  │ Step 5b: Notification Svc │
│                             │  │                            │
│ ┌─────────────────────────┐│  │┌─────────────────────────┐│
│ │ Trace ID: 550e8400...   ││  ││ Trace ID: 550e8400...   ││
│ │ Span ID:  fedcba098765  ││  ││ Span ID:  567890abcdef  ││
│ │ Parent:   abcdef1234... ││  ││ Parent:   abcdef1234... ││
│ │ Span: inventory.update  ││  ││ Span: notification.send ││
│ │ Duration: 100ms         ││  ││ Duration: 150ms         ││
│ └─────────────────────────┘│  │└─────────────────────────┘│
│                             │  │                            │
│ Actions:                    │  │ Actions:                   │
│ ✓ Extract trace from msg   │  │ ✓ Extract trace from msg  │
│ ✓ Update inventory         │  │ ✓ Send email notification │
│ ✓ Log completion           │  │ ✓ Log completion          │
└─────────────────────────────┘  └────────────────────────────┘

         │                                  │
         │ Export traces                   │ Export traces
         │                                  │
         └──────────────┬───────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────────────┐
│ Step 6: Tempo (Trace Storage)                                       │
│                                                                        │
│  Complete Trace Tree:                                                │
│                                                                        │
│  graphql.createOrder (250ms)                                         │
│  └── order.create (180ms)                                            │
│      ├── jdbc.insert (30ms)                                          │
│      ├── rabbitmq.publish (10ms)                                     │
│      ├── inventory.update (100ms)  ← Parallel                       │
│      └── notification.send (150ms) ← Parallel                       │
│                                                                        │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 3. Component Interactions

### Micrometer Tracing Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                      YOUR APPLICATION CODE                            │
│                                                                        │
│  @Observed                                                            │
│  public void createOrder() { ... }                                   │
│                                                                        │
└────────────────────────────┬─────────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    MICROMETER TRACING API                             │
│                    (Vendor-Neutral Abstraction)                       │
│                                                                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                 │
│  │   Tracer    │  │    Span     │  │  Baggage    │                 │
│  │  Interface  │  │  Interface  │  │  Interface  │                 │
│  └─────────────┘  └─────────────┘  └─────────────┘                 │
│                                                                        │
└────────────────────────────┬─────────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────────┐
│              MICROMETER TRACING BRIDGE (OTEL)                        │
│              Translates Micrometer → OpenTelemetry                   │
│                                                                        │
└────────────────────────────┬─────────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────────┐
│                   OPENTELEMETRY SDK                                   │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │                    Trace Context                          │       │
│  │  - Trace ID generation                                    │       │
│  │  - Span ID generation                                     │       │
│  │  - Context propagation                                    │       │
│  └──────────────────────────────────────────────────────────┘       │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │                  Span Processor                           │       │
│  │  - Batch spans for efficiency                             │       │
│  │  - Add resource attributes                                │       │
│  │  - Sampling decisions                                     │       │
│  └──────────────────────────────────────────────────────────┘       │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │                Context Propagators                        │       │
│  │  - W3C Trace Context (traceparent header)                │       │
│  │  - Baggage propagation                                    │       │
│  └──────────────────────────────────────────────────────────┘       │
│                                                                        │
└────────────────────────────┬─────────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────────┐
│                     OTLP EXPORTER                                     │
│                                                                        │
│  Protocol: HTTP/Protobuf                                             │
│  Endpoint: http://localhost:4318/v1/traces                           │
│  Compression: gzip                                                    │
│                                                                        │
└────────────────────────────┬─────────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────────┐
│                         TEMPO                                         │
│                   (Trace Backend Storage)                             │
│                                                                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                 │
│  │ Distributor │→ │  Ingester   │→ │  Compactor  │                 │
│  └─────────────┘  └─────────────┘  └─────────────┘                 │
│                                                                        │
└────────────────────────────┬─────────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────────┐
│                        GRAFANA                                        │
│                    (Visualization)                                    │
│                                                                        │
│  Query Tempo → Display Traces → Correlate with Logs                 │
│                                                                        │
└──────────────────────────────────────────────────────────────────────┘
```

### Logging Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                      YOUR APPLICATION CODE                            │
│                                                                        │
│  log.info("Order created: {}", orderId);                             │
│                                                                        │
└────────────────────────────┬─────────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────────┐
│                         SLF4J API                                     │
│                    (Logging Abstraction)                              │
│                                                                        │
└────────────────────────────┬─────────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────────┐
│                        LOGBACK                                        │
│                   (Logging Implementation)                            │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │                  MDC (Mapped Diagnostic Context)          │       │
│  │  Automatically populated by Micrometer:                   │       │
│  │  - traceId                                                 │       │
│  │  - spanId                                                  │       │
│  │  - service.name                                            │       │
│  └──────────────────────────────────────────────────────────┘       │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │                     Encoders                              │       │
│  │  - LogstashEncoder (JSON format)                          │       │
│  │  - Pattern encoder (human-readable)                       │       │
│  └──────────────────────────────────────────────────────────┘       │
│                                                                        │
└────────────┬──────────────────────┬────────────────────────────────────┘
             │                      │
             ▼                      ▼
┌─────────────────────┐  ┌──────────────────────┐
│  Console Appender   │  │   Loki Appender      │
│  (Development)      │  │   (Production)       │
└─────────────────────┘  └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │   Async Appender     │
                         │   (Non-blocking)     │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │        LOKI          │
                         │   (Log Storage)      │
                         └──────────────────────┘
```

---

## 4. Data Flow

### Trace Data Structure

```
Trace Object:
┌────────────────────────────────────────────────────────────┐
│ Trace ID: 550e8400-e29b-41d4-a716-446655440000             │
│ Start Time: 2026-01-07T11:25:53.299Z                       │
│ Duration: 250ms                                             │
│                                                              │
│ Spans: [                                                    │
│   ┌──────────────────────────────────────────────────┐    │
│   │ Span 1 (Root):                                    │    │
│   │   Span ID: 1234567890abcdef                       │    │
│   │   Parent ID: null                                 │    │
│   │   Name: graphql.createOrder                       │    │
│   │   Start: 2026-01-07T11:25:53.299Z                │    │
│   │   Duration: 250ms                                  │    │
│   │   Attributes: {                                    │    │
│   │     service.name: "graphql-service"               │    │
│   │     http.method: "POST"                           │    │
│   │     http.url: "/graphql"                          │    │
│   │   }                                                │    │
│   │   Events: []                                       │    │
│   └──────────────────────────────────────────────────┘    │
│                                                              │
│   ┌──────────────────────────────────────────────────┐    │
│   │ Span 2 (Child):                                   │    │
│   │   Span ID: abcdef1234567890                       │    │
│   │   Parent ID: 1234567890abcdef                     │    │
│   │   Name: order.create                              │    │
│   │   Start: 2026-01-07T11:25:53.310Z                │    │
│   │   Duration: 180ms                                  │    │
│   │   Attributes: {                                    │    │
│   │     service.name: "order-service"                 │    │
│   │     order.id: "abc-123"                           │    │
│   │     product.id: "laptop"                          │    │
│   │     order.quantity: "2"                           │    │
│   │   }                                                │    │
│   │   Events: [                                        │    │
│   │     { time: ..., name: "order.saved.to.database" }│    │
│   │   ]                                                │    │
│   └──────────────────────────────────────────────────┘    │
│                                                              │
│   ┌──────────────────────────────────────────────────┐    │
│   │ Span 3 (Grandchild):                              │    │
│   │   Span ID: fedcba0987654321                       │    │
│   │   Parent ID: abcdef1234567890                     │    │
│   │   Name: jdbc.insert                               │    │
│   │   Start: 2026-01-07T11:25:53.320Z                │    │
│   │   Duration: 30ms                                   │    │
│   │   Attributes: {                                    │    │
│   │     service.name: "order-service"                 │    │
│   │     db.system: "h2"                               │    │
│   │     db.statement: "INSERT INTO orders..."         │    │
│   │   }                                                │    │
│   └──────────────────────────────────────────────────┘    │
│                                                              │
│   ... more spans ...                                        │
│ ]                                                           │
└────────────────────────────────────────────────────────────┘
```

### Log Entry Structure

```json
{
  "@timestamp": "2026-01-07T11:25:53.320+06:00",
  "@version": "1",
  "level": "INFO",
  "logger": "com.example.tracing.order.OrderController",
  "message": "Received REST request to create order: ID=abc-123, Product=laptop",
  "service": "order-service",
  "host": "localhost",
  "thread": "http-nio-8081-exec-1",
  "traceId": "550e8400e29b41d4a716446655440000",
  "spanId": "abcdef1234567890"
}
```

### Correlation Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    Single Request Flow                       │
└─────────────────────────────────────────────────────────────┘

Trace ID: 550e8400-e29b-41d4-a716-446655440000

┌─────────────────┐
│ GraphQL Service │
│ Span: 1234...   │
│ Logs:           │
│  [11:25:53.299] │ traceId: 550e8400...
│  [11:25:53.300] │ traceId: 550e8400...
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Order Service  │
│ Span: abcdef... │
│ Logs:           │
│  [11:25:53.310] │ traceId: 550e8400...
│  [11:25:53.320] │ traceId: 550e8400...
│  [11:25:53.350] │ traceId: 550e8400...
└────────┬────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌─────────┐ ┌─────────┐
│Inventory│ │Notific. │
│Span:    │ │Span:    │
│fedcba...│ │567890...│
│Logs:    │ │Logs:    │
│[11:25:  │ │[11:25:  │ traceId: 550e8400...
│53.360]  │ │53.360]  │ traceId: 550e8400...
└─────────┘ └─────────┘

All logs and spans share the SAME Trace ID!
```

---

## 5. Deployment Architecture

### Development Environment

```
┌──────────────────────────────────────────────────────────────┐
│                      Developer Machine                        │
│                                                                │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              Application Services                       │ │
│  │  (Running via Maven spring-boot:run)                    │ │
│  │                                                          │ │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │ │
│  │  │ GraphQL  │ │  Order   │ │Inventory │ │Notificat.│ │ │
│  │  │  :8080   │ │  :8081   │ │  :8082   │ │  :8083   │ │ │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                                │
│  ┌────────────────────────────────────────────────────────┐ │
│  │         Infrastructure (Docker Compose)                 │ │
│  │                                                          │ │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │ │
│  │  │ RabbitMQ │ │  Tempo   │ │   Loki   │ │ Grafana  │ │ │
│  │  │  :5672   │ │  :4318   │ │  :3100   │ │  :3000   │ │ │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                                │
└──────────────────────────────────────────────────────────────┘
```

### Production Environment (Kubernetes)

```
┌──────────────────────────────────────────────────────────────────┐
│                      Kubernetes Cluster                           │
│                                                                    │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                   Application Namespace                     │ │
│  │                                                              │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐       │ │
│  │  │ GraphQL Svc │  │ Order Svc   │  │Inventory Svc│       │ │
│  │  │ Deployment  │  │ Deployment  │  │ Deployment  │       │ │
│  │  │ Replicas: 3 │  │ Replicas: 5 │  │ Replicas: 3 │       │ │
│  │  └─────────────┘  └─────────────┘  └─────────────┘       │ │
│  │                                                              │ │
│  │  Each pod exports traces to OpenTelemetry Collector        │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                    │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │               Observability Namespace                       │ │
│  │                                                              │ │
│  │  ┌─────────────────────────────────────────────────────┐  │ │
│  │  │        OpenTelemetry Collector                       │  │ │
│  │  │        (Receives traces from all pods)               │  │ │
│  │  │        Deployment: 2 replicas                        │  │ │
│  │  └──────────────────┬──────────────────────────────────┘  │ │
│  │                     │                                       │ │
│  │              ┌──────┴──────┐                               │ │
│  │              ▼             ▼                                │ │
│  │  ┌─────────────────┐  ┌─────────────────┐                │ │
│  │  │     Tempo       │  │      Loki       │                │ │
│  │  │  StatefulSet    │  │   StatefulSet   │                │ │
│  │  │  Replicas: 3    │  │   Replicas: 3   │                │ │
│  │  │  + S3 Storage   │  │   + S3 Storage  │                │ │
│  │  └─────────────────┘  └─────────────────┘                │ │
│  │                                                              │ │
│  │  ┌─────────────────────────────────────────────────────┐  │ │
│  │  │               Grafana                                │  │ │
│  │  │               Deployment: 2 replicas                 │  │ │
│  │  │               + LoadBalancer Service                 │  │ │
│  │  └─────────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                    │
└──────────────────────────────────────────────────────────────────┘
```

### Network Flow

```
Internet
   │
   ▼
┌─────────────┐
│   Ingress   │
│  Controller │
└──────┬──────┘
       │
       ├──────────────────┬──────────────────┬─────────────────┐
       │                  │                  │                 │
       ▼                  ▼                  ▼                 ▼
┌──────────┐      ┌──────────┐      ┌──────────┐     ┌──────────┐
│ GraphQL  │─────▶│  Order   │─────▶│ Inventory│     │Notification│
│ Service  │ HTTP │ Service  │ AMQP │ Service  │     │  Service   │
└──────────┘      └──────────┘      └──────────┘     └──────────┘
     │                  │                  │                 │
     │ OTLP             │ OTLP             │ OTLP            │ OTLP
     │                  │                  │                 │
     └──────────────────┴──────────────────┴─────────────────┘
                                │
                                ▼
                    ┌──────────────────────┐
                    │ OpenTelemetry        │
                    │ Collector            │
                    └──────────┬───────────┘
                               │
                      ┌────────┴────────┐
                      ▼                 ▼
                ┌──────────┐      ┌──────────┐
                │  Tempo   │      │   Loki   │
                └────┬─────┘      └────┬─────┘
                     │                 │
                     └────────┬────────┘
                              ▼
                        ┌──────────┐
                        │ Grafana  │
                        └──────────┘
```

---

## Summary

### Key Takeaways

1. **Trace Propagation**: Trace context flows through HTTP headers and AMQP message headers
2. **Vendor Neutrality**: Micrometer provides abstraction over OpenTelemetry
3. **Automatic Instrumentation**: Spring Boot auto-configures most tracing
4. **Log Correlation**: MDC automatically includes trace IDs in logs
5. **Unified Observability**: Grafana provides single interface for traces and logs

### Data Flow Summary

```
Application → Micrometer → OpenTelemetry → OTLP → Tempo → Grafana
Application → SLF4J → Logback → Loki → Grafana
```

---

*Last updated: January 7, 2026*
