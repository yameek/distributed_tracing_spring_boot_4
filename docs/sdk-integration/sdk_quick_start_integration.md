# Quick Start: Integrate Tracing into Bits API SDK

**Goal:** Get tracing working in your SDK in the next 2-4 weeks  
**Audience:** You (the SDK maintainer)  
**Date:** January 7, 2026

---

## 📋 Prerequisites Checklist

Before you start, ensure you have:

- [ ] Bits API SDK Generator source code
- [ ] Working knowledge of JSR 269 annotation processors
- [ ] Familiarity with your existing generators
- [ ] Access to `tracing-demo-v2` for reference
- [ ] IDE set up (IntelliJ IDEA recommended)

---

## 🎯 Phase 1 Quick Start (Week 1-2)

### Goal: Basic tracing support for generated clients

**Success Criteria:** Generated clients have @Observed annotations and Tracer injection

---

### Step 1: Extend @BitsSdk Annotation (30 minutes)

**File:** `bits-sdk-annotations/src/main/java/com/bracits/sdk/annotation/BitsSdk.java`

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface BitsSdk {
    
    String sdkPackage();
    
    // Add these new fields:
    
    /**
     * Enable distributed tracing
     */
    boolean enableTracing() default false;
    
    /**
     * Service name for tracing (defaults to controller name)
     */
    String serviceName() default "";
    
    /**
     * Sampling rate (0.0 to 1.0)
     */
    double samplingRate() default 1.0;
}
```

**Test it:**
```bash
cd bits-sdk-annotations
./gradlew clean build publishToMavenLocal
```

---

### Step 2: Create TracingConfig Model (30 minutes)

**File:** `bits-sdk-processor/src/main/java/com/bracits/sdk/model/TracingConfig.java`

```java
package com.bracits.sdk.model;

public class TracingConfig {
    private final boolean enabled;
    private final String serviceName;
    private final double samplingRate;
    
    public TracingConfig(boolean enabled, String serviceName, double samplingRate) {
        this.enabled = enabled;
        this.serviceName = serviceName;
        this.samplingRate = samplingRate;
    }
    
    public static TracingConfig from(BitsSdk annotation, String defaultServiceName) {
        String serviceName = annotation.serviceName().isEmpty() 
            ? defaultServiceName 
            : annotation.serviceName();
            
        return new TracingConfig(
            annotation.enableTracing(),
            serviceName,
            annotation.samplingRate()
        );
    }
    
    // Getters
    public boolean isEnabled() { return enabled; }
    public String getServiceName() { return serviceName; }
    public double getSamplingRate() { return samplingRate; }
}
```

---

### Step 3: Update Annotation Processor (1 hour)

**File:** `bits-sdk-processor/src/main/java/com/bracits/sdk/processor/BitsSdkProcessor.java`

**Find the process() method and add tracing detection:**

```java
@Override
public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    
    for (Element element : roundEnv.getElementsAnnotatedWith(BitsSdk.class)) {
        
        TypeElement typeElement = (TypeElement) element;
        BitsSdk annotation = element.getAnnotation(BitsSdk.class);
        
        // NEW: Extract tracing configuration
        TracingConfig tracingConfig = TracingConfig.from(
            annotation, 
            typeElement.getSimpleName().toString()
        );
        
        try {
            // Pass tracingConfig to generators
            generateClientSdk(typeElement, annotation, tracingConfig);
            
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
```

**Update generateClientSdk() signature:**

```java
private void generateClientSdk(
        TypeElement typeElement, 
        BitsSdk annotation, 
        TracingConfig tracingConfig) throws IOException {
    
    // Your existing code...
    
    // Pass tracingConfig to all generators
    ClientInterfaceGenerator interfaceGen = new ClientInterfaceGenerator(/* ... */);
    ClientImplementationGenerator implGen = new ClientImplementationGenerator(
        /* existing params */, 
        tracingConfig  // NEW
    );
    
    // Generate files...
}
```

---

### Step 4: Enhance ClientImplementationGenerator (2-3 hours)

**File:** `bits-sdk-processor/src/main/java/com/bracits/sdk/generator/ClientImplementationGenerator.java`

**Add TracingConfig field:**

```java
public class ClientImplementationGenerator {
    
    private final TracingConfig tracingConfig;  // NEW
    
    public ClientImplementationGenerator(
            /* existing params */,
            TracingConfig tracingConfig) {  // NEW
        // existing assignments...
        this.tracingConfig = tracingConfig;
    }
    
    // Rest of class...
}
```

**Find where you generate class fields, add Tracer:**

```java
private String generateFields() {
    StringBuilder sb = new StringBuilder();
    
    sb.append("    private final RestClient restClient;\n");
    sb.append("    private final CircuitBreaker circuitBreaker;\n");
    
    // NEW: Add tracer field if tracing enabled
    if (tracingConfig.isEnabled()) {
        sb.append("    private final Tracer tracer;\n");
    }
    
    return sb.toString();
}
```

**Find constructor generation, add Tracer injection:**

```java
private String generateConstructor(String className) {
    StringBuilder sb = new StringBuilder();
    
    sb.append("    @Autowired\n");
    sb.append("    public ").append(className).append("(\n");
    sb.append("            RestClient.Builder restClientBuilder,\n");
    sb.append("            CircuitBreaker circuitBreaker");
    
    // NEW: Add tracer parameter
    if (tracingConfig.isEnabled()) {
        sb.append(",\n            Tracer tracer");
    }
    
    sb.append(") {\n");
    sb.append("        this.restClient = restClientBuilder.build();\n");
    sb.append("        this.circuitBreaker = circuitBreaker;\n");
    
    // NEW: Assign tracer
    if (tracingConfig.isEnabled()) {
        sb.append("        this.tracer = tracer;\n");
    }
    
    sb.append("    }\n\n");
    
    return sb.toString();
}
```

**Find method generation, add @Observed:**

```java
private String generateMethod(MethodInfo method, String httpMethod, String path) {
    StringBuilder sb = new StringBuilder();
    
    // NEW: Add @Observed annotation
    if (tracingConfig.isEnabled()) {
        String spanName = tracingConfig.getServiceName() + ".client." + method.getMethodName();
        sb.append("    @Observed(")
          .append("name = \"").append(spanName).append("\", ")
          .append("contextualName = \"").append(method.getMethodName()).append("\"")
          .append(")\n");
    }
    
    // Existing method generation
    sb.append("    @Override\n");
    sb.append("    public ").append(method.getReturnType()).append(" ")
      .append(method.getMethodName()).append("(");
    // ... rest of method ...
    
    return sb.toString();
}
```

**Add imports at the top of generated file:**

```java
private String generateImports() {
    StringBuilder sb = new StringBuilder();
    
    // Existing imports...
    sb.append("import org.springframework.web.client.RestClient;\n");
    sb.append("import io.github.resilience4j.circuitbreaker.CircuitBreaker;\n");
    
    // NEW: Add tracing imports
    if (tracingConfig.isEnabled()) {
        sb.append("import io.micrometer.tracing.Tracer;\n");
        sb.append("import io.micrometer.observation.annotation.Observed;\n");
    }
    
    return sb.toString();
}
```

---

### Step 5: Test the Basic Integration (1 hour)

**Create test controller:**

**File:** `test-project/src/main/java/com/example/TestController.java`

```java
package com.example;

import com.bracits.sdk.annotation.BitsSdk;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
@BitsSdk(
    sdkPackage = "com.example.sdk.client",
    enableTracing = true,
    serviceName = "test-service"
)
public interface TestController {
    
    @GetMapping("/test/{id}")
    ResponseEntity<String> getTest(@PathVariable Long id);
}
```

**Build and verify:**

```bash
cd test-project
./gradlew clean build

# Check generated client
cat build/generated/sources/annotationProcessor/java/main/com/example/sdk/client/TestClientImpl.java
```

**Expected output should include:**

```java
import io.micrometer.tracing.Tracer;
import io.micrometer.observation.annotation.Observed;

public class TestClientImpl implements TestClient {
    
    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final Tracer tracer;  // ← Should be present
    
    @Autowired
    public TestClientImpl(
            RestClient.Builder restClientBuilder,
            CircuitBreaker circuitBreaker,
            Tracer tracer) {  // ← Should be present
        this.restClient = restClientBuilder.build();
        this.circuitBreaker = circuitBreaker;
        this.tracer = tracer;
    }
    
    @Override
    @Observed(name = "test-service.client.getTest", contextualName = "getTest")  // ← Should be present
    public String getTest(Long id) {
        // ... rest of method
    }
}
```

**If it looks like this, you're done with Phase 1! 🎉**

---

## 🎯 Phase 2 Quick Start (Week 3-4)

### Goal: Add custom span attributes and logging

---

### Step 6: Add Span Attributes (2 hours)

**File:** `ClientImplementationGenerator.java`

**Add method to generate span attributes:**

```java
private String generateSpanAttributes(MethodInfo method) {
    if (!tracingConfig.isEnabled()) {
        return "";
    }
    
    StringBuilder sb = new StringBuilder();
    sb.append("        \n");
    sb.append("        // Add custom span attributes\n");
    sb.append("        if (tracer.currentSpan() != null) {\n");
    sb.append("            tracer.currentSpan().tag(\"method\", \"")
      .append(method.getMethodName()).append("\");\n");
    
    // Add path variables as span attributes
    for (ParameterInfo param : method.getParameters()) {
        if (param.isPathVariable()) {
            sb.append("            tracer.currentSpan().tag(\"")
              .append(param.getName())
              .append("\", String.valueOf(")
              .append(param.getName())
              .append("));\n");
        }
    }
    
    sb.append("        }\n");
    return sb.toString();
}
```

**Insert into method generation (before REST call):**

```java
private String generateMethod(MethodInfo method, String httpMethod, String path) {
    StringBuilder sb = new StringBuilder();
    
    // @Observed annotation (from Phase 1)
    if (tracingConfig.isEnabled()) {
        // ... @Observed code ...
    }
    
    // Method signature
    sb.append("    @Override\n");
    sb.append("    public ").append(method.getReturnType()).append(" ")
      .append(method.getMethodName()).append("(");
    sb.append(generateParameters(method));
    sb.append(") {\n");
    
    // NEW: Add span attributes
    sb.append(generateSpanAttributes(method));
    
    // Rest of method (circuit breaker, REST call)
    sb.append(generateRestCall(method, httpMethod, path));
    
    sb.append("    }\n\n");
    return sb.toString();
}
```

---

### Step 7: Add Structured Logging (1 hour)

**Add logger field:**

```java
private String generateFields() {
    StringBuilder sb = new StringBuilder();
    
    // NEW: Add logger
    if (tracingConfig.isEnabled()) {
        sb.append("    private static final Logger log = LoggerFactory.getLogger(")
          .append(className).append(".class);\n\n");
    }
    
    sb.append("    private final RestClient restClient;\n");
    sb.append("    private final CircuitBreaker circuitBreaker;\n");
    
    if (tracingConfig.isEnabled()) {
        sb.append("    private final Tracer tracer;\n");
    }
    
    return sb.toString();
}
```

**Add logging to methods:**

```java
private String generateMethod(MethodInfo method, String httpMethod, String path) {
    StringBuilder sb = new StringBuilder();
    
    // ... @Observed annotation ...
    
    // Method signature
    sb.append("    @Override\n");
    sb.append("    public ").append(method.getReturnType()).append(" ")
      .append(method.getMethodName()).append("(");
    sb.append(generateParameters(method));
    sb.append(") {\n");
    
    // NEW: Add logging
    if (tracingConfig.isEnabled()) {
        sb.append("        log.info(\"Calling ")
          .append(method.getMethodName())
          .append("(");
        
        // Add parameters to log
        List<ParameterInfo> params = method.getParameters();
        for (int i = 0; i < params.size(); i++) {
            sb.append(params.get(i).getName()).append("={}");
            if (i < params.size() - 1) sb.append(", ");
        }
        sb.append(")\", ");
        
        for (int i = 0; i < params.size(); i++) {
            sb.append(params.get(i).getName());
            if (i < params.size() - 1) sb.append(", ");
        }
        sb.append(");\n");
    }
    
    // Span attributes
    sb.append(generateSpanAttributes(method));
    
    // REST call
    sb.append(generateRestCall(method, httpMethod, path));
    
    sb.append("    }\n\n");
    return sb.toString();
}
```

**Add import:**

```java
private String generateImports() {
    StringBuilder sb = new StringBuilder();
    
    // ... existing imports ...
    
    if (tracingConfig.isEnabled()) {
        sb.append("import org.slf4j.Logger;\n");
        sb.append("import org.slf4j.LoggerFactory;\n");
        sb.append("import io.micrometer.tracing.Tracer;\n");
        sb.append("import io.micrometer.observation.annotation.Observed;\n");
    }
    
    return sb.toString();
}
```

---

### Step 8: Create Configuration Generator (3 hours)

**File:** `bits-sdk-processor/src/main/java/com/bracits/sdk/generator/TracingConfigurationGenerator.java`

```java
package com.bracits.sdk.generator;

import com.bracits.sdk.model.TracingConfig;

public class TracingConfigurationGenerator {
    
    private final TracingConfig config;
    
    public TracingConfigurationGenerator(TracingConfig config) {
        this.config = config;
    }
    
    /**
     * Generate application.yml with tracing configuration
     */
    public String generateApplicationYml() {
        return String.format("""
            spring:
              application:
                name: %s
            
            management:
              endpoints:
                web:
                  exposure:
                    include: health,metrics,prometheus
              tracing:
                sampling:
                  probability: %.2f
            
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
              pattern:
                level: "%%5p [${spring.application.name:},%%X{traceId:-},%%X{spanId:-}]"
            """, 
            config.getServiceName(),
            config.getSamplingRate()
        );
    }
    
    /**
     * Generate dependencies instructions
     */
    public String generateDependenciesInstructions() {
        return """
            # Add these dependencies to your build.gradle:
            
            dependencies {
                implementation 'org.springframework.boot:spring-boot-starter-actuator'
                implementation 'io.micrometer:micrometer-tracing-bridge-otel'
                implementation 'io.opentelemetry:opentelemetry-exporter-otlp'
            }
            
            # Or if using Maven, add to pom.xml:
            
            <dependencies>
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
            </dependencies>
            """;
    }
}
```

---

### Step 9: Integrate Configuration Generator (1 hour)

**File:** `BitsSdkProcessor.java`

```java
private void generateClientSdk(
        TypeElement typeElement, 
        BitsSdk annotation, 
        TracingConfig tracingConfig) throws IOException {
    
    // Existing client generation...
    ClientInterfaceGenerator interfaceGen = new ClientInterfaceGenerator(/* ... */);
    ClientImplementationGenerator implGen = new ClientImplementationGenerator(
        /* ... */, tracingConfig
    );
    
    // Generate client files (existing)
    String interfaceCode = interfaceGen.generate();
    String implCode = implGen.generate();
    writeJavaFile(interfaceCode, /* ... */);
    writeJavaFile(implCode, /* ... */);
    
    // NEW: Generate tracing configurations if enabled
    if (tracingConfig.isEnabled()) {
        generateTracingConfigurations(tracingConfig);
    }
}

private void generateTracingConfigurations(TracingConfig tracingConfig) throws IOException {
    TracingConfigurationGenerator generator = new TracingConfigurationGenerator(tracingConfig);
    
    // Generate application.yml
    String applicationYml = generator.generateApplicationYml();
    writeResourceFile("application-tracing.yml", applicationYml);
    
    // Generate dependencies instructions
    String dependencies = generator.generateDependenciesInstructions();
    writeTextFile("TRACING_SETUP_INSTRUCTIONS.txt", dependencies);
}

private void writeResourceFile(String filename, String content) throws IOException {
    // Get resources directory
    FileObject fileObject = processingEnv.getFiler().createResource(
        StandardLocation.CLASS_OUTPUT,
        "",
        filename
    );
    
    try (Writer writer = fileObject.openWriter()) {
        writer.write(content);
    }
}

private void writeTextFile(String filename, String content) throws IOException {
    FileObject fileObject = processingEnv.getFiler().createResource(
        StandardLocation.CLASS_OUTPUT,
        "",
        filename
    );
    
    try (Writer writer = fileObject.openWriter()) {
        writer.write(content);
    }
}
```

---

### Step 10: End-to-End Test (2 hours)

**Create full test project:**

```bash
mkdir -p tracing-sdk-test
cd tracing-sdk-test
```

**File:** `build.gradle`

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.example'
version = '1.0.0'
sourceCompatibility = '21'

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // Your SDK
    annotationProcessor 'com.bracits:bits-sdk-processor:1.0.0'
    implementation 'com.bracits:bits-sdk-annotations:1.0.0'
    
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    
    // Tracing (add these after seeing TRACING_SETUP_INSTRUCTIONS.txt)
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-tracing-bridge-otel'
    implementation 'io.opentelemetry:opentelemetry-exporter-otlp'
    
    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

**File:** `src/main/java/com/example/UserController.java`

```java
package com.example;

import com.bracits.sdk.annotation.BitsSdk;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/users")
@BitsSdk(
    sdkPackage = "com.example.sdk.client",
    enableTracing = true,
    serviceName = "user-service",
    samplingRate = 1.0
)
public interface UserController {
    
    @GetMapping("/{id}")
    ResponseEntity<UserDto> getUser(@PathVariable Long id);
    
    @PostMapping
    ResponseEntity<UserDto> createUser(@RequestBody CreateUserRequest request);
}
```

**Build:**

```bash
./gradlew clean build
```

**Check generated files:**

```bash
# Check client implementation
cat build/generated/sources/annotationProcessor/java/main/com/example/sdk/client/UserClientImpl.java

# Check configuration
cat build/resources/main/application-tracing.yml

# Check setup instructions
cat build/resources/main/TRACING_SETUP_INSTRUCTIONS.txt
```

**Expected client code:**

```java
@Component
public class UserClientImpl implements UserClient {
    
    private static final Logger log = LoggerFactory.getLogger(UserClientImpl.class);
    
    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final Tracer tracer;
    
    @Autowired
    public UserClientImpl(
            RestClient.Builder restClientBuilder,
            CircuitBreaker circuitBreaker,
            Tracer tracer) {
        this.restClient = restClientBuilder.build();
        this.circuitBreaker = circuitBreaker;
        this.tracer = tracer;
    }
    
    @Override
    @Observed(name = "user-service.client.getUser", contextualName = "getUser")
    public UserDto getUser(Long id) {
        log.info("Calling getUser(id={})", id);
        
        // Add custom span attributes
        if (tracer.currentSpan() != null) {
            tracer.currentSpan().tag("method", "getUser");
            tracer.currentSpan().tag("id", String.valueOf(id));
        }
        
        return circuitBreaker.executeSupplier(() ->
            restClient.get()
                .uri("/api/users/" + id)
                .retrieve()
                .body(UserDto.class)
        );
    }
    
    // ... createUser method similar ...
}
```

**If this works, Phase 2 is complete! 🎉🎉**

---

## 🎯 Validation Checklist

Before moving to production:

### Code Generation
- [ ] Client has `Tracer` field
- [ ] Client constructor accepts `Tracer` parameter
- [ ] Methods have `@Observed` annotation
- [ ] Span attributes are added
- [ ] Logger is present
- [ ] Log statements are added
- [ ] Imports are correct

### Configuration Files
- [ ] `application-tracing.yml` generated
- [ ] `TRACING_SETUP_INSTRUCTIONS.txt` generated
- [ ] Service name is correct
- [ ] Sampling rate is correct

### Functionality
- [ ] Builds without errors
- [ ] No missing dependencies
- [ ] Works with `enableTracing = false` (backward compatible)
- [ ] Works with `enableTracing = true`

---

## 🚀 Production Deployment

### Step 11: Update SDK Version

**File:** `build.gradle` (root)

```gradle
version = '2.0.0'  // Major version for tracing feature
```

### Step 12: Publish to Maven Local (for testing)

```bash
./gradlew clean build publishToMavenLocal
```

### Step 13: Test in Real Project

```bash
cd your-real-project
```

**Update dependency:**

```gradle
dependencies {
    annotationProcessor 'com.bracits:bits-sdk-processor:2.0.0'
    implementation 'com.bracits:bits-sdk-annotations:2.0.0'
}
```

**Add tracing to one controller:**

```java
@BitsSdk(
    sdkPackage = "...",
    enableTracing = true  // ← Just add this!
)
```

**Build and verify:**

```bash
./gradlew clean build
```

---

## 📚 Reference Implementation

For complete working examples, see:
- `tracing-demo-v2/` - Working microservices with tracing
- `COMPREHENSIVE_IMPLEMENTATION_GUIDE.md` - Detailed tracing guide
- `QUICK_REFERENCE.md` - Code snippets and templates

---

## 🐛 Troubleshooting

### Issue: Generated code won't compile

**Symptom:** Errors about missing `Tracer` or `@Observed`

**Solution:**
```gradle
dependencies {
    // Make sure these are present
    implementation 'io.micrometer:micrometer-tracing-bridge-otel'
}
```

---

### Issue: `application-tracing.yml` not generated

**Symptom:** No configuration file in `build/resources/main/`

**Solution:**
- Check that `tracingConfig.isEnabled()` returns `true`
- Add debug logging in `generateTracingConfigurations()`
- Verify `writeResourceFile()` is called

---

### Issue: Tracer is null at runtime

**Symptom:** `NullPointerException` when accessing `tracer.currentSpan()`

**Solution:**
```java
// Always check for null
if (tracer != null && tracer.currentSpan() != null) {
    tracer.currentSpan().tag("key", "value");
}
```

---

## ⏱️ Timeline Summary

| Phase | Duration | Deliverable |
|-------|----------|-------------|
| Week 1 | 8-10 hours | Basic tracing support (Steps 1-5) |
| Week 2 | 8-10 hours | Span attributes & logging (Steps 6-7) |
| Week 3 | 10-12 hours | Configuration generation (Steps 8-9) |
| Week 4 | 6-8 hours | Testing & polish (Step 10) |

**Total:** 32-40 hours over 4 weeks

---

## 🎉 Success!

You now have:
- ✅ Tracing-enabled SDK generator
- ✅ Automatic @Observed annotations
- ✅ Tracer injection
- ✅ Custom span attributes
- ✅ Structured logging
- ✅ Configuration generation
- ✅ Backward compatibility

**Next:** Roll out to all services by adding `enableTracing = true`!

---

## 📞 Need Help?

1. **Review detailed plan:** `TRACING_SDK_INTEGRATION_PLAN.md`
2. **Check working example:** `tracing-demo-v2/`
3. **Read implementation guide:** `COMPREHENSIVE_IMPLEMENTATION_GUIDE.md`
4. **Quick reference:** `QUICK_REFERENCE.md`

---

*Created: January 7, 2026*  
*Your path to distributed tracing excellence! 🚀*
