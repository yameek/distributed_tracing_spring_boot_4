# CQRS Service - Architecture Deep Dive

## Table of Contents
1. [Overview](#overview)
2. [CQRS Pattern](#cqrs-pattern)
3. [Command Bus Architecture](#command-bus-architecture)
4. [Event Bus Architecture](#event-bus-architecture)
5. [Outbox Pattern](#outbox-pattern)
6. [Tracing Strategy](#tracing-strategy)
7. [Metrics Strategy](#metrics-strategy)
8. [Error Handling](#error-handling)
9. [Transaction Management](#transaction-management)
10. [Scalability Considerations](#scalability-considerations)

## Overview

This service implements a production-ready CQRS architecture with:
- **Separation of reads and writes** for scalability
- **Event-driven architecture** for loose coupling
- **Transactional outbox** for reliable event delivery
- **Comprehensive observability** at every layer

## CQRS Pattern

### What is CQRS?

CQRS (Command Query Responsibility Segregation) separates read and write operations:

```
┌─────────────────────────────────────────────────────────┐
│                    Client Request                        │
└────────────────┬────────────────────────────────────────┘
                 │
        ┌────────┴────────┐
        │                 │
        ▼                 ▼
   ┌─────────┐      ┌──────────┐
   │ Command │      │  Query   │
   │  (Write)│      │  (Read)  │
   └────┬────┘      └────┬─────┘
        │                │
        ▼                ▼
   ┌─────────┐      ┌──────────┐
   │ Write   │      │  Read    │
   │ Model   │      │  Model   │
   └────┬────┘      └──────────┘
        │
        ▼
   ┌─────────┐
   │ Events  │
   └─────────┘
```

### Benefits

1. **Scalability**: Read and write sides scale independently
2. **Optimization**: Each side optimized for its use case
3. **Flexibility**: Different data models for reads and writes
4. **Clarity**: Clear separation of concerns

### Implementation in This Service

**Commands (Write Side)**:
- `CreateProductCommand` - Creates new product
- `UpdateProductPriceCommand` - Updates price
- `UpdateStockCommand` - Updates stock

**Queries (Read Side)**:
- `GetProductByIdQuery` - Retrieves single product
- `GetAllProductsQuery` - Retrieves all products

**Domain Events**:
- `ProductCreatedEvent` - Product was created
- `ProductPriceUpdatedEvent` - Price was updated
- `ProductStockUpdatedEvent` - Stock was updated

## Command Bus Architecture

### Design

```java
public interface Command {
    String getCommandId();
}

public interface CommandHandler<T extends Command, R> {
    R handle(T command);
    Class<T> getCommandType();
}

@Component
public class CommandBus {
    private Map<Class<? extends Command>, CommandHandler<?, ?>> handlers;
    
    public <T extends Command, R> R dispatch(T command) {
        // Find handler
        // Create trace span
        // Execute with metrics
        // Handle errors
    }
}
```

### Key Features

1. **Single Handler per Command**: Ensures clear ownership
2. **Type Safety**: Compile-time verification of handler registration
3. **Automatic Tracing**: Every command creates a trace span
4. **Metrics Collection**: Success/failure rates and timing
5. **Error Handling**: Consistent error handling across all commands

### Tracing Implementation

```java
return Observation.createNotStarted("command.bus.dispatch", observationRegistry)
    .lowCardinalityKeyValue("command.type", commandName)
    .lowCardinalityKeyValue("command.id", commandId)
    .observe(() -> {
        // Execute command
    });
```

**Span Attributes**:
- `command.type`: Name of the command (e.g., "CreateProductCommand")
- `command.id`: Unique identifier for this command instance
- `handler.name`: Name of the handler that processed it

### Metrics

- `command.bus.success` (Counter): Successful executions
- `command.bus.failure` (Counter): Failed executions
- `command.bus.execution.time` (Timer): Execution duration

### Registration

Handlers are registered at startup:

```java
@Bean
public CommandLineRunner registerCommandHandlers(
        CommandBus commandBus,
        CreateProductCommandHandler createProductHandler) {
    return args -> {
        commandBus.registerHandler(createProductHandler);
    };
}
```

## Event Bus Architecture

### Design

```java
public interface DomainEvent {
    String getEventId();
    Instant getOccurredAt();
    String getAggregateId();
}

public interface EventHandler<T extends DomainEvent> {
    void handle(T event);
    Class<T> getEventType();
}

@Component
public class EventBus {
    private Map<Class<? extends DomainEvent>, List<EventHandler<?>>> handlers;
    
    public <T extends DomainEvent> void publish(T event) {
        // Find all handlers
        // Create trace span
        // Execute each handler in isolation
        // Collect metrics
    }
}
```

### Key Features

1. **Multiple Handlers per Event**: Supports pub/sub pattern
2. **Handler Isolation**: One handler failure doesn't affect others
3. **Automatic Tracing**: Parent span for publish, child spans for each handler
4. **Metrics per Handler**: Track individual handler performance
5. **Async Processing**: Events published asynchronously via outbox

### Tracing Implementation

```java
// Parent span for event publication
Observation.createNotStarted("event.bus.publish", observationRegistry)
    .lowCardinalityKeyValue("event.type", eventType)
    .lowCardinalityKeyValue("event.id", eventId)
    .observe(() -> {
        for (EventHandler<?> handler : handlers) {
            // Child span for each handler
            Observation.createNotStarted("event.handler.execute", observationRegistry)
                .lowCardinalityKeyValue("handler.name", handlerName)
                .observe(() -> handler.handle(event));
        }
    });
```

**Span Hierarchy**:
```
event.bus.publish
├─ event.handler.execute (Handler 1)
├─ event.handler.execute (Handler 2)
└─ event.handler.execute (Handler 3)
```

### Metrics

- `event.bus.published` (Counter): Events published
- `event.bus.handled` (Counter): Events successfully handled
- `event.bus.handling.failure` (Counter): Handler failures
- `event.bus.handling.time` (Timer): Handler execution time

## Outbox Pattern

### Why Outbox Pattern?

**Problem**: How to reliably publish events when updating the database?

**Traditional Approach (Broken)**:
```java
// ❌ NOT RELIABLE
@Transactional
public void createProduct(CreateProductCommand cmd) {
    Product product = new Product(...);
    productRepository.save(product);  // DB transaction
    
    // What if this fails? Event lost!
    // What if DB transaction fails? Event already sent!
    eventBus.publish(new ProductCreatedEvent(...));
}
```

**Outbox Pattern (Reliable)**:
```java
// ✅ RELIABLE
@Transactional
public void createProduct(CreateProductCommand cmd) {
    Product product = new Product(...);
    productRepository.save(product);
    
    // Store event in outbox table (SAME TRANSACTION)
    outboxService.storeEvent(new ProductCreatedEvent(...));
    
    // Transaction commits: both product and event saved atomically
}

// Separate process polls outbox and publishes events
@Scheduled(fixedDelay = 5000)
public void publishPendingEvents() {
    List<OutboxEvent> pending = outboxRepository.findPending();
    for (OutboxEvent event : pending) {
        rabbitTemplate.send(event.getPayload());
        outboxService.markAsPublished(event.getId());
    }
}
```

### Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  Command Handler                         │
│  (Executes in single transaction)                       │
│                                                          │
│  1. Save domain entity to database                      │
│  2. Store event in outbox table                         │
│                                                          │
│  ← Transaction commits (atomic) →                       │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│              Outbox Publisher (Async)                    │
│  (Runs on scheduled interval)                           │
│                                                          │
│  1. Poll outbox table for PENDING events                │
│  2. Publish to RabbitMQ                                 │
│  3. Update status to PUBLISHED                          │
│  4. Retry on failure                                    │
└─────────────────────────────────────────────────────────┘
```

### Outbox Event Lifecycle

```
PENDING → PROCESSING → PUBLISHED
   ↓
FAILED (retry up to 3 times)
```

### Database Schema

```sql
CREATE TABLE outbox_events (
    id VARCHAR(36) PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(36) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    retry_count INTEGER DEFAULT 0,
    error_message TEXT,
    version BIGINT
);

-- Optimize polling query
CREATE INDEX idx_outbox_status_created 
ON outbox_events(status, created_at);

-- Optimize aggregate queries
CREATE INDEX idx_outbox_aggregate 
ON outbox_events(aggregate_id);
```

### Polling Strategy

**Pending Events** (every 5 seconds):
```java
@Scheduled(fixedDelay = 5000, initialDelay = 10000)
@Transactional
public void publishPendingEvents() {
    List<OutboxEvent> pending = repository.findPendingEventsForProcessing();
    for (OutboxEvent event : pending) {
        publishEvent(event);
    }
}
```

**Failed Events** (every 60 seconds):
```java
@Scheduled(fixedDelay = 60000, initialDelay = 30000)
@Transactional
public void retryFailedEvents() {
    Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
    List<OutboxEvent> failed = repository.findFailedEventsForRetry(3, cutoff);
    for (OutboxEvent event : failed) {
        publishEvent(event);
    }
}
```

### Concurrency Control

**Pessimistic Locking**:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT e FROM OutboxEvent e WHERE e.status = 'PENDING'")
List<OutboxEvent> findPendingEventsForProcessing();
```

Prevents multiple instances from processing the same event.

### Tracing

```
command.handler.create.product
└─ outbox.store
   └─ [async] outbox.poll
       └─ outbox.publish
           └─ event.bus.publish
               └─ event.handler.execute
```

### Guarantees

1. **Atomicity**: Domain changes and event storage are atomic
2. **At-least-once delivery**: Events may be delivered multiple times (idempotent handlers required)
3. **Ordering**: Events for same aggregate are processed in order
4. **Reliability**: Automatic retry on failure

## Tracing Strategy

### Trace Propagation

```
HTTP Request (trace-id: abc123)
  └─ API Controller (span-id: 001)
      └─ Command Bus (span-id: 002)
          └─ Command Handler (span-id: 003)
              ├─ Database Save (span-id: 004)
              └─ Outbox Store (span-id: 005)
                  └─ [async] Outbox Publish (span-id: 006)
                      └─ Event Bus (span-id: 007)
                          └─ Event Handler (span-id: 008)
```

**All spans share the same trace-id: abc123**

### Span Attributes

**Low Cardinality** (indexed, filterable):
- `service.name`: "cqrs-service"
- `command.type`: "CreateProductCommand"
- `event.type`: "ProductCreatedEvent"
- `handler.name`: "CreateProductCommandHandler"

**High Cardinality** (not indexed):
- `command.id`: Unique per command
- `event.id`: Unique per event
- `aggregate.id`: Product ID

### Implementation

```java
@Component
public class CommandBus {
    private final ObservationRegistry observationRegistry;
    
    public <T extends Command, R> R dispatch(T command) {
        return Observation.createNotStarted("command.bus.dispatch", observationRegistry)
            .lowCardinalityKeyValue("command.type", command.getClass().getSimpleName())
            .lowCardinalityKeyValue("command.id", command.getCommandId())
            .observe(() -> {
                // Execute command
            });
    }
}
```

### Trace Sampling

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% sampling (development)
                        # Use 0.1 (10%) in production
```

## Metrics Strategy

### Metric Types

1. **Counters**: Monotonically increasing values
   - `command.bus.success`
   - `products.created`
   - `events.published`

2. **Timers**: Duration measurements with percentiles
   - `command.bus.execution.time`
   - `event.bus.handling.time`

3. **Gauges**: Current value at a point in time
   - `outbox.pending.count` (could be added)

### Metric Naming Convention

```
<component>.<operation>.<metric_type>

Examples:
- command.bus.success (counter)
- event.bus.handling.time (timer)
- products.created (counter)
- alerts.low.stock (counter)
```

### Implementation

```java
@Component
public class CommandBus {
    private final MeterRegistry meterRegistry;
    private final Counter successCounter;
    private final Timer executionTimer;
    
    public CommandBus(MeterRegistry meterRegistry) {
        this.successCounter = Counter.builder("command.bus.success")
            .description("Number of successful commands")
            .register(meterRegistry);
            
        this.executionTimer = Timer.builder("command.bus.execution.time")
            .description("Command execution time")
            .register(meterRegistry);
    }
    
    public <T extends Command, R> R dispatch(T command) {
        return executionTimer.record(() -> {
            R result = handler.handle(command);
            successCounter.increment();
            return result;
        });
    }
}
```

### Prometheus Queries

```promql
# Success rate
rate(command_bus_success_total[5m])

# Error rate
rate(command_bus_failure_total[5m])

# 95th percentile latency
histogram_quantile(0.95, rate(command_bus_execution_time_bucket[5m]))

# Events published per second
rate(outbox_events_published_total[5m])
```

## Error Handling

### Layers of Error Handling

1. **Controller Layer**: HTTP error responses
2. **Bus Layer**: Wraps exceptions with context
3. **Handler Layer**: Domain-specific exceptions
4. **Infrastructure Layer**: Retry and recovery

### Command Error Handling

```java
@Component
public class CommandBus {
    public <T extends Command, R> R dispatch(T command) {
        try {
            return handler.handle(command);
        } catch (Exception e) {
            failureCounter.increment();
            log.error("Command failed: {}", command.getClass().getSimpleName(), e);
            throw new CommandExecutionException("Failed to execute command", e);
        }
    }
}
```

### Event Error Handling

```java
@Component
public class EventBus {
    private void dispatchToHandler(EventHandler<T> handler, T event) {
        try {
            handler.handle(event);
            handledCounter.increment();
        } catch (Exception e) {
            failureCounter.increment();
            log.error("Event handler failed: {}", handler.getHandlerName(), e);
            // Continue to next handler (isolation)
        }
    }
}
```

### Outbox Error Handling

```java
@Component
public class OutboxPublisher {
    private void publishEvent(OutboxEvent event) {
        try {
            rabbitTemplate.send(event.getPayload());
            outboxService.markAsPublished(event.getId());
        } catch (Exception e) {
            String errorMsg = "Failed to publish: " + e.getMessage();
            outboxService.markAsFailed(event.getId(), errorMsg);
            // Will be retried later
        }
    }
}
```

### HTTP Error Responses

```java
@RestController
public class ProductController {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Error processing request", e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(e.getMessage()));
    }
}
```

## Transaction Management

### Command Handler Transactions

```java
@Component
public class CreateProductCommandHandler {
    @Override
    @Transactional  // Single transaction
    public String handle(CreateProductCommand command) {
        // 1. Save product
        Product product = productRepository.save(new Product(...));
        
        // 2. Store event in outbox (SAME TRANSACTION)
        outboxService.storeEvent(new ProductCreatedEvent(...));
        
        // Transaction commits: both operations succeed or both fail
        return product.getId();
    }
}
```

### Outbox Publisher Transactions

```java
@Component
public class OutboxPublisher {
    @Scheduled(fixedDelay = 5000)
    @Transactional  // Separate transaction per event
    public void publishPendingEvents() {
        List<OutboxEvent> pending = repository.findPending();
        for (OutboxEvent event : pending) {
            publishEvent(event);  // Each event in its own transaction
        }
    }
}
```

### Transaction Boundaries

```
┌─────────────────────────────────────────┐
│  Command Handler Transaction            │
│  ┌─────────────────────────────────┐   │
│  │ 1. Save Product                 │   │
│  │ 2. Store Event in Outbox        │   │
│  └─────────────────────────────────┘   │
│  COMMIT (atomic)                        │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  Outbox Publisher Transaction           │
│  ┌─────────────────────────────────┐   │
│  │ 1. Read Event from Outbox       │   │
│  │ 2. Publish to RabbitMQ          │   │
│  │ 3. Update Status                │   │
│  └─────────────────────────────────┘   │
│  COMMIT                                 │
└─────────────────────────────────────────┘
```

## Scalability Considerations

### Horizontal Scaling

**Command Side**:
- Multiple instances can handle commands concurrently
- Database handles concurrency with optimistic locking
- Each instance processes its own requests

**Query Side**:
- Read replicas can be added
- Caching layer can be introduced
- No write contention

**Outbox Publisher**:
- Multiple instances can run concurrently
- Pessimistic locking prevents duplicate processing
- Each instance polls independently

### Vertical Scaling

**Database**:
- Indexes on outbox table for fast polling
- Connection pooling for efficiency
- Partitioning for large event volumes

**Message Broker**:
- RabbitMQ clustering for high availability
- Multiple queues for parallel processing
- Persistent messages for reliability

### Performance Optimizations

1. **Batch Processing**: Poll multiple events at once
2. **Async Publishing**: Don't block command handlers
3. **Caching**: Cache frequently accessed data
4. **Indexing**: Optimize database queries
5. **Connection Pooling**: Reuse database connections

### Monitoring

Key metrics to monitor:
- Command execution time (p50, p95, p99)
- Event publishing lag (time in outbox)
- Outbox table size (pending events)
- RabbitMQ queue depth
- Database connection pool usage

## Summary

This architecture provides:
- ✅ **Reliability**: Transactional outbox ensures no event loss
- ✅ **Scalability**: Read and write sides scale independently
- ✅ **Observability**: Complete tracing, metrics, and logging
- ✅ **Maintainability**: Clear separation of concerns
- ✅ **Extensibility**: Easy to add new commands, events, queries
- ✅ **Performance**: Optimized for high throughput
- ✅ **Resilience**: Automatic retry and error handling

The combination of CQRS, event sourcing, outbox pattern, and comprehensive observability creates a production-ready, enterprise-grade service architecture.
