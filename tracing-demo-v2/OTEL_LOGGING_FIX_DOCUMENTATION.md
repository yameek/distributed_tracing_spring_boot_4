# OpenTelemetry Logging Fix Documentation

## Branch Comparison: `master` vs `otel_collector_log`

This document explains the issues found in the `master` branch regarding log collection and how they were fixed in the `otel_collector_log` branch.

---

## Executive Summary

| Aspect | Master Branch | otel_collector_log Branch |
|--------|---------------|---------------------------|
| **Log Flow** | Services → Loki (Direct) | Services → OTel Collector → Loki |
| **Log Protocol** | Loki4j Push API | OTLP (OpenTelemetry Protocol) |
| **Trace Correlation** | Manual MDC extraction | Native OpenTelemetry correlation |
| **Result** | Logs NOT reaching Loki | Logs successfully in Loki with traceId/spanId |

---

## Problem Statement

In the `master` branch, **logs were not being ingested into Grafana Loki**. The observability stack was incomplete:

- ✅ **Traces**: Working (Services → OTel Collector → Tempo)
- ❌ **Logs**: NOT working (Services → ??? → Loki empty)
- ✅ **Metrics**: Working (via Prometheus)

### Symptoms Observed
1. Loki API returned `{"status":"success"}` with no labels
2. Loki queries returned `total_entries=0`
3. OTel Collector only showed `"otelcol.signal": "traces"`, no logs signal
4. Service logs were only written to files, not sent to any centralized location

---

## Root Cause Analysis

### Issue 1: Wrong Logging Architecture in Master

**Master Branch Approach:**
```
┌─────────────┐     ┌─────────────┐
│   Service   │ ──► │    Loki     │  (Direct push via Loki4j)
│  (Loki4j)   │     │             │
└─────────────┘     └─────────────┘
```

**Problems:**
- Used `loki4j` library to push logs directly to Loki
- This bypassed the OTel Collector entirely
- Inconsistent with the traces architecture (which used OTel Collector)
- Loki4j appender was configured to push to `http://localhost:3100` but services run in different network contexts

**Master Branch `build.gradle`:**
```groovy
// Logging
implementation 'com.github.loki4j:loki-logback-appender:1.4.2'
implementation 'net.logstash.logback:logstash-logback-encoder:8.0'
```

**Master Branch `logback-spring.xml`:**
```xml
<!-- Loki appender for centralized logging -->
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <http>
        <url>http://localhost:3100/loki/api/v1/push</url>
    </http>
    ...
</appender>
```

---

### Issue 2: No OTLP Log Export Configuration

**Master Branch `application.yml`:**
```yaml
management:
  opentelemetry:
    logging:
      export:
        otlp:
          enabled: false  # ❌ Logging export was DISABLED!
```

Even if OTLP logging was intended, it was explicitly disabled.

---

### Issue 3: OTel Collector Had No Logs Pipeline

**Master Branch `otel-collector-config.yaml`:**
```yaml
service:
  pipelines:
    traces:                    # ✅ Only traces pipeline
      receivers: [otlp]
      processors: [memory_limiter, batch]
      exporters: [otlp, debug]
    # ❌ NO logs pipeline defined!
```

The OTel Collector was only configured to handle traces, not logs.

---

### Issue 4: Loki Had No OTLP Configuration

**Master Branch `loki.yaml`:**
```yaml
auth_enabled: false

server:
  http_listen_port: 3100
  grpc_listen_port: 9096

# ❌ NO OTLP ingestion configuration
# ❌ NO structured metadata configuration
```

Loki was not configured to accept OTLP log ingestion.

---

## The Fix: Unified OTLP Log Pipeline

### New Architecture (otel_collector_log Branch)

```
┌─────────────┐     ┌─────────────────┐     ┌─────────────┐
│   Service   │ ──► │  OTel Collector │ ──► │    Loki     │
│ (OTLP SDK)  │     │  (logs pipeline)│     │   (OTLP)    │
└─────────────┘     └─────────────────┘     └─────────────┘
        │                   │
        │                   ▼
        │           ┌─────────────┐
        │           │    Tempo    │
        └──────────►│  (traces)   │
                    └─────────────┘
```

**Benefits:**
- Consistent architecture for both traces AND logs
- Vendor-neutral: can switch from Loki to any OTLP-compatible log backend
- Native trace correlation (traceId/spanId automatically included)
- Centralized processing and routing

---

## Detailed Changes

### 1. Dependency Change: Loki4j → OpenTelemetry Logback Appender

**Before (Master):**
```groovy
implementation 'com.github.loki4j:loki-logback-appender:1.4.2'
```

**After (otel_collector_log):**
```groovy
implementation 'io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.24.0-alpha'
```

**Why?**
- The OpenTelemetry Logback appender sends logs via OTLP protocol
- It integrates natively with Spring Boot 4's OpenTelemetry SDK
- Automatic trace context propagation (traceId, spanId)

---

### 2. Logback Configuration: Loki4j → OTEL Appender

**Before (Master):**
```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <http>
        <url>http://localhost:3100/loki/api/v1/push</url>
    </http>
    <format>
        <label>
            <pattern>service=${appName},level=%level</pattern>
        </label>
        <message>
            <pattern>{"timestamp":"%d","level":"%level",...}</pattern>
        </message>
    </format>
</appender>
```

**After (otel_collector_log):**
```xml
<appender name="OTEL" class="io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender">
    <captureExperimentalAttributes>true</captureExperimentalAttributes>
    <captureCodeAttributes>true</captureCodeAttributes>
    <captureMarkerAttribute>true</captureMarkerAttribute>
    <captureKeyValuePairAttributes>true</captureKeyValuePairAttributes>
    <captureMdcAttributes>traceId,spanId,parentSpanId</captureMdcAttributes>
</appender>
```

**Why?**
- OTEL appender uses the SDK's LoggerProvider for export
- Captures code location (file, line, function)
- Automatic MDC context capture

---

### 3. Application Configuration: Enable OTLP Logging

**Before (Master):**
```yaml
management:
  opentelemetry:
    logging:
      export:
        otlp:
          enabled: false  # ❌ Disabled
```

**After (otel_collector_log):**
```yaml
management:
  opentelemetry:
    # OTLP Logging export - correct path for Spring Boot 4
    logging:
      export:
        otlp:
          enabled: true
          endpoint: http://localhost:4318/v1/logs
```

**Key Points:**
- The property path is `management.opentelemetry.logging.export.otlp` (NOT `management.logging.export.otlp`)
- Uses HTTP endpoint (port 4318) with `/v1/logs` path
- This is the Spring Boot 4 specific configuration

---

### 4. Critical: OpenTelemetry Appender Installation

**New Component Added (did not exist in Master):**

```java
@Component
public class OpenTelemetryAppenderInstaller implements InitializingBean {
    
    private final OpenTelemetry openTelemetry;
    
    public OpenTelemetryAppenderInstaller(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        // Connect the Logback appender to the OpenTelemetry SDK
        OpenTelemetryAppender.install(openTelemetry);
    }
}
```

**Why is this required?**
- The OpenTelemetry Logback appender is a "bridge" to the SDK
- It needs to be explicitly connected to the SDK's LoggerProvider
- Without this, logs go into the appender but never get exported
- This is documented in the OpenTelemetry Java Instrumentation guide

---

### 5. OTel Collector: Add Logs Pipeline

**Before (Master):**
```yaml
service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [memory_limiter, batch]
      exporters: [otlp, debug]
```

**After (otel_collector_log):**
```yaml
exporters:
  # Tempo exporter - for traces
  otlp/tempo:
    endpoint: tempo:4317
    tls:
      insecure: true
  
  # Loki exporter - for logs via OTLP HTTP
  otlphttp/loki:
    endpoint: http://loki:3100/otlp
    tls:
      insecure: true

service:
  pipelines:
    # Traces pipeline
    traces:
      receivers: [otlp]
      processors: [memory_limiter, batch]
      exporters: [otlp/tempo, debug]
    
    # Logs pipeline (NEW!)
    logs:
      receivers: [otlp]
      processors: [memory_limiter, resource, batch]
      exporters: [otlphttp/loki, debug]
```

**Key Changes:**
- Added `otlphttp/loki` exporter pointing to Loki's OTLP endpoint
- Added `logs` pipeline with the same receiver (otlp) as traces
- Added `resource` processor to set `loki.resource.labels` for indexing

---

### 6. Loki Configuration: Enable OTLP Ingestion

**Before (Master):**
```yaml
# Basic Loki config with no OTLP support
auth_enabled: false
server:
  http_listen_port: 3100
```

**After (otel_collector_log):**
```yaml
auth_enabled: false

server:
  http_listen_port: 3100
  grpc_listen_port: 9096

# Enable OTLP ingestion for logs from OTel Collector
limits_config:
  allow_structured_metadata: true
  otlp_config:
    resource_attributes:
      attributes_config:
        - action: index_label
          attributes:
            - service.name
        - action: structured_metadata
          attributes:
            - service.instance.id
            - telemetry.sdk.language
```

**Why?**
- `allow_structured_metadata: true` enables rich OTLP attributes
- `otlp_config` tells Loki how to handle OTLP resource attributes
- `action: index_label` creates queryable labels (like `service_name`)
- Loki 3.0+ has native OTLP support at `/otlp/v1/logs`

---

### 7. Version Compatibility Fix

**Initial Attempt (Failed):**
```groovy
implementation 'io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.11.0-alpha'
```

**Error:**
```
java.lang.ClassNotFoundException: io.opentelemetry.api.incubator.common.ExtendedAttributeKey
```

**Fixed Version:**
```groovy
implementation 'io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.24.0-alpha'
```

**Why?**
- Spring Boot 4 uses OpenTelemetry SDK 1.55.0
- The appender version 2.11.0-alpha was incompatible
- Version 2.24.0-alpha is compatible with the SDK version

---

## Verification

After the fix, the following verification confirms logs are flowing:

### Check OTel Collector Receiving Logs
```bash
docker compose logs otel-collector | grep "otelcol.signal.*logs"
# Output: "otelcol.signal": "logs"
```

### Check Loki Has Labels
```bash
curl -s "http://localhost:3100/loki/api/v1/labels"
# Output: {"status":"success","data":["service_name"]}
```

### Query Logs in Loki
```bash
curl -s "http://localhost:3100/loki/api/v1/label/service_name/values"
# Output: {"status":"success","data":["cqrs-service","graphql-service","inventory-service","notification-service","orchestrator-service","order-service"]}
```

### Verify Trace Correlation
```bash
# Query a log entry and check for traceId/spanId
curl -s "http://localhost:3100/loki/api/v1/query_range" \
  --data-urlencode 'query={service_name="cqrs-service"} | json' \
  --data-urlencode "limit=1" | jq '.data.result[0].stream'

# Output includes:
# "traceId": "84c30fec33ff5ae4e27c64f07ed8be35"
# "spanId": "988d549548dc57b8"
```

---

## Summary of Files Changed

| File | Change |
|------|--------|
| `*/build.gradle` (6 files) | `loki4j` → `opentelemetry-logback-appender-1.0:2.24.0-alpha` |
| `*/application.yml` (6 files) | Fixed OTLP logging path, enabled export |
| `*/logback-spring.xml` (6 files) | Replaced `LOKI` appender with `OTEL` appender |
| `*/config/OpenTelemetryAppenderInstaller.java` (6 files) | **NEW**: SDK installation component |
| `config/otel-collector-config.yaml` | Added `logs` pipeline and `otlphttp/loki` exporter |
| `config/loki.yaml` | Added OTLP ingestion config with `limits_config` |
| `docker-compose.yml` | Updated Loki dependency on OTel Collector |

---

## Lessons Learned

1. **Spring Boot 4 OTLP Logging Path**: The correct path is `management.opentelemetry.logging.export.otlp`, NOT `management.logging.export.otlp`

2. **OpenTelemetry Logback Appender Requires Installation**: The appender must be explicitly connected to the SDK via `OpenTelemetryAppender.install(openTelemetry)`

3. **Version Compatibility**: The logback appender version must be compatible with Spring Boot 4's OpenTelemetry SDK version

4. **Unified Architecture**: Using OTel Collector for both traces AND logs provides:
   - Consistent configuration
   - Vendor neutrality
   - Native trace-log correlation
   - Centralized processing

5. **Loki OTLP Support**: Loki 3.0+ has native OTLP ingestion, but requires explicit configuration in `limits_config`

---

## References

- [Spring Boot 4 OpenTelemetry Guide](https://foojay.io/today/spring-boot-4-opentelemetry-explained/)
- [OpenTelemetry Logback Appender](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/logback/logback-appender-1.0/library)
- [Loki OTLP Ingestion](https://grafana.com/docs/loki/latest/send-data/otel/)
- [OTel Collector Loki Exporter](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/exporter/lokiexporter)
