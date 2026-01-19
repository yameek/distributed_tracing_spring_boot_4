# @Observed Annotation Quick Reference

## Basic Usage

```java
import io.micrometer.observation.annotation.Observed;

@Service
public class MyService {
    
    @Observed(name = "my.operation", contextualName = "my-operation")
    public String doSomething() {
        // Your business logic
        return "result";
    }
}
```

## Common Patterns

### REST Controller
```java
@RestController
public class OrderController {
    
    @PostMapping("/orders")
    @Observed(name = "order.create", contextualName = "create-order")
    public Order createOrder(@RequestBody OrderRequest request) {
        return orderService.create(request);
    }
}
```

### Service Layer
```java
@Service
public class OrderService {
    
    @Observed(name = "order.process", contextualName = "process-order")
    public Order create(OrderRequest request) {
        // Business logic
        return order;
    }
}
```

### RabbitMQ Listener
```java
@Service
public class OrderListener {
    
    @RabbitListener(queues = "orders.queue")
    @Observed(name = "order.consume", contextualName = "consume-order-message")
    public void handleOrder(Order order) {
        // Process message
    }
}
```

### GraphQL Mutation
```java
@Controller
public class OrderController {
    
    @MutationMapping
    @Observed(name = "graphql.createOrder", contextualName = "graphql-create-order")
    public Order createOrder(@Argument String productId, @Argument int quantity) {
        return orderClient.createOrder(productId, quantity);
    }
}
```

### Async Method
```java
@Service
public class NotificationService {
    
    @Async
    @Observed(name = "notification.send", contextualName = "send-notification")
    public CompletableFuture<Void> sendEmail(String recipient, String message) {
        // Send email
        return CompletableFuture.completedFuture(null);
    }
}
```

## Parameters

| Parameter | Required | Description | Example |
|-----------|----------|-------------|---------|
| `name` | Yes | Metric/span name | `"order.create"` |
| `contextualName` | Yes | Human-readable name | `"create-order"` |
| `lowCardinalityKeyValues` | No | Static tags | `{"service", "order-service"}` |

## Configuration

### application.yml
```yaml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0
  observations:
    annotations:
      enabled: true  # Required for @Observed
```

### build.gradle
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-aop'
    implementation 'io.micrometer:micrometer-tracing-bridge-otel'
    implementation 'io.opentelemetry:opentelemetry-exporter-otlp'
}
```

## Migration Cheat Sheet

### From @NewSpan
```java
// Before
@NewSpan("order.process")
public Order createOrder(@SpanTag("product.id") String productId) {
    return order;
}

// After
@Observed(name = "order.process", contextualName = "order-process")
public Order createOrder(String productId) {
    return order;
}
```

### From Observation.createNotStarted()
```java
// Before
public Order createOrder(OrderRequest request) {
    return Observation.createNotStarted("order.create", observationRegistry)
            .observe(() -> {
                // Business logic
                return order;
            });
}

// After
@Observed(name = "order.create", contextualName = "create-order")
public Order createOrder(OrderRequest request) {
    // Business logic
    return order;
}
```

## Naming Conventions

### Span Names (name parameter)
- Use dot notation: `service.operation`
- Lowercase
- Examples:
  - `order.create`
  - `inventory.update`
  - `notification.send`
  - `command.bus.dispatch`
  - `event.handler.execute`

### Contextual Names (contextualName parameter)
- Use kebab-case: `service-operation`
- Descriptive
- Examples:
  - `create-order`
  - `update-inventory`
  - `send-notification`
  - `command-bus-dispatch`
  - `event-handler-execute`

## Important Notes

1. ✅ **Works on public methods only** (Spring AOP limitation)
2. ✅ **Requires Spring AOP** (included in spring-boot-starter-aop)
3. ✅ **Auto-propagates trace context** (no manual context management)
4. ✅ **Works with async methods** (use with @Async)
5. ❌ **Don't use on private methods** (won't work due to proxying)
6. ❌ **Don't use on final methods** (can't be proxied)
7. ❌ **Don't call from same class** (use `this.method()` bypasses proxy)

## Troubleshooting

### Spans not created?
- Check: `management.observations.annotations.enabled=true`
- Check: Method is public
- Check: Method called through Spring proxy (not `this.method()`)

### No parent-child relationship?
- Check: Spring AOP is enabled
- Check: Both methods are @Observed
- Check: Calling through Spring beans (not direct instantiation)

### Duplicate spans?
- Remove manual `Observation.createNotStarted()` calls
- Use only `@Observed` annotation

## Complete Example

```java
package com.example.order;

import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    
    public OrderService(OrderRepository orderRepository, 
                       NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
    }
    
    @Transactional
    @Observed(name = "order.create", contextualName = "create-order")
    public Order createOrder(OrderRequest request) {
        // Validate
        validateRequest(request);
        
        // Create order
        Order order = new Order();
        order.setProductId(request.getProductId());
        order.setQuantity(request.getQuantity());
        order.setStatus("CREATED");
        
        // Save to database (auto-instrumented)
        order = orderRepository.save(order);
        
        // Send notification (creates child span)
        notificationService.sendOrderConfirmation(order);
        
        return order;
    }
    
    @Observed(name = "order.validate", contextualName = "validate-order")
    private void validateRequest(OrderRequest request) {
        if (request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }
}

@Service
class NotificationService {
    
    @Async
    @Observed(name = "notification.send", contextualName = "send-order-confirmation")
    public CompletableFuture<Void> sendOrderConfirmation(Order order) {
        // Send email/SMS
        log.info("Sending confirmation for order: {}", order.getId());
        return CompletableFuture.completedFuture(null);
    }
}
```

## Trace Visualization

The above example creates this trace hierarchy:

```
order.create
├─ Database INSERT (auto-instrumented)
└─ notification.send (async, new trace)
```

## Resources

- [Full Migration Guide](./OBSERVED_ANNOTATION_MIGRATION_GUIDE.md)
- [Spring Boot Observability Docs](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.observability)
- [Micrometer Observation](https://micrometer.io/docs/observation)
