# OpenTelemetry Collector Guide

## Why Use a Collector?

The OpenTelemetry Collector acts as an intermediary between your services and the tracing backend, providing several key benefits:

### 1. **Vendor Independence** 🎯
- **No code changes needed**: Switch between Tempo, Jaeger, Zipkin, or any other backend without modifying service code
- **Backend agnostic**: Your services only know about the collector, not the specific backend
- **Future-proof**: Easily migrate to new tracing solutions as they emerge

### 2. **Centralized Configuration**
- All export logic in one place (`config/otel-collector-config.yaml`)
- Consistent configuration across all services
- Easier to maintain and update

### 3. **Multiple Backends Simultaneously**
- Send traces to multiple destinations (e.g., Tempo for production + Jaeger for debugging)
- A/B test different backends
- Gradual migration between systems

### 4. **Processing Capabilities**
- **Sampling**: Implement sophisticated sampling strategies
- **Filtering**: Remove sensitive data or unnecessary spans
- **Enrichment**: Add metadata to all traces
- **Batching**: Optimize network usage
- **Retry logic**: Handle backend outages gracefully

### 5. **Performance & Reliability**
- Buffer traces during backend downtime
- Rate limiting and backpressure handling
- Memory management to prevent OOM

## Current Architecture

```
┌─────────────────┐
│ graphql-service │──┐
└─────────────────┘  │
                     │
┌─────────────────┐  │    ┌──────────────────┐    ┌───────┐
│  order-service  │──┼───▶│ OTel Collector   │───▶│ Tempo │
└─────────────────┘  │    │ (Port 4317/4318) │    └───────┘
                     │    └──────────────────┘
┌─────────────────┐  │         │
│inventory-service│──┤         │ (can forward to)
└─────────────────┘  │         ▼
                     │    ┌──────────┐
┌─────────────────┐  │    │  Jaeger  │
│notification-svc │──┘    │  Zipkin  │
└─────────────────┘       │ DataDog  │
                          │ New Relic│
                          └──────────┘
```

## How to Switch Backends

### Option 1: Switch to Jaeger

1. Add Jaeger to `docker-compose.yml`:
```yaml
  jaeger:
    image: jaegertracing/all-in-one:latest
    ports:
      - "16686:16686"  # Jaeger UI
      - "14250:14250"  # gRPC
    environment:
      - COLLECTOR_OTLP_ENABLED=true
```

2. Update `config/otel-collector-config.yaml`:
```yaml
exporters:
  otlp/jaeger:
    endpoint: jaeger:4317
    tls:
      insecure: true

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [memory_limiter, batch]
      exporters: [otlp/jaeger, logging]  # Changed from 'otlp' to 'otlp/jaeger'
```

3. Restart: `docker-compose restart otel-collector`
4. Access Jaeger UI at `http://localhost:16686`

**No service code changes required!** ✅

### Option 2: Switch to Zipkin

1. Add Zipkin to `docker-compose.yml`:
```yaml
  zipkin:
    image: openzipkin/zipkin:latest
    ports:
      - "9411:9411"
```

2. Update `config/otel-collector-config.yaml`:
```yaml
exporters:
  zipkin:
    endpoint: http://zipkin:9411/api/v2/spans

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [memory_limiter, batch]
      exporters: [zipkin, logging]  # Changed to zipkin
```

3. Restart: `docker-compose restart otel-collector`
4. Access Zipkin UI at `http://localhost:9411`

**No service code changes required!** ✅

### Option 3: Multiple Backends (Tempo + Jaeger)

Update `config/otel-collector-config.yaml`:
```yaml
exporters:
  otlp/tempo:
    endpoint: tempo:4317
    tls:
      insecure: true
  
  otlp/jaeger:
    endpoint: jaeger:4317
    tls:
      insecure: true

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [memory_limiter, batch]
      exporters: [otlp/tempo, otlp/jaeger, logging]  # Send to both!
```

**No service code changes required!** ✅

### Option 4: Cloud Providers (DataDog, New Relic, etc.)

For DataDog:
```yaml
exporters:
  datadog:
    api:
      key: ${DD_API_KEY}
      site: datadoghq.com

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [memory_limiter, batch]
      exporters: [datadog, logging]
```

For New Relic:
```yaml
exporters:
  otlphttp:
    endpoint: https://otlp.nr-data.net
    headers:
      api-key: ${NEW_RELIC_LICENSE_KEY}

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [memory_limiter, batch]
      exporters: [otlphttp, logging]
```

**No service code changes required!** ✅

## Advanced Processing Examples

### Sampling (Reduce trace volume)

```yaml
processors:
  probabilistic_sampler:
    sampling_percentage: 10  # Keep only 10% of traces

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [probabilistic_sampler, memory_limiter, batch]
      exporters: [otlp]
```

### Filtering (Remove sensitive data)

```yaml
processors:
  attributes:
    actions:
      - key: password
        action: delete
      - key: credit_card
        action: delete

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [attributes, memory_limiter, batch]
      exporters: [otlp]
```

### Enrichment (Add metadata)

```yaml
processors:
  resource:
    attributes:
      - key: environment
        value: production
        action: insert
      - key: region
        value: us-east-1
        action: insert

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [resource, memory_limiter, batch]
      exporters: [otlp]
```

## Testing the Collector

### 1. Check Collector Health
```bash
curl http://localhost:8888/metrics
```

### 2. View Collector Logs
```bash
docker-compose logs -f otel-collector
```

### 3. Verify Traces Flow
```bash
# Send a test request
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ orders { id status } }"}'

# Check collector logs for received spans
docker-compose logs otel-collector | grep -i "span"
```

### 4. Monitor Collector Metrics
The collector exposes Prometheus metrics at `http://localhost:8888/metrics`:
- `otelcol_receiver_accepted_spans` - Spans received from services
- `otelcol_exporter_sent_spans` - Spans sent to backend
- `otelcol_processor_batch_batch_send_size` - Batch sizes

## Key Configuration Files

1. **`config/otel-collector-config.yaml`** - Collector configuration
   - Receivers: How collector accepts data
   - Processors: How data is transformed
   - Exporters: Where data is sent
   - Pipelines: Connects receivers → processors → exporters

2. **`docker-compose.yml`** - Collector service definition
   - Exposes port 4317 (gRPC) for services
   - Exposes port 8888 for metrics

3. **Service `application.yml` files** - Point to collector
   - `endpoint: http://localhost:4317`
   - Services don't know about the backend

## Troubleshooting

### Traces not appearing in backend?

1. Check collector is running:
   ```bash
   docker-compose ps otel-collector
   ```

2. Check collector logs:
   ```bash
   docker-compose logs otel-collector
   ```

3. Verify services can reach collector:
   ```bash
   curl http://localhost:4317
   # Should return HTTP/2 response (gRPC endpoint)
   ```

4. Check backend is reachable from collector:
   ```bash
   docker-compose exec otel-collector ping tempo
   ```

### High memory usage?

Adjust memory limiter in `config/otel-collector-config.yaml`:
```yaml
processors:
  memory_limiter:
    check_interval: 1s
    limit_mib: 256  # Reduce from 512
    spike_limit_mib: 64  # Reduce from 128
```

## Benefits Summary

| Aspect | Without Collector | With Collector |
|--------|------------------|----------------|
| **Backend Change** | Modify all service configs + rebuild | Edit one YAML file |
| **Multiple Backends** | Configure in each service | Configure once in collector |
| **Sampling** | Limited to probability | Advanced strategies available |
| **Filtering** | Not possible | Full control |
| **Debugging** | Check each service | Centralized logging |
| **Downtime Handling** | Services fail to export | Collector buffers |

## Next Steps

1. ✅ Collector is now set up with Tempo
2. Try switching to Jaeger (see Option 1 above)
3. Experiment with sampling or filtering
4. Monitor collector metrics
5. Consider adding a second backend for redundancy

## References

- [OpenTelemetry Collector Documentation](https://opentelemetry.io/docs/collector/)
- [Collector Configuration Reference](https://opentelemetry.io/docs/collector/configuration/)
- [Available Processors](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/processor)
- [Available Exporters](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/exporter)
