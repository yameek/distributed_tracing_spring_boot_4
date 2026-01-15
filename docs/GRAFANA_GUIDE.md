# Grafana Observability Guide

This guide explains how to view traces, logs, and metrics in Grafana for your distributed tracing demo.

## Overview

Your setup includes:
- **Grafana** (http://localhost:3000) - Visualization and dashboarding
- **Tempo** (http://localhost:4318) - Distributed tracing backend
- **Loki** (http://localhost:3100) - Log aggregation
- **RabbitMQ Management** (http://localhost:15672) - Message broker UI

## 1. Accessing Grafana

1. Open your browser and navigate to: **http://localhost:3000**
2. You should be automatically logged in (anonymous auth is enabled)

## 2. Viewing Traces in Tempo

### Method 1: Search by Trace ID

1. In Grafana, click **Explore** (compass icon) in the left sidebar
2. Select **Tempo** from the data source dropdown at the top
3. You have several search options:

#### Option A: Search by Trace ID
- Select "Search" → "TraceQL" (or "Trace ID" if available)
- Paste a trace ID from your test output
- Click **Run Query**

Example trace ID from the test output:
```
8669f1b3d058cf4cda28757c7d647140
```

#### Option B: Search by Service Name
- Select "Search" mode
- Enter service name: `graphql-service`, `order-service`, `inventory-service`, or `notification-service`
- Set a time range (e.g., "Last 15 minutes")
- Click **Run Query**

#### Option C: Use TraceQL (Advanced)
```traceql
{ service.name="graphql-service" }
```

Or filter by span attributes:
```traceql
{ span.http.method="POST" && service.name="order-service" }
```

### Understanding the Trace View

Once you open a trace, you'll see:

1. **Service Graph** - Visual representation of service interactions
2. **Spans Timeline** - Waterfall view showing:
   - Each operation (span) as a horizontal bar
   - Duration of each operation
   - Parent-child relationships between spans
   - Color-coded by service

3. **Span Details** - Click any span to see:
   - Attributes (HTTP method, status code, etc.)
   - Events (log messages within the span)
   - Tags (custom metadata)
   - Trace ID and Span ID

### What to Look For:

- **GraphQL Service Spans**: The root span showing the GraphQL mutation
- **HTTP Calls**: REST calls from graphql-service to order-service
- **Database Operations**: H2 database interactions in order-service
- **Message Publishing**: RabbitMQ publish operations

## 3. Viewing Logs in Loki

### Search Logs by Trace ID

1. In Grafana, click **Explore**
2. Select **Loki** from the data source dropdown
3. Enter a LogQL query:

```logql
{service_name=~".+"} | json | traceId="8669f1b3d058cf4cda28757c7d647140"
```

Replace the trace ID with one from your test output.

### Other Useful Log Queries

#### All logs from a specific service:
```logql
{service_name="order-service"}
```

#### Filter by log level:
```logql
{service_name="order-service"} | json | level="ERROR"
```

#### Search for specific text:
```logql
{service_name=~".+"} | json | message =~ "(?i)order.*created"
```

#### Logs with trace context (has traceId):
```logql
{service_name=~".+"} | json | traceId != ""
```

#### View all services for a request:
```logql
{service_name=~".+"} | json | traceId="YOUR_TRACE_ID_HERE" | line_format "{{.timestamp}} [{{.service_name}}] {{.message}}"
```

### Log Correlation

The power of distributed tracing is correlating logs across services:

1. Find an interesting trace in Tempo
2. Copy the Trace ID
3. Search for that trace ID in Loki
4. You'll see all log messages from all services for that single request!

## 4. Viewing Metrics

### Application Metrics

The Spring Boot services expose metrics to Prometheus format at:
- http://localhost:8080/actuator/prometheus (GraphQL Service)
- http://localhost:8081/actuator/prometheus (Order Service)
- http://localhost:8082/actuator/prometheus (Inventory Service)
- http://localhost:8083/actuator/prometheus (Notification Service)

However, services are also sending metrics via OTLP to the collector (though you may see connection errors in logs).

### Common Metrics to Monitor:

1. **HTTP Request Rate**:
   - `http_server_requests_seconds_count`
   
2. **HTTP Request Duration**:
   - `http_server_requests_seconds_sum`
   - `http_server_requests_seconds_max`

3. **JVM Metrics**:
   - `jvm_memory_used_bytes`
   - `jvm_threads_live_threads`
   - `jvm_gc_pause_seconds_count`

4. **Database Metrics**:
   - `jdbc_connections_active`
   - `hikaricp_connections_active`

## 5. Creating Dashboards

### Create a Dashboard for Your Services

1. Click **Dashboards** → **New Dashboard**
2. Click **Add visualization**
3. Select your data source (Tempo, Loki, or Prometheus if configured)
4. Build queries for the metrics you want

### Example Dashboard Panels:

#### Panel 1: Request Rate by Service
- Data source: Loki
- Query:
```logql
sum by (service_name) (rate({service_name=~".+"} | json | level="INFO" [1m]))
```

#### Panel 2: Trace Duration Distribution
- Data source: Tempo
- Use TraceQL to query spans and their durations

#### Panel 3: Error Logs
- Data source: Loki
- Query:
```logql
{service_name=~".+"} | json | level="ERROR"
```

## 6. Service Graph (Observability Map)

Tempo can generate a service graph showing how services communicate:

1. Go to **Explore** → **Tempo**
2. Click on **Service Graph** (if available in your Grafana version)
3. You'll see a visual representation of:
   - All services (nodes)
   - Communication paths (edges)
   - Request rates
   - Error rates
   - Latencies

## 7. Troubleshooting

### No traces appearing?

1. Verify Tempo is running:
   ```bash
   docker compose ps tempo
   ```

2. Check if services can reach Tempo:
   ```bash
   curl http://localhost:4318/v1/traces
   ```

3. Check service logs for OTLP export errors:
   ```bash
   tail -f logs/order-service.log | grep -i "exporter"
   ```

### Logs not showing in Loki?

Logs are written to files, not sent to Loki automatically. To send logs to Loki, you would need:

1. **Promtail** - Log shipping agent
2. Or **Fluentd/Fluent Bit**
3. Or configure logback to send directly to Loki

Currently, your logs are in:
- `graphql-service/logs/graphql-service.json.log`
- `order-service/logs/order-service.json.log`
- `inventory-service/logs/inventory-service.json.log`
- `notification-service/logs/notification-service.json.log`

### Trace IDs are different between services?

This suggests trace propagation isn't working correctly. The trace context should be propagated via HTTP headers (`traceparent` or `X-B3-TraceId`).

Check:
1. OpenTelemetry auto-instrumentation is enabled
2. `RestTemplate` or HTTP client is instrumented
3. Headers are being propagated

## 8. Advanced: TraceQL Queries

TraceQL is Tempo's query language. Here are some useful queries:

### Find slow traces (>1 second):
```traceql
{ duration > 1s }
```

### Find traces with errors:
```traceql
{ status = error }
```

### Find traces for specific endpoint:
```traceql
{ span.http.target = "/api/orders" }
```

### Complex query:
```traceql
{ 
  service.name = "order-service" 
  && span.http.method = "POST" 
  && status = error 
}
```

## 9. Real-World Usage Pattern

Here's a typical debugging workflow:

1. **User reports a problem**: "Order creation is slow"

2. **Find recent traces**:
   - Go to Tempo
   - Search for `order-service`
   - Look at trace durations
   - Sort by slowest

3. **Analyze the slow trace**:
   - Click on the slowest trace
   - Look at the waterfall view
   - Identify which span took the longest

4. **Correlate with logs**:
   - Copy the trace ID
   - Go to Loki
   - Search: `{service_name=~".+"} | json | traceId="TRACE_ID"`
   - Look for errors or warnings

5. **Check metrics**:
   - Look at CPU, memory, request rate
   - Identify if it's a resource issue

6. **Fix the issue** and verify with new traces

## 10. Next Steps

1. **Add Prometheus** for better metrics visualization:
   ```yaml
   prometheus:
     image: prom/prometheus:latest
     volumes:
       - ./config/prometheus.yml:/etc/prometheus/prometheus.yml
     ports:
       - "9090:9090"
   ```

2. **Add Promtail** to ship logs to Loki:
   ```yaml
   promtail:
     image: grafana/promtail:latest
     volumes:
       - ./logs:/var/log/app
       - ./config/promtail.yml:/etc/promtail/config.yml
   ```

3. **Configure exemplars** to link metrics → traces

4. **Set up alerts** in Grafana for high error rates or slow traces

5. **Create pre-built dashboards** for your team

## Quick Reference

| Tool | URL | Purpose |
|------|-----|---------|
| Grafana | http://localhost:3000 | Visualization |
| Tempo | http://localhost:3200 | Trace storage |
| Loki | http://localhost:3100 | Log aggregation |
| RabbitMQ | http://localhost:15672 | Message broker (user: guest, pass: guest) |
| GraphQL | http://localhost:8080/graphiql | API endpoint |

## Example Test Flow

1. Run the test script:
   ```bash
   ./test_tracing_complete.sh
   ```

2. Copy the trace ID from the output

3. Open Grafana → Explore → Tempo

4. Paste the trace ID and search

5. Explore the spans and service interactions

6. Go to Loki and search for logs with that trace ID

7. Correlate the logs with the trace timeline

---

**Happy Observing! 🔍**
