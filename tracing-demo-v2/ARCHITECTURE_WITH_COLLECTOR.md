# Architecture with OpenTelemetry Collector

## Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              APPLICATION LAYER                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐          │
│  │ GraphQL Service │   │  Order Service  │   │Inventory Service│          │
│  │   Port 8080     │   │   Port 8081     │   │   Port 8082     │          │
│  └────────┬────────┘   └────────┬────────┘   └────────┬────────┘          │
│           │                     │                      │                    │
│           │  ┌─────────────────────────────┐          │                    │
│           │  │  Notification Service       │          │                    │
│           │  │      Port 8083              │          │                    │
│           │  └──────────────┬──────────────┘          │                    │
│           │                 │                          │                    │
│           │                 │  OTLP gRPC (traces)     │                    │
│           └─────────────────┼──────────────────────────┘                    │
│                             │                                               │
└─────────────────────────────┼───────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           COLLECTION LAYER                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                   ┌────────────────────────────────┐                        │
│                   │  OpenTelemetry Collector       │                        │
│                   │                                │                        │
│                   │  Receivers:                    │                        │
│                   │    • OTLP gRPC (4317)         │                        │
│                   │    • OTLP HTTP (4318)         │                        │
│                   │                                │                        │
│                   │  Processors:                   │                        │
│                   │    • Memory Limiter            │                        │
│                   │    • Batch Processor           │                        │
│                   │    • (Sampling, Filtering...)  │                        │
│                   │                                │                        │
│                   │  Exporters:                    │                        │
│                   │    • OTLP (to Tempo)          │                        │
│                   │    • Logging (debug)           │                        │
│                   │    • (Jaeger, Zipkin...)       │                        │
│                   │                                │                        │
│                   │  Metrics: Port 8888            │                        │
│                   └────────────┬───────────────────┘                        │
│                                │                                            │
└────────────────────────────────┼────────────────────────────────────────────┘
                                 │
                                 │ OTLP gRPC
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            BACKEND LAYER                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────┐      ┌──────────────┐      ┌──────────────┐            │
│  │    Tempo     │      │     Loki     │      │   Grafana    │            │
│  │              │      │              │      │              │            │
│  │ Trace Store  │      │  Log Store   │      │ Visualization│            │
│  │ Port 3200    │      │  Port 3100   │      │  Port 3000   │            │
│  └──────────────┘      └──────────────┘      └──────────────┘            │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Message Flow

```
┌─────────────────┐
│ GraphQL Service │
└────────┬────────┘
         │ HTTP POST /graphql
         ▼
┌─────────────────┐
│  Order Service  │
└────────┬────────┘
         │ RabbitMQ publish
         ├──────────────────────┐
         │                      │
         ▼                      ▼
┌─────────────────┐    ┌─────────────────┐
│Inventory Service│    │Notification Svc │
└─────────────────┘    └─────────────────┘

All services send traces to Collector (port 4317)
```

## Benefits of This Architecture

### 1. Vendor Independence

**Before (Direct to Backend):**
```
Services → Tempo
```
To switch to Jaeger: Modify all 4 service configs + rebuild + redeploy

**After (With Collector):**
```
Services → Collector → Tempo
```
To switch to Jaeger: Edit one YAML file + restart collector container

### 2. Multiple Backends

```
                    ┌─→ Tempo (production)
Services → Collector ┼─→ Jaeger (debugging)
                    └─→ DataDog (monitoring)
```

### 3. Processing Pipeline

```
Services → Collector → [Sample 10%] → [Remove PII] → [Add metadata] → Backend
```

### 4. Resilience

```
Services → Collector (buffers) → Backend (down)
           ↓
        Retries with backoff
```

## Configuration Files

### Service Configuration (`application.yml`)
```yaml
management:
  opentelemetry:
    tracing:
      export:
        otlp:
          endpoint: http://localhost:4317  # Points to Collector
```

### Collector Configuration (`config/otel-collector-config.yaml`)
```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317

exporters:
  otlp:
    endpoint: tempo:4317  # Points to Backend
```

### Docker Compose
```yaml
services:
  otel-collector:
    ports:
      - "4317:4317"  # Services connect here
    depends_on:
      - tempo
  
  tempo:
    # No longer exposed to host
```

## Switching Backends - Examples

### Switch to Jaeger

1. Add Jaeger to `docker-compose.yml`
2. Update collector config:
   ```yaml
   exporters:
     otlp/jaeger:
       endpoint: jaeger:4317
   ```
3. Restart collector: `docker-compose restart otel-collector`

**No service changes needed!** ✅

### Add Multiple Backends

```yaml
exporters:
  otlp/tempo:
    endpoint: tempo:4317
  otlp/jaeger:
    endpoint: jaeger:4317

service:
  pipelines:
    traces:
      exporters: [otlp/tempo, otlp/jaeger]  # Both!
```

### Add Sampling

```yaml
processors:
  probabilistic_sampler:
    sampling_percentage: 10  # Keep 10%

service:
  pipelines:
    traces:
      processors: [probabilistic_sampler, batch]
```

## Monitoring the Collector

### Metrics Endpoint
```bash
curl http://localhost:8888/metrics
```

Key metrics:
- `otelcol_receiver_accepted_spans` - Spans received from services
- `otelcol_exporter_sent_spans` - Spans sent to backend
- `otelcol_processor_batch_batch_send_size` - Batch sizes

### Health Check
```bash
curl http://localhost:8888/metrics | grep otelcol_receiver_accepted_spans
```

### Logs
```bash
docker-compose logs -f otel-collector
```

## Troubleshooting Flow

```
1. Check services are sending traces
   ↓
   curl http://localhost:8888/metrics | grep receiver_accepted_spans
   
2. Check collector is processing
   ↓
   docker-compose logs otel-collector
   
3. Check collector is exporting
   ↓
   curl http://localhost:8888/metrics | grep exporter_sent_spans
   
4. Check backend is receiving
   ↓
   Open Grafana → Tempo → Search
```

## Performance Considerations

### Batching
```yaml
processors:
  batch:
    timeout: 1s           # Send every 1 second
    send_batch_size: 1024 # Or when 1024 spans collected
```

### Memory Limits
```yaml
processors:
  memory_limiter:
    limit_mib: 512        # Max memory usage
    spike_limit_mib: 128  # Spike allowance
```

### Concurrency
```yaml
exporters:
  otlp:
    sending_queue:
      num_consumers: 10   # Parallel export workers
```

## Security Considerations

### TLS
```yaml
exporters:
  otlp:
    endpoint: tempo:4317
    tls:
      insecure: false
      cert_file: /path/to/cert.pem
      key_file: /path/to/key.pem
```

### Authentication
```yaml
exporters:
  otlp:
    endpoint: tempo:4317
    headers:
      authorization: Bearer ${API_TOKEN}
```

## Comparison: With vs Without Collector

| Feature | Without Collector | With Collector |
|---------|------------------|----------------|
| **Backend Switch** | Modify 4 services | Edit 1 YAML file |
| **Multiple Backends** | Configure in each service | Configure once |
| **Sampling** | Basic probability only | Advanced strategies |
| **Filtering** | Not possible | Full control |
| **Buffering** | None | Built-in |
| **Retry Logic** | Service handles | Collector handles |
| **Debugging** | Check 4 services | Check 1 collector |
| **Complexity** | Lower | Slightly higher |
| **Flexibility** | Lower | Much higher |

## Recommended for

✅ **Use Collector when:**
- You might switch backends
- You need multiple backends
- You want advanced sampling/filtering
- You have many services
- You need centralized control

❌ **Skip Collector when:**
- Single service, single backend
- Very simple use case
- Minimal infrastructure preferred
- No backend switching planned

## Next Steps

1. ✅ Collector is now set up
2. Run `./test_collector.sh` to verify
3. Try switching to Jaeger (see `COLLECTOR_GUIDE.md`)
4. Experiment with sampling or filtering
5. Monitor collector metrics at `http://localhost:8888/metrics`

## References

- [OpenTelemetry Collector Docs](https://opentelemetry.io/docs/collector/)
- [Collector Configuration](https://opentelemetry.io/docs/collector/configuration/)
- [Available Processors](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/processor)
- [Available Exporters](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/exporter)
