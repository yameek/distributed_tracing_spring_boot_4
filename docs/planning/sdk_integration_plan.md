# Tracing & Logging Integration Plan for Bits API SDK Generator

**Project:** Integrate distributed tracing and logging into the Bits API SDK Generator  
**Date:** January 7, 2026  
**Complexity:** Medium-High (3-6 months for full integration)

---

## Executive Summary

### What You Already Have ✅

**Bits API SDK Generator:**
- ✅ Generates client SDKs from Spring REST Controllers
- ✅ Parses controller annotations (@RestController, @GetMapping, etc.)
- ✅ Generates DTOs automatically
- ✅ Creates RestClient implementations
- ✅ Includes Circuit Breaker support
- ✅ Uses JSR 269 annotation processor

### What We'll Add 🎯

**Tracing & Logging Enhancement:**
- ✅ Add `@Observed` annotations to generated controllers
- ✅ Inject `Tracer` beans automatically
- ✅ Add structured logging with trace context
- ✅ Generate tracing configurations
- ✅ Add trace propagation to generated clients
- ✅ Create comprehensive logging setup

### Key Advantage

**You already have the hard parts done!**
- ✓ AST parsing infrastructure
- ✓ Code generation engine
- ✓ Build integration (Gradle)
- ✓ Annotation processing pipeline

**You just need to enhance the generators with tracing awareness.**

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Integration Strategy](#integration-strategy)
3. [Implementation Phases](#implementation-phases)
4. [Component Modifications](#component-modifications)
5. [New Components](#new-components)
6. [Configuration Design](#configuration-design)
7. [Code Examples](#code-examples)
8. [Testing Strategy](#testing-strategy)

---

## Architecture Overview

### Current Architecture (Simplified)

```
┌──────────────────────────────────────────────────────────────┐
│                   Input: Spring Controllers                   │
│            with @BitsSdk annotation                           │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│            Annotation Processor (JSR 269)                     │
│                                                                │
│  1. Scan classes with @BitsSdk                               │
│  2. Parse REST annotations                                    │
│  3. Extract types and mappings                               │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│                   Generators                                  │
│                                                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ Client       │  │ DTO          │  │ Config       │       │
│  │ Interface    │  │ Generator    │  │ Generator    │       │
│  │ Generator    │  │              │  │              │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
│                                                                │
│  ┌──────────────┐                                            │
│  │ Client       │                                            │
│  │ Implementation│                                           │
│  │ Generator    │                                            │
│  └──────────────┘                                            │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│                   Output: Client SDK                          │
│                                                                │
│  - Client Interface                                           │
│  - Client Implementation (with RestClient)                    │
│  - Request/Response DTOs                                      │
│  - Configuration classes                                      │
└──────────────────────────────────────────────────────────────┘
```

### Enhanced Architecture (With Tracing)

```
┌──────────────────────────────────────────────────────────────┐
│                   Input: Spring Controllers                   │
│            with @BitsSdk(enableTracing=true) ← NEW            │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│            Annotation Processor (JSR 269)                     │
│                                                                │
│  1. Scan classes with @BitsSdk                               │
│  2. Parse REST annotations                                    │
│  3. Extract types and mappings                               │
│  4. Extract tracing configuration ← NEW                      │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│                   Generators (Enhanced)                       │
│                                                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ Client       │  │ DTO          │  │ Config       │       │
│  │ Interface    │  │ Generator    │  │ Generator    │       │
│  │ Generator    │  │              │  │ + Tracing ← │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
│                                                                │
│  ┌──────────────┐  ┌──────────────┐ ← NEW                   │
│  │ Client       │  │ Tracing      │                          │
│  │ Implementation│  │ Enhancer     │                          │
│  │ + Tracing ←  │  └──────────────┘                          │
│  └──────────────┘                                            │
│                     ┌──────────────┐ ← NEW                   │
│                     │ Logging      │                          │
│                     │ Config Gen   │                          │
│                     └──────────────┘                          │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│            Output: Client SDK + Tracing Setup                 │
│                                                                │
│  - Client Interface                                           │
│  - Client Implementation (with tracing) ← ENHANCED            │
│  - Request/Response DTOs                                      │
│  - Configuration classes (with Tracer) ← ENHANCED            │
│  - application.yml (with tracing config) ← NEW               │
│  - logback-spring.xml ← NEW                                  │
│  - Tracing dependencies (pom.xml/build.gradle) ← NEW         │
└──────────────────────────────────────────────────────────────┘
```

---

## Integration Strategy

### Approach: Extend, Don't Replace

**Philosophy:** Enhance your existing generators with tracing capabilities rather than building a separate tool.

### Two-Mode Operation

```java
@BitsSdk(
    sdkPackage = "com.example.sdk.client",
    enableTracing = true  // ← NEW flag
)
public interface UserController {
    // Your existing controller
}
```

**Mode 1: Tracing Disabled (default - backward compatible)**
- Generates SDK as it does now
- No tracing code added
- Existing clients unaffected

**Mode 2: Tracing Enabled (opt-in)**
- Generates SDK with tracing
- Adds @Observed annotations
- Injects Tracer beans
- Generates configuration files
- Adds tracing dependencies

### Integration Points

| Component | Change Type | Complexity |
|-----------|-------------|------------|
| `@BitsSdk` annotation | Add optional parameter | ⭐ Easy |
| Annotation Processor | Detect tracing flag | ⭐ Easy |
| Client Implementation Generator | Add tracing code | ⭐⭐ Medium |
| Configuration Generator | Generate tracing config | ⭐⭐ Medium |
| Build File Generator | Add dependencies | ⭐⭐ Medium |
| Logging Generator | Generate logback-spring.xml | ⭐⭐⭐ Hard |

---

## Implementation Phases

### Phase 1: Foundation (2-3 weeks)
**Goal:** Basic tracing support for generated controllers

**Tasks:**
1. Extend `@BitsSdk` annotation with tracing options
2. Create `TracingConfiguration` model
3. Add tracing dependency injection to generated config
4. Generate basic application.yml with tracing

**Deliverable:**
```java
// Input Controller
@BitsSdk(sdkPackage = "com.example.sdk", enableTracing = true)
@RestController
public interface UserController {
    @GetMapping("/users/{id}")
    ResponseEntity<UserDto> getUser(@PathVariable Long id);
}

// Generated Configuration (with tracing)
@Configuration
public class UserClientConfiguration {
    
    private final Tracer tracer;  // ← NEW
    
    @Autowired
    public UserClientConfiguration(Tracer tracer) {  // ← NEW
        this.tracer = tracer;
    }
    
    // Rest of config...
}
```

### Phase 2: Client Instrumentation (2-3 weeks)
**Goal:** Add tracing to generated client calls

**Tasks:**
1. Enhance `ClientImplementationGenerator`
2. Add `@Observed` to generated methods
3. Inject trace context into HTTP headers
4. Add custom span attributes

**Deliverable:**
```java
// Generated Client Implementation (with tracing)
public class UserClientImpl implements UserClient {
    
    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final Tracer tracer;  // ← NEW
    
    @Override
    @Observed(name = "user.client.getUser", contextualName = "get-user")  // ← NEW
    public UserDto getUser(Long id) {
        
        // Add custom span attributes  // ← NEW
        if (tracer.currentSpan() != null) {
            tracer.currentSpan().tag("user.id", id.toString());
            tracer.currentSpan().tag("client.type", "rest");
        }
        
        return circuitBreaker.executeSupplier(() ->
            restClient.get()
                .uri("/users/" + id)
                // Trace context automatically propagated by Spring
                .retrieve()
                .body(UserDto.class)
        );
    }
}
```

### Phase 3: Server-Side Enhancement (3-4 weeks)
**Goal:** Add tracing to server controllers (optional feature)

**Tasks:**
1. Create `ServerTracingEnhancer`
2. Modify source controllers to add @Observed
3. Add structured logging
4. Generate logback-spring.xml

**Deliverable:**
```java
// Original Controller (input)
@RestController
@BitsSdk(sdkPackage = "com.example.sdk", enableTracing = true, enhanceServer = true)
public class UserController {
    @GetMapping("/users/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }
}

// Enhanced Controller (modified by processor)
@RestController
public class UserController {
    
    private static final Logger log = LoggerFactory.getLogger(UserController.class);  // ← NEW
    private final Tracer tracer;  // ← NEW
    
    @Autowired
    public UserController(UserService userService, Tracer tracer) {  // ← Modified
        this.userService = userService;
        this.tracer = tracer;
    }
    
    @GetMapping("/users/{id}")
    @Observed(name = "user.getUser", contextualName = "get-user-by-id")  // ← NEW
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        log.info("Fetching user: {}", id);  // ← NEW
        
        if (tracer.currentSpan() != null) {  // ← NEW
            tracer.currentSpan().tag("user.id", id.toString());
        }
        
        return ResponseEntity.ok(userService.getUser(id));
    }
}
```

### Phase 4: Advanced Features (2-3 weeks)
**Goal:** Complete tracing ecosystem

**Tasks:**
1. Add sampling configuration
2. Support custom span names
3. Add baggage propagation
4. Generate Grafana dashboards
5. Add tracing validation

**Deliverable:** Production-ready tracing integration

---

## Component Modifications

### 1. Enhance `@BitsSdk` Annotation

**File:** `bits-sdk-annotations/src/main/java/com/bracits/sdk/annotation/BitsSdk.java`

```java
package com.bracits.sdk.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface BitsSdk {
    
    /**
     * Package name for generated SDK client classes
     */
    String sdkPackage();
    
    /**
     * Enable distributed tracing for generated client and optionally server
     * @since 2.0.0
     */
    boolean enableTracing() default false;
    
    /**
     * Add tracing to server-side controller (modifies source)
     * Only applies if enableTracing = true
     * @since 2.0.0
     */
    boolean enhanceServer() default false;
    
    /**
     * Custom service name for tracing
     * Defaults to controller class name
     * @since 2.0.0
     */
    String serviceName() default "";
    
    /**
     * Sampling rate for tracing (0.0 to 1.0)
     * @since 2.0.0
     */
    double samplingRate() default 1.0;
    
    /**
     * Generate logging configuration (logback-spring.xml)
     * @since 2.0.0
     */
    boolean generateLoggingConfig() default true;
}
```

### 2. Create Tracing Configuration Model

**New File:** `bits-sdk-processor/src/main/java/com/bracits/sdk/model/TracingConfig.java`

```java
package com.bracits.sdk.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TracingConfig {
    
    /**
     * Whether tracing is enabled
     */
    private boolean enabled;
    
    /**
     * Whether to enhance server-side controller
     */
    private boolean enhanceServer;
    
    /**
     * Service name for tracing
     */
    private String serviceName;
    
    /**
     * Sampling rate (0.0 to 1.0)
     */
    private double samplingRate;
    
    /**
     * Whether to generate logging configuration
     */
    private boolean generateLoggingConfig;
    
    /**
     * Tempo endpoint for trace export
     */
    private String tempoEndpoint;
    
    /**
     * Loki endpoint for log aggregation
     */
    private String lokiEndpoint;
    
    /**
     * Whether to add custom span attributes
     */
    private boolean addSpanAttributes;
    
    /**
     * Whether to inject structured logging
     */
    private boolean structuredLogging;
    
    public static TracingConfig from(BitsSdk annotation, String defaultServiceName) {
        return TracingConfig.builder()
            .enabled(annotation.enableTracing())
            .enhanceServer(annotation.enhanceServer())
            .serviceName(annotation.serviceName().isEmpty() 
                ? defaultServiceName 
                : annotation.serviceName())
            .samplingRate(annotation.samplingRate())
            .generateLoggingConfig(annotation.generateLoggingConfig())
            .tempoEndpoint("http://localhost:4318/v1/traces")
            .lokiEndpoint("http://localhost:3100/loki/api/v1/push")
            .addSpanAttributes(true)
            .structuredLogging(true)
            .build();
    }
}
```

### 3. Enhance ClientImplementationGenerator

**File:** `bits-sdk-processor/src/main/java/com/bracits/sdk/generator/ClientImplementationGenerator.java`

**Add new method:**

```java
/**
 * Generate client method with tracing support
 */
private String generateMethodWithTracing(
        MethodInfo method, 
        String httpMethod, 
        String path,
        TracingConfig tracingConfig) {
    
    StringBuilder sb = new StringBuilder();
    
    // Add @Observed annotation
    if (tracingConfig.isEnabled()) {
        String spanName = String.format("%s.%s", 
            tracingConfig.getServiceName(), 
            method.getMethodName());
        
        sb.append("    @Observed(")
          .append("name = \"").append(spanName).append("\", ")
          .append("contextualName = \"").append(method.getMethodName()).append("\"")
          .append(")\n");
    }
    
    // Method signature
    sb.append("    @Override\n");
    sb.append("    public ").append(method.getReturnType()).append(" ")
      .append(method.getMethodName()).append("(");
    
    // Parameters
    sb.append(generateParameters(method));
    sb.append(") {\n");
    
    // Add logging if enabled
    if (tracingConfig.isStructuredLogging()) {
        sb.append("        log.info(\"Calling ")
          .append(method.getMethodName())
          .append(" with params: {}\", ");
        sb.append(generateLogParams(method));
        sb.append(");\n\n");
    }
    
    // Add custom span attributes
    if (tracingConfig.isAddSpanAttributes()) {
        sb.append(generateSpanAttributes(method));
    }
    
    // Circuit breaker + REST call (existing logic)
    sb.append(generateRestCall(method, httpMethod, path));
    
    sb.append("    }\n\n");
    
    return sb.toString();
}

/**
 * Generate custom span attributes
 */
private String generateSpanAttributes(MethodInfo method) {
    StringBuilder sb = new StringBuilder();
    
    sb.append("        // Add custom span attributes\n");
    sb.append("        if (tracer.currentSpan() != null) {\n");
    
    // Add method name
    sb.append("            tracer.currentSpan().tag(\"method\", \"")
      .append(method.getMethodName()).append("\");\n");
    
    // Add path variables as attributes
    for (ParameterInfo param : method.getParameters()) {
        if (param.isPathVariable()) {
            sb.append("            tracer.currentSpan().tag(\"")
              .append(param.getName())
              .append("\", String.valueOf(")
              .append(param.getName())
              .append("));\n");
        }
    }
    
    sb.append("        }\n\n");
    
    return sb.toString();
}
```

### 4. Create TracingConfigurationGenerator

**New File:** `bits-sdk-processor/src/main/java/com/bracits/sdk/generator/TracingConfigurationGenerator.java`

```java
package com.bracits.sdk.generator;

import com.bracits.sdk.model.TracingConfig;

public class TracingConfigurationGenerator {
    
    /**
     * Generate application.yml with tracing configuration
     */
    public String generateApplicationYml(TracingConfig config) {
        StringBuilder yml = new StringBuilder();
        
        yml.append("spring:\n");
        yml.append("  application:\n");
        yml.append("    name: ").append(config.getServiceName()).append("\n\n");
        
        yml.append("management:\n");
        yml.append("  endpoints:\n");
        yml.append("    web:\n");
        yml.append("      exposure:\n");
        yml.append("        include: health,metrics,prometheus\n");
        yml.append("  tracing:\n");
        yml.append("    sampling:\n");
        yml.append("      probability: ").append(config.getSamplingRate()).append("\n\n");
        
        yml.append("otel:\n");
        yml.append("  service:\n");
        yml.append("    name: ${spring.application.name}\n");
        yml.append("  exporter:\n");
        yml.append("    otlp:\n");
        yml.append("      endpoint: ").append(config.getTempoEndpoint()).append("\n");
        yml.append("      protocol: http/protobuf\n");
        yml.append("  propagators: tracecontext,baggage\n\n");
        
        yml.append("logging:\n");
        yml.append("  level:\n");
        yml.append("    root: INFO\n");
        yml.append("  pattern:\n");
        yml.append("    level: \"%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]\"\n");
        
        return yml.toString();
    }
    
    /**
     * Generate logback-spring.xml
     */
    public String generateLogbackXml(TracingConfig config) {
        return """
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
                        <url>%s</url>
                    </http>
                    <format>
                        <label>
                            <pattern>service=${serviceName},level=%%level</pattern>
                        </label>
                        <message>
                            <pattern>{"level":"%%level","message":"%%message","traceId":"%%X{traceId:-}"}</pattern>
                        </message>
                    </format>
                </appender>
                
                <root level="INFO">
                    <appender-ref ref="CONSOLE"/>
                    <appender-ref ref="LOKI"/>
                </root>
            </configuration>
            """.formatted(config.getLokiEndpoint());
    }
    
    /**
     * Generate Maven dependencies for tracing
     */
    public String generateMavenDependencies() {
        return """
            <!-- Distributed Tracing -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-actuator</artifactId>
            </dependency>
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
            """;
    }
    
    /**
     * Generate Gradle dependencies for tracing
     */
    public String generateGradleDependencies() {
        return """
            // Distributed Tracing
            implementation 'org.springframework.boot:spring-boot-starter-actuator'
            implementation 'io.micrometer:micrometer-tracing-bridge-otel'
            implementation 'io.opentelemetry:opentelemetry-exporter-otlp'
            
            // Logging
            implementation 'com.github.loki4j:loki-logback-appender:1.4.2'
            implementation 'net.logstash.logback:logstash-logback-encoder:8.0'
            """;
    }
}
```

### 5. Modify Annotation Processor

**File:** `bits-sdk-processor/src/main/java/com/bracits/sdk/processor/BitsSdkProcessor.java`

**Add tracing detection:**

```java
@Override
public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    
    for (Element element : roundEnv.getElementsAnnotatedWith(BitsSdk.class)) {
        
        if (element.getKind() != ElementKind.INTERFACE && 
            element.getKind() != ElementKind.CLASS) {
            continue;
        }
        
        TypeElement typeElement = (TypeElement) element;
        BitsSdk annotation = element.getAnnotation(BitsSdk.class);
        
        // Extract tracing configuration
        TracingConfig tracingConfig = TracingConfig.from(
            annotation, 
            typeElement.getSimpleName().toString()
        );
        
        try {
            // Generate client SDK (existing)
            generateClientSdk(typeElement, annotation, tracingConfig);
            
            // Generate tracing configurations if enabled
            if (tracingConfig.isEnabled()) {
                generateTracingConfigurations(typeElement, tracingConfig);
            }
            
            // Enhance server controller if requested
            if (tracingConfig.isEnhanceServer()) {
                enhanceServerController(typeElement, tracingConfig);
            }
            
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "Failed to generate SDK: " + e.getMessage(),
                element
            );
        }
    }
    
    return true;
}

/**
 * Generate tracing configuration files
 */
private void generateTracingConfigurations(
        TypeElement typeElement, 
        TracingConfig tracingConfig) throws IOException {
    
    TracingConfigurationGenerator generator = new TracingConfigurationGenerator();
    
    // Generate application.yml
    String applicationYml = generator.generateApplicationYml(tracingConfig);
    writeResourceFile("application.yml", applicationYml);
    
    // Generate logback-spring.xml
    if (tracingConfig.isGenerateLoggingConfig()) {
        String logbackXml = generator.generateLogbackXml(tracingConfig);
        writeResourceFile("logback-spring.xml", logbackXml);
    }
    
    // Generate dependency instructions
    String dependencies = isMaven() 
        ? generator.generateMavenDependencies()
        : generator.generateGradleDependencies();
    writeFile("TRACING_DEPENDENCIES.txt", dependencies);
}
```

---

## Configuration Design

### User Configuration Options

**Option 1: Annotation-based (Recommended)**

```java
@BitsSdk(
    sdkPackage = "com.example.sdk.client",
    enableTracing = true,
    enhanceServer = true,
    serviceName = "user-service",
    samplingRate = 1.0,
    generateLoggingConfig = true
)
public interface UserController {
    // Controller methods
}
```

**Option 2: External Configuration File**

**File:** `tracing-sdk.yml` (in project root)

```yaml
tracing:
  enabled: true
  enhance-server: true
  
  # Service configuration
  service:
    name: user-service
    version: 1.0.0
  
  # Sampling
  sampling:
    rate: 1.0  # 100% for dev, 0.1 for prod
  
  # Endpoints
  tempo:
    endpoint: http://localhost:4318/v1/traces
  loki:
    endpoint: http://localhost:3100/loki/api/v1/push
  
  # Features
  features:
    span-attributes: true
    structured-logging: true
    logging-config: true
  
  # Customization
  span-naming:
    format: "{service}.{method}"  # or "{service}.{class}.{method}"
  
  # Exclusions
  exclude:
    methods:
      - "healthCheck"
      - "metrics"
    paths:
      - "/actuator/**"
```

**Processor would read this file:**

```java
private TracingConfig loadTracingConfig(BitsSdk annotation, String serviceName) {
    // Try to load external config first
    File configFile = new File("tracing-sdk.yml");
    if (configFile.exists()) {
        TracingConfig config = yamlParser.parse(configFile);
        // Annotation overrides external config
        return mergeConfigs(config, annotation);
    }
    
    // Fall back to annotation
    return TracingConfig.from(annotation, serviceName);
}
```

---

## Code Examples

### Example 1: Basic Tracing Integration

**Input Controller:**

```java
@RestController
@BitsSdk(sdkPackage = "com.example.sdk", enableTracing = true)
public class ProductController {
    
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductDto> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }
    
    @PostMapping("/products")
    public ResponseEntity<ProductDto> createProduct(@RequestBody CreateProductRequest request) {
        return ResponseEntity.ok(productService.create(request));
    }
}
```

**Generated Client (with tracing):**

```java
@Component
public class ProductClientImpl implements ProductClient {
    
    private static final Logger log = LoggerFactory.getLogger(ProductClientImpl.class);
    
    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final Tracer tracer;  // ← Added
    
    @Autowired
    public ProductClientImpl(
            RestClient.Builder restClientBuilder,
            CircuitBreaker circuitBreaker,
            Tracer tracer) {  // ← Added
        this.restClient = restClientBuilder.build();
        this.circuitBreaker = circuitBreaker;
        this.tracer = tracer;
    }
    
    @Override
    @Observed(name = "product.client.getProduct", contextualName = "get-product")  // ← Added
    public ProductDto getProduct(Long id) {
        log.info("Fetching product: {}", id);  // ← Added
        
        // Add custom span attributes  // ← Added
        if (tracer.currentSpan() != null) {
            tracer.currentSpan().tag("product.id", id.toString());
            tracer.currentSpan().tag("operation", "getProduct");
        }
        
        return circuitBreaker.executeSupplier(() ->
            restClient.get()
                .uri("/products/" + id)
                .retrieve()
                .body(ProductDto.class)
        );
    }
    
    @Override
    @Observed(name = "product.client.createProduct", contextualName = "create-product")  // ← Added
    public ProductDto createProduct(CreateProductRequest request) {
        log.info("Creating product: {}", request.getName());  // ← Added
        
        if (tracer.currentSpan() != null) {  // ← Added
            tracer.currentSpan().tag("product.name", request.getName());
            tracer.currentSpan().tag("operation", "createProduct");
        }
        
        return circuitBreaker.executeSupplier(() ->
            restClient.post()
                .uri("/products")
                .body(request)
                .retrieve()
                .body(ProductDto.class)
        );
    }
}
```

**Generated Configuration:**

```java
@Configuration
public class ProductClientConfiguration {
    
    @Bean
    public RestClient.Builder productRestClientBuilder(ClientConfiguration config) {
        return RestClient.builder()
            .baseUrl(config.getBaseUrl())
            .defaultHeaders(headers -> {
                headers.set("User-Agent", "Product-Client-SDK/1.0");
                // Trace context automatically propagated by Spring
            });
    }
    
    @Bean
    public CircuitBreaker productCircuitBreaker(CircuitBreakerConfig config) {
        return CircuitBreaker.of("product-client", config.getConfig());
    }
    
    @Bean
    public ProductClient productClient(
            RestClient.Builder restClientBuilder,
            CircuitBreaker circuitBreaker,
            Tracer tracer) {  // ← Tracer injected automatically by Spring
        return new ProductClientImpl(restClientBuilder, circuitBreaker, tracer);
    }
}
```

**Generated application.yml:**

```yaml
spring:
  application:
    name: product-service

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  tracing:
    sampling:
      probability: 1.0

otel:
  service:
    name: ${spring.application.name}
  exporter:
    otlp:
      endpoint: http://localhost:4318/v1/traces
      protocol: http/protobuf
  propagators: tracecontext,baggage

logging:
  level:
    root: INFO
    com.example: DEBUG
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

---

### Example 2: Server Enhancement (Optional)

**Original Controller (input):**

```java
@RestController
@RequestMapping("/api/orders")
@BitsSdk(
    sdkPackage = "com.example.sdk",
    enableTracing = true,
    enhanceServer = true  // ← Enable server enhancement
)
public class OrderController {
    
    private final OrderService orderService;
    
    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }
    
    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }
}
```

**Enhanced Controller (generated/modified):**

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);  // ← Added
    
    private final OrderService orderService;
    private final Tracer tracer;  // ← Added
    
    @Autowired
    public OrderController(OrderService orderService, Tracer tracer) {  // ← Modified
        this.orderService = orderService;
        this.tracer = tracer;
    }
    
    @GetMapping("/{id}")
    @Observed(name = "order.getOrder", contextualName = "get-order-by-id")  // ← Added
    public ResponseEntity<OrderDto> getOrder(@PathVariable Long id) {
        log.info("Fetching order: {}", id);  // ← Added
        
        if (tracer.currentSpan() != null) {  // ← Added
            tracer.currentSpan().tag("order.id", id.toString());
            tracer.currentSpan().event("order.fetch.started");
        }
        
        OrderDto order = orderService.getOrder(id);
        
        if (tracer.currentSpan() != null) {  // ← Added
            tracer.currentSpan().event("order.fetch.completed");
            tracer.currentSpan().tag("order.status", order.getStatus());
        }
        
        return ResponseEntity.ok(order);
    }
    
    @PostMapping
    @Observed(name = "order.createOrder", contextualName = "create-order")  // ← Added
    public ResponseEntity<OrderDto> createOrder(@RequestBody CreateOrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());  // ← Added
        
        if (tracer.currentSpan() != null) {  // ← Added
            tracer.currentSpan().tag("customer.id", request.getCustomerId().toString());
            tracer.currentSpan().tag("order.items.count", String.valueOf(request.getItems().size()));
        }
        
        return ResponseEntity.ok(orderService.createOrder(request));
    }
}
```

---

## Testing Strategy

### Unit Tests

**Test tracing configuration generation:**

```java
@Test
void shouldGenerateTracingConfig() {
    TracingConfig config = TracingConfig.builder()
        .enabled(true)
        .serviceName("test-service")
        .samplingRate(1.0)
        .build();
    
    TracingConfigurationGenerator generator = new TracingConfigurationGenerator();
    String yml = generator.generateApplicationYml(config);
    
    assertThat(yml).contains("spring.application.name: test-service");
    assertThat(yml).contains("sampling.probability: 1.0");
}

@Test
void shouldGenerateLogbackXml() {
    TracingConfig config = TracingConfig.builder()
        .serviceName("test-service")
        .lokiEndpoint("http://localhost:3100")
        .build();
    
    TracingConfigurationGenerator generator = new TracingConfigurationGenerator();
    String xml = generator.generateLogbackXml(config);
    
    assertThat(xml).contains("<url>http://localhost:3100");
    assertThat(xml).contains("includeMdcKeyName>traceId");
}
```

### Integration Tests

**Test complete SDK generation with tracing:**

```java
@Test
void shouldGenerateClientWithTracing() {
    // Given: Controller with @BitsSdk(enableTracing = true)
    String controllerSource = """
        @RestController
        @BitsSdk(sdkPackage = "com.example.sdk", enableTracing = true)
        public interface UserController {
            @GetMapping("/users/{id}")
            ResponseEntity<UserDto> getUser(@PathVariable Long id);
        }
        """;
    
    // When: Process annotation
    Compilation compilation = compile(controllerSource);
    
    // Then: Generated client should have tracing
    assertThat(compilation).succeeded();
    assertThat(compilation)
        .generatedSourceFile("com.example.sdk.UserClientImpl")
        .hasSourceEquivalentTo(expectedClientWithTracing);
    
    // And: Configuration files generated
    assertThat(compilation)
        .generatedFile(StandardLocation.CLASS_OUTPUT, "application.yml")
        .exists();
}
```

### End-to-End Test

**Test actual tracing in running application:**

```java
@SpringBootTest
@AutoConfigureObservability
class TracingIntegrationTest {
    
    @Autowired
    private UserClient userClient;
    
    @Autowired
    private TestObservationRegistry observationRegistry;
    
    @Test
    void shouldTraceClientCall() {
        // When: Call client method
        userClient.getUser(123L);
        
        // Then: Observation should be recorded
        TestObservationRegistryAssert.assertThat(observationRegistry)
            .hasObservationWithNameEqualTo("user.client.getUser")
            .that()
            .hasHighCardinalityKeyValue("user.id", "123");
    }
}
```

---

## Timeline & Effort Estimation

### Phase 1: Foundation (2-3 weeks)
- **Effort:** 40-60 hours
- **Team:** 1 developer
- **Risk:** Low

### Phase 2: Client Instrumentation (2-3 weeks)
- **Effort:** 40-60 hours
- **Team:** 1 developer
- **Risk:** Medium

### Phase 3: Server Enhancement (3-4 weeks)
- **Effort:** 60-80 hours
- **Team:** 1-2 developers
- **Risk:** High (modifies source code)

### Phase 4: Advanced Features (2-3 weeks)
- **Effort:** 40-60 hours
- **Team:** 1 developer
- **Risk:** Low

**Total Timeline:** 3-6 months (depending on team size and priority)

---

## Success Criteria

### MVP Success (Phase 1-2)
- [ ] Generated clients have @Observed annotations
- [ ] Tracer beans injected automatically
- [ ] application.yml generated with tracing config
- [ ] Trace context propagates across client calls
- [ ] No breaking changes to existing SDK users

### Production Ready (Phase 3-4)
- [ ] Server controllers can be enhanced (opt-in)
- [ ] Structured logging with trace correlation
- [ ] logback-spring.xml generated correctly
- [ ] Custom span attributes added appropriately
- [ ] Sampling configuration works
- [ ] Documentation complete
- [ ] 90%+ test coverage

---

## Risk Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Breaking existing users | High | Make tracing opt-in (enableTracing=false by default) |
| Server code modification bugs | High | Extensive testing, make optional, provide rollback |
| Performance overhead | Medium | Configurable sampling, async logging |
| Dependency conflicts | Medium | Use Spring Boot BOM, test with multiple versions |
| Learning curve | Low | Comprehensive docs, examples, defaults that work |

---

## Next Steps (Action Items)

### Week 1: Planning
1. ✅ Review this plan with team
2. ✅ Decide on annotation vs config file approach
3. ✅ Set up development branch
4. ✅ Create project structure for new components

### Week 2-3: Phase 1 Implementation
1. ✅ Enhance @BitsSdk annotation
2. ✅ Create TracingConfig model
3. ✅ Implement TracingConfigurationGenerator
4. ✅ Modify annotation processor
5. ✅ Write unit tests

### Week 4-5: Phase 2 Implementation
1. ✅ Enhance ClientImplementationGenerator
2. ✅ Add @Observed annotation generation
3. ✅ Add Tracer injection
4. ✅ Add span attributes
5. ✅ Integration tests

### Week 6+: Continue with remaining phases...

---

## Advantages of This Approach

### For You
1. ✅ **Leverage existing infrastructure** - Don't rebuild what you have
2. ✅ **Incremental rollout** - Opt-in feature, no breaking changes
3. ✅ **Consistent with SDK philosophy** - Generate everything automatically
4. ✅ **Maintainable** - Single codebase, unified approach

### For Users
1. ✅ **Easy adoption** - Just add `enableTracing=true`
2. ✅ **Consistent tracing** - All clients traced the same way
3. ✅ **Best practices** - Tracing done right automatically
4. ✅ **Zero boilerplate** - No manual tracing code

### For The Ecosystem
1. ✅ **Standardization** - All SDKs trace consistently
2. ✅ **Observability** - Full distributed tracing out of the box
3. ✅ **Production-ready** - Proper logging, sampling, configuration

---

## Conclusion

**This integration is highly feasible because:**
1. ✅ You already have the hard parts (AST parsing, code generation)
2. ✅ Tracing additions are mostly additive (not invasive)
3. ✅ Can be done incrementally (phase by phase)
4. ✅ Backward compatible (opt-in feature)

**Timeline is much shorter than standalone SDK:**
- **MVP:** 1-2 months (vs 3-4 months standalone)
- **Production:** 3-6 months (vs 12 months standalone)

**Recommendation:** Start with Phase 1 (Foundation) immediately. It's low-risk and high-value.

---

*Created: January 7, 2026*
*Status: Ready for Implementation*
