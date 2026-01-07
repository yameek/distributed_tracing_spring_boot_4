# CRITICAL: Trace IDs Not Appearing in Logs

## Current Status ❌

Distributed tracing is **NOT working correctly**. Logs show:
```json
{
  "traceId": null,    // Should be: "3c5e0f8a2b1d4e6a9c7f3e5b8d1a4c6e"
  "spanId": null,     // Should be: "9c7f3e5b8d1a4c6e" 
  "message": "..."
}
```

**This defeats the entire purpose of having tracing!**

## Root Cause

Spring Boot 4.0.1 with Java 25 has **different auto-configuration** than previous versions. The automatic MDC propagation that worked in Spring Boot 3.x **does not work the same way**.

## What's Missing

The trace context is being created and sent to Tempo, BUT:
1. ❌ Trace IDs are NOT being put into SLF4J MDC
2. ❌ Cannot correlate logs across services
3. ❌ Cannot query Loki by traceId  
4. ❌ Distributed tracing is useless without correlated logs

## Two Solutions

### Solution 1: Use Spring Boot 3.4.x (RECOMMENDED) ✅

**Downgrade to Spring Boot 3.4.1** which has mature, stable tracing:

```xml
<properties>
    <spring-boot.version>3.4.1</spring-boot.version>
    <java.version>21</java.version>  <!-- or 17 -->
</properties>
```

**Pros**:
- Auto-configuration works out of the box
- MDC propagation is automatic
- Well-documented and battle-tested
- Many examples online

**Cons**:
- Can't use Java 25 features (but you're not using them anyway)
- "Downgrade" feels wrong psychologically

### Solution 2: Manual MDC Integration (COMPLEX) ⚠️

Stay on Spring Boot 4.0.1 but manually integrate MDC:

**Step 1**: Create `TracingMdcConfiguration.java` in each service:

```java
package com.example.tracing.order;

import io.micrometer.tracing.Tracer;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class TracingMdcConfiguration implements WebMvcConfigurer {
    
    private final Tracer tracer;
    
    public TracingMdcConfiguration(Tracer tracer) {
        this.tracer = tracer;
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if (tracer != null && tracer.currentSpan() != null) {
                    var context = tracer.currentSpan().context();
                    MDC.put("traceId", context.traceId());
                    MDC.put("spanId", context.spanId());
                }
                return true;
            }
            
            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
                MDC.remove("traceId");
                MDC.remove("spanId");
            }
        });
    }
}
```

**Step 2**: Create `RabbitMqTracingConfiguration.java`:

```java
package com.example.tracing.order;

import io.micrometer.tracing.Tracer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;

@Aspect
@Configuration
public class RabbitMqTracingConfiguration {
    
    private final Tracer tracer;
    
    public RabbitMqTracingConfiguration(Tracer tracer) {
        this.tracer = tracer;
    }
    
    @Around("@annotation(org.springframework.amqp.rabbit.annotation.RabbitListener)")
    public Object aroundRabbitListener(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            if (tracer != null && tracer.currentSpan() != null) {
                var context = tracer.currentSpan().context();
                MDC.put("traceId", context.traceId());
                MDC.put("spanId", context.spanId());
            }
            return joinPoint.proceed();
        } finally {
            MDC.remove("traceId");
            MDC.remove("spanId");
        }
    }
}
```

**Step 3**: Add AspectJ dependency to all POMs:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
    <version>${spring-boot.version}</version>
</dependency>
```

**Pros**:
- Stays on latest Spring Boot
- Can use Java 25

**Cons**:
- Complex - need to maintain custom code
- Easy to miss edge cases
- Need to update for each new async/reactive pattern
- More prone to bugs

## My Recommendation 🎯

**Use Spring Boot 3.4.1 with Java 21.**

Here's why:
1. Tracing is a **solved problem** in Spring Boot 3.4.x
2. You're not using any Java 25-specific features
3. Production stability > bleeding edge
4. Your time is valuable - don't fight framework issues

## Quick Migration to Spring Boot 3.4.1

### For each `pom.xml`:

```xml
<properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <spring-boot.version>3.4.1</spring-boot.version>
</properties>
```

### Update Maven compiler plugin:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.14.0</version>
    <configuration>
        <source>21</source>
        <target>21</target>
    </configuration>
</plugin>
```

### Rebuild:

```bash
./stop_all.sh
for service in order-service graphql-service inventory-service notification-service; do
    (cd $service && mvn clean install -DskipTests)
done
./run_all.sh
```

### Test:

```bash
./test_system.sh
tail -1 order-service/logs/order-service.json.log | jq '{traceId, spanId, message}'
```

You should see actual trace IDs!

## Alternative: Wait for Spring Boot 4.1

Spring Boot 4.0.1 is a **milestone release** (not GA). Wait for 4.1 or 4.2 where these issues will be fixed.

## Bottom Line

**You have working distributed tracing to Tempo**, but **logs don't have trace IDs**.

Without correlated logs, you can't:
- Debug issues using logs
- Use Loki effectively  
- Understand request flow from logs
- Correlate errors with traces

**Fix this before going to production!**

---

**Created**: January 7, 2026  
**Priority**: 🔥 CRITICAL  
**Impact**: Makes distributed tracing mostly useless  
**Recommendation**: Downgrade to Spring Boot 3.4.1 + Java 21
