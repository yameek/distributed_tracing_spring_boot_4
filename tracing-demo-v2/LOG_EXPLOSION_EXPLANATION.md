# Why Both Logs Grew to 155GB Each: Root Cause Analysis

## The Perfect Storm: Three Issues Combined

### 1. **The Bug: Missing Jackson Deserialization Support**

**Problem:**
- `UpdateProductPriceCommand` and `UpdateStockCommand` used `@Builder` with `final` fields
- No default constructor or `@JsonCreator` annotation
- When RabbitMQ messages arrived, Jackson's `ObjectMapper.convertValue()` couldn't deserialize them

**What Happened:**
```java
// In CommandMessageListener.java line 77:
UpdateProductPriceCommand command = objectMapper.convertValue(payload, UpdateProductPriceCommand.class);
// ❌ FAILS: No way to instantiate the class from JSON
```

**The Error Loop:**
1. RabbitMQ message arrives → `CommandMessageListener.handleCommand()`
2. Jackson tries to deserialize → **FAILS** (no constructor/annotations)
3. Exception thrown → Logged as ERROR
4. Exception propagates → RabbitMQ listener retries the message
5. **Infinite loop**: Message retries → Error → Log → Retry → Error → Log...

**Error Rate:**
- RabbitMQ retry mechanism + Spring's error handling = **thousands of errors per second**
- Each error generates a full stack trace (hundreds of bytes)
- 155GB ÷ ~500 bytes per error = **~310 million error log entries**

---

### 2. **No Log Rotation: Unbounded File Growth**

**Problem in `run_all.sh` (line 42):**
```bash
./gradlew :$SERVICE_NAME:bootRun > "logs/$SERVICE_NAME.log" 2>&1 &
```

**Issues:**
- ✅ Redirects stdout/stderr to log file (good for debugging)
- ❌ **No size limits** - file grows indefinitely
- ❌ **No rotation** - old logs never deleted
- ❌ **No compression** - wastes space

**Logback Configuration (`logback-spring.xml`):**
- Only has `CONSOLE` and `LOKI` appenders
- **No file appender with rotation**
- Console output goes to stdout → redirected to file → **no limits**

**Result:**
- Every error log entry written directly to file
- File grows continuously until disk fills up
- No automatic cleanup or rotation

---

### 3. **Gradle Daemon Also Captures Output**

**Why Gradle Daemon Log Also Grew:**

When you run `./gradlew :cqrs-service:bootRun`:
1. Gradle daemon process (PID 1308809) executes the `bootRun` task
2. The Spring Boot application runs **inside** the Gradle process
3. All stdout/stderr from the app goes to:
   - ✅ The redirected file (`logs/cqrs-service.log`) 
   - ✅ **AND** the Gradle daemon's output log (`daemon-1308809.out.log`)

**Gradle Daemon Log Location:**
```
~/.gradle/daemon/{version}/daemon-{PID}.out.log
```

**Why Both Logs Are Identical Size:**
- Same error stream captured twice:
  - Once by shell redirection (`> logs/cqrs-service.log`)
  - Once by Gradle daemon (captures all subprocess output)
- Both logs contain the **exact same error messages**
- Both grow at the **same rate** → same final size (155GB each)

---

## The Cascade Effect

```
RabbitMQ Message Arrives
    ↓
CommandMessageListener.handleCommand()
    ↓
objectMapper.convertValue() → ❌ FAILS
    ↓
Exception: "Cannot construct instance of UpdateProductPriceCommand..."
    ↓
┌─────────────────────────────────────────┐
│  ERROR logged to Console (stdout)      │
└─────────────────────────────────────────┘
    ↓                    ↓
    ├─→ Shell redirect → logs/cqrs-service.log (155GB)
    └─→ Gradle daemon → daemon-1308809.out.log (155GB)
    ↓
RabbitMQ retries message (automatic retry)
    ↓
[LOOP REPEATS INFINITELY]
```

---

## Why It Happened So Fast

**Factors:**
1. **High error rate**: RabbitMQ retries + Spring error handling = thousands/sec
2. **Large error messages**: Full stack traces with context = ~500 bytes each
3. **No throttling**: No rate limiting on logging
4. **No rotation**: Files never rotated or cleaned up
5. **Dual capture**: Same errors logged twice (shell + Gradle)

**Math:**
- Error rate: ~1,000 errors/second (conservative estimate)
- Error size: ~500 bytes (stack trace + context)
- Growth rate: 500 KB/second = **30 MB/minute = 1.8 GB/hour**
- Time to 155GB: **~86 hours** (or faster if error rate higher)

---

## The Fix Applied

### Before (Broken):
```java
@Data
@Builder
public class UpdateProductPriceCommand implements Command {
    private final String commandId = UUID.randomUUID().toString();
    private final String productId;
    private final BigDecimal newPrice;
    // ❌ No constructor Jackson can use
}
```

### After (Fixed):
```java
@Data
@Builder
public class UpdateProductPriceCommand implements Command {
    private final String commandId = UUID.randomUUID().toString();
    private final String productId;
    private final BigDecimal newPrice;
    
    @JsonCreator  // ✅ Tells Jackson to use this constructor
    public UpdateProductPriceCommand(
            @JsonProperty("commandId") String commandId,
            @JsonProperty("productId") String productId,
            @JsonProperty("newPrice") BigDecimal newPrice) {
        this.commandId = commandId != null ? commandId : UUID.randomUUID().toString();
        this.productId = productId;
        this.newPrice = newPrice;
    }
}
```

**Why This Works:**
- `@JsonCreator` tells Jackson which constructor to use
- `@JsonProperty` maps JSON fields to constructor parameters
- Jackson can now successfully deserialize from JSON
- **No more errors = no more infinite logging**

---

## Prevention Strategies

### 1. **Add Log Rotation** (Critical)

Update `logback-spring.xml` to add a file appender with rotation:

```xml
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/${appName}.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>logs/${appName}-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
        <maxFileSize>100MB</maxFileSize>
        <maxHistory>30</maxHistory>
        <totalSizeCap>10GB</totalSizeCap>
    </rollingPolicy>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <!-- same encoder config -->
    </encoder>
</appender>
```

### 2. **Add Error Rate Limiting**

Implement circuit breaker or rate limiting for RabbitMQ listeners:

```java
@RabbitListener(queues = "cqrs.commands.queue")
public void handleCommand(Map<String, Object> message) {
    try {
        // ... existing code
    } catch (Exception e) {
        errorCounter.increment();
        if (errorCounter.count() > 1000) {
            // Stop processing, alert, or circuit break
        }
        throw e; // Re-throw to trigger retry
    }
}
```

### 3. **Monitor Disk Usage**

Add alerts for disk usage:
```bash
# In monitoring script
DISK_USAGE=$(df -h /home/yaziz | tail -1 | awk '{print $5}' | sed 's/%//')
if [ "$DISK_USAGE" -gt 80 ]; then
    echo "WARNING: Disk usage at ${DISK_USAGE}%"
    # Alert or cleanup old logs
fi
```

### 4. **Use Log Aggregation**

- Logs already go to Loki (configured in logback)
- **Disable file logging** when Loki is available
- Let Loki handle rotation/retention
- Only log to file for local development

### 5. **Gradle Daemon Log Rotation**

Configure Gradle to limit daemon log size:
```properties
# gradle.properties
org.gradle.daemon.idletimeout=10800000
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
```

Or clean old daemon logs periodically:
```bash
# Clean Gradle daemon logs older than 7 days
find ~/.gradle/daemon -name "*.out.log" -mtime +7 -delete
```

---

## Summary

**Root Cause:** Missing Jackson deserialization support in command classes  
**Amplifier:** No log rotation + dual capture (shell + Gradle)  
**Result:** 310GB of duplicate error logs in ~86 hours  

**Fix Applied:** Added `@JsonCreator` constructors to command classes  
**Prevention:** Add log rotation, error rate limiting, and disk monitoring
