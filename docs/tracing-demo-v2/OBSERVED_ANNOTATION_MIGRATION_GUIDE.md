# Migration Guide: From Manual Span Creation to @Observed Annotation

## Overview

This guide documents the migration from manual span creation (`@NewSpan` and `Observation.createNotStarted()`) to the **`@Observed`** annotation for Spring Boot 3.x/4.x applications.

## What Changed

### ✅ Completed Migrations

1. **order-service** - Migrated from `@NewSpan` to `@Observed`
2. **graphql-service** - Migrated from `@NewSpan` to `@Observed`
3. **inventory-service** - Migrated from `@NewSpan` to `@Observed`
4. **notification-service** - Migrated from `@NewSpan` to `@Observed`
5. **cqrs-service (Infrastructure)** - Migrated CommandBus, EventBus, QueryBus, OutboxService, OutboxPublisher

### 📝 Recommended for CQRS Handlers

The CQRS service handlers (command/event/query handlers) can also be migrated, but since they're already wrapped by the Bus classes (which now use `@Observed`), the migration is optional. The current `Observation.createNotStarted()` approach works fine.

## Why @Observed?

### Benefits

1. **Declarative** - Cleaner, annotation-based approach
2. **Less Boilerplate** - No need to inject `ObservationRegistry`
3. **Automatic Context Propagation** - Spring handles trace context automatically
4. **Better Integration** - Works seamlessly with Spring AOP
5. **Recommended by Spring** - Official Spring Boot 3.x+ approach

### Comparison

#### Old Approach (@NewSpan)

```java
@RestController
public class OrderController {
    
    @PostMapping("/orders")
    @NewSpan("order.process")
    public Order createOrder(@SpanTag("product.id") @RequestBody CreateOrderRequest request) {
        // Business logic
        return order;
    }
}
```

#### Old Approach (Observation API)

```java
@Component
public class CommandBus {
    private final ObservationRegistry observationRegistry;
    
    public <T extends Command, R> R dispatch(T command) {
        return Observation.createNotStarted("command.bus.dispatch", observationRegistry)
                .lowCardinalityKeyValue("command.type", commandName)
                .observe(() -> {
                    // Business logic
                    return handler.handle(command);
                });
    }
}
```

#### New Approach (@Observed)

```java
@RestController
public class OrderController {
    
    @PostMapping("/orders")
    @Observed(name = "order.process", contextualName = "order-process")
    public Order createOrder(@RequestBody CreateOrderRequest request) {
        // Business logic
        return order;
    }
}
```

```java
@Component
public class CommandBus {
    // No ObservationRegistry needed!
    
    @Observed(name = "command.bus.dispatch", contextualName = "command-bus-dispatch")
    public <T extends Command, R> R dispatch(T command) {
        // Business logic
        return handler.handle(command);
    }
}
```

## Migration Steps

### Step 1: Update Imports

**Remove:**
```java
import io.micrometer.tracing.annotation.NewSpan;
import io.micrometer.tracing.annotation.SpanTag;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
```

**Add:**
```java
import io.micrometer.observation.annotation.Observed;
```

### Step 2: Remove ObservationRegistry Dependency

**Before:**
```java
@Component
public class MyService {
    private final ObservationRegistry observationRegistry;
    
    public MyService(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }
}
```

**After:**
```java
@Component
public class MyService {
    // No ObservationRegistry needed!
    
    public MyService() {
    }
}
```

### Step 3: Replace @NewSpan with @Observed

**Before:**
```java
@NewSpan("inventory.update")
public void handleOrder(Order order) {
    // Business logic
}
```

**After:**
```java
@Observed(name = "inventory.update", contextualName = "inventory-update-order")
public void handleOrder(Order order) {
    // Business logic
}
```

### Step 4: Replace Observation.createNotStarted() with @Observed

**Before:**
```java
public <T> T execute(Command<T> command) {
    return Observation.createNotStarted("command.execute", observationRegistry)
            .lowCardinalityKeyValue("command.type", command.getType())
            .observe(() -> {
                // Business logic
                return result;
            });
}
```

**After:**
```java
@Observed(name = "command.execute", contextualName = "command-execute")
public <T> T execute(Command<T> command) {
    // Business logic
    return result;
}
```

## @Observed Annotation Parameters

### Required Parameters

- **`name`** - The metric/span name (e.g., "order.process")
- **`contextualName`** - Human-readable name for the span (e.g., "order-process")

### Optional Parameters

- **`lowCardinalityKeyValues`** - Array of key-value pairs for low cardinality tags
  ```java
  @Observed(
      name = "order.create",
      contextualName = "create-order",
      lowCardinalityKeyValues = {"service", "order-service"}
  )
  ```

## Complete Migration Examples

### Example 1: REST Controller

**Before:**
```java
@RestController
public class OrderController {
    private final OrderPublisher orderPublisher;
    
    @PostMapping("/orders")
    @NewSpan("order.process")
    public Order createOrder(@RequestBody CreateOrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        Order order = new Order(orderId, "CREATED", "Order for " + request.getProductId());
        orderPublisher.publishOrder(order);
        return order;
    }
}
```

**After:**
```java
@RestController
public class OrderController {
    private final OrderPublisher orderPublisher;
    
    @PostMapping("/orders")
    @Observed(name = "order.process", contextualName = "order-process")
    public Order createOrder(@RequestBody CreateOrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        Order order = new Order(orderId, "CREATED", "Order for " + request.getProductId());
        orderPublisher.publishOrder(order);
        return order;
    }
}
```

### Example 2: Service Method

**Before:**
```java
@Service
public class OrderPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final Tracer tracer;
    
    public OrderPublisher(RabbitTemplate rabbitTemplate, Tracer tracer) {
        this.rabbitTemplate = rabbitTemplate;
        this.tracer = tracer;
    }
    
    public void publishOrder(Order order) {
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY, order);
    }
}
```

**After:**
```java
@Service
public class OrderPublisher {
    private final RabbitTemplate rabbitTemplate;
    
    public OrderPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }
    
    @Observed(name = "order.publish", contextualName = "order-publish-rabbitmq")
    public void publishOrder(Order order) {
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY, order);
    }
}
```

### Example 3: RabbitMQ Listener

**Before:**
```java
@Service
public class OrderListener {
    
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "orders.queue"),
            exchange = @Exchange(name = "orders.exchange", type = ExchangeTypes.TOPIC),
            key = "orders.created"
    ))
    @NewSpan("inventory.update")
    public void handleOrder(Order order) throws InterruptedException {
        log.info("Received order from RabbitMQ: {}", order.getOrderId());
        Thread.sleep(100);
        log.info("Inventory updated for order: {}", order.getOrderId());
    }
}
```

**After:**
```java
@Service
public class OrderListener {
    
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "orders.queue"),
            exchange = @Exchange(name = "orders.exchange", type = ExchangeTypes.TOPIC),
            key = "orders.created"
    ))
    @Observed(name = "inventory.update", contextualName = "inventory-update-order")
    public void handleOrder(Order order) throws InterruptedException {
        log.info("Received order from RabbitMQ: {}", order.getOrderId());
        Thread.sleep(100);
        log.info("Inventory updated for order: {}", order.getOrderId());
    }
}
```

### Example 4: GraphQL Mutation

**Before:**
```java
@Controller
public class OrderController {
    private final OrderClient orderClient;
    
    @MutationMapping
    @NewSpan("graphql.createOrder")
    public Order createOrder(@SpanTag("product.id") @Argument String productId, 
                             @SpanTag("order.quantity") @Argument int quantity) {
        return orderClient.createOrder(productId, quantity);
    }
}
```

**After:**
```java
@Controller
public class OrderController {
    private final OrderClient orderClient;
    
    @MutationMapping
    @Observed(name = "graphql.createOrder", contextualName = "graphql-create-order")
    public Order createOrder(@Argument String productId, 
                             @Argument int quantity) {
        return orderClient.createOrder(productId, quantity);
    }
}
```

### Example 5: Command Bus (Complex)

**Before:**
```java
@Component
public class CommandBus {
    private final Map<Class<? extends Command>, CommandHandler<?, ?>> handlers = new ConcurrentHashMap<>();
    private final ObservationRegistry observationRegistry;
    private final MeterRegistry meterRegistry;
    
    public CommandBus(ObservationRegistry observationRegistry, MeterRegistry meterRegistry) {
        this.observationRegistry = observationRegistry;
        this.meterRegistry = meterRegistry;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Command, R> R dispatch(T command) {
        String commandName = command.getClass().getSimpleName();
        
        return Observation.createNotStarted("command.bus.dispatch", observationRegistry)
                .lowCardinalityKeyValue("command.type", commandName)
                .observe(() -> {
                    CommandHandler<T, R> handler = (CommandHandler<T, R>) handlers.get(command.getClass());
                    return handler.handle(command);
                });
    }
}
```

**After:**
```java
@Component
public class CommandBus {
    private final Map<Class<? extends Command>, CommandHandler<?, ?>> handlers = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;
    
    public CommandBus(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    @SuppressWarnings("unchecked")
    @Observed(name = "command.bus.dispatch", contextualName = "command-bus-dispatch")
    public <T extends Command, R> R dispatch(T command) {
        CommandHandler<T, R> handler = (CommandHandler<T, R>) handlers.get(command.getClass());
        return handler.handle(command);
    }
}
```

## Configuration Requirements

### Enable @Observed Support

Make sure you have the following in your `application.yml`:

```yaml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0
  observations:
    annotations:
      enabled: true  # Enable @Observed annotation support
```

### Required Dependencies

```gradle
dependencies {
    // Spring Boot Actuator (includes Micrometer)
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    
    // Micrometer Tracing Bridge for OpenTelemetry
    implementation 'io.micrometer:micrometer-tracing-bridge-otel'
    
    // OpenTelemetry Exporter
    implementation 'io.opentelemetry:opentelemetry-exporter-otlp'
    
    // AOP for @Observed (usually included with Spring Boot)
    implementation 'org.springframework.boot:spring-boot-starter-aop'
}
```

## Trace Hierarchy

The `@Observed` annotation automatically creates proper parent-child span relationships:

```
HTTP Request (auto-instrumented)
└─ api.create.product (@Observed on controller)
   └─ command.bus.dispatch (@Observed on CommandBus)
      └─ command.handler.create.product (@Observed on handler)
         ├─ Database Save (auto-instrumented)
         └─ outbox.store (@Observed on OutboxService)
            └─ [async] outbox.poll (@Observed on OutboxPublisher)
               └─ outbox.publish (@Observed on publishEvent)
                  └─ event.bus.publish (@Observed on EventBus)
                     └─ event.handler.execute (@Observed on dispatchToHandler)
```

## Testing

After migration, verify that:

1. **Traces are still created** - Check Grafana/Tempo
2. **Span names are correct** - Should match the `name` parameter
3. **Parent-child relationships are preserved** - Check trace visualization
4. **Metrics still work** - Check Prometheus
5. **Logs are correlated** - Check Loki for trace IDs

### Test Commands

```bash
# Start services
./gradlew :order-service:bootRun
./gradlew :graphql-service:bootRun
./gradlew :inventory-service:bootRun
./gradlew :notification-service:bootRun
./gradlew :cqrs-service:bootRun

# Create an order
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{"productId": "laptop-123", "quantity": 2}'

# Check traces in Grafana
open http://localhost:3000
# Navigate to Explore → Tempo → Search for service.name="order-service"
```

## Troubleshooting

### Issue: Spans not being created

**Solution:** Ensure `management.observations.annotations.enabled=true` in `application.yml`

### Issue: No parent-child relationship

**Solution:** Make sure Spring AOP is enabled and the method is being called through a Spring proxy (not `this.method()`)

### Issue: @Observed not working on private methods

**Solution:** `@Observed` only works on public methods due to Spring AOP proxy limitations

### Issue: Duplicate spans

**Solution:** Remove both `@Observed` and manual `Observation.createNotStarted()` - use only one approach

## Best Practices

1. **Use descriptive names** - `name = "order.process"` is better than `name = "process"`
2. **Use contextual names** - `contextualName = "order-process"` helps in trace visualization
3. **Don't over-instrument** - Only add `@Observed` to key business methods
4. **Keep span names consistent** - Use dot notation: `service.operation`
5. **Avoid high-cardinality tags** - Don't use user IDs or timestamps in span names

## Summary

The migration to `@Observed` provides:
- ✅ Cleaner code
- ✅ Less boilerplate
- ✅ Better Spring integration
- ✅ Same observability features
- ✅ Easier maintenance

All services in the tracing-demo-v2 project now use the modern `@Observed` annotation approach for creating spans and traces.

## Files Modified

### order-service
- `OrderController.java` - Added `@Observed` to `createOrder()`
- `OrderPublisher.java` - Added `@Observed` to `publishOrder()`

### graphql-service
- `OrderController.java` - Replaced `@NewSpan` with `@Observed`
- `OrderClient.java` - Added `@Observed` to `createOrder()`

### inventory-service
- `OrderListener.java` - Replaced `@NewSpan` with `@Observed`

### notification-service
- `NotificationListener.java` - Replaced `@NewSpan` with `@Observed`

### cqrs-service (Infrastructure)
- `ProductController.java` - Replaced `Observation.createNotStarted()` with `@Observed`
- `CommandBus.java` - Replaced `Observation.createNotStarted()` with `@Observed`
- `EventBus.java` - Replaced `Observation.createNotStarted()` with `@Observed`
- `QueryBus.java` - Replaced `Observation.createNotStarted()` with `@Observed`
- `OutboxService.java` - Replaced `Observation.createNotStarted()` with `@Observed`
- `OutboxPublisher.java` - Replaced `Observation.createNotStarted()` with `@Observed`

### cqrs-service (Handlers) - Optional
The command/event/query handlers still use `Observation.createNotStarted()` but can be migrated following the same pattern. Since they're already wrapped by the Bus classes (which now use `@Observed`), the migration is optional.

## References

- [Spring Boot Observability Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.observability)
- [Micrometer Observation API](https://micrometer.io/docs/observation)
- [OpenTelemetry Java](https://opentelemetry.io/docs/instrumentation/java/)
