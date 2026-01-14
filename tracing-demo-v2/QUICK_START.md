# Quick Start: Viewing Traces and Metrics in Grafana

## ✅ Fixed Issues

The test script has been fixed to handle:
- JSON parsing errors from multi-line stack traces in logs
- Proper trace ID extraction and validation
- Better error reporting for RabbitMQ trace propagation

## 🚀 Quick Steps to View Your Traces

### 1. Run the Test Script

```bash
cd tracing-demo-v2
./test_tracing_complete.sh
```

The script will:
- ✅ Verify all services are running
- ✅ Create a test order via GraphQL
- ✅ Extract trace IDs from logs
- ✅ Show you the trace ID to search in Grafana

### 2. Open Grafana

Open your browser and go to: **http://localhost:3000**

(No login required - anonymous access is enabled)

### 3. View Traces in Tempo

1. Click **Explore** (compass icon) in the left sidebar
2. Select **Tempo** from the dropdown at the top
3. In the query builder:
   - Select **"Search"** tab
   - Enter service name: `graphql-service` or `order-service`
   - Set time range: **"Last 15 minutes"**
   - Click **"Run Query"**

4. Click on any trace in the results to see:
   - Service interaction graph
   - Waterfall timeline of all operations
   - Span details (HTTP calls, database queries, etc.)

### 4. View Logs in Loki (Currently File-Based)

**Note:** Logs are currently written to files, not automatically shipped to Loki. To view logs:

#### Option A: View log files directly
```bash
# View order service logs
tail -f order-service/logs/order-service.json.log | jq

# View GraphQL service logs
tail -f graphql-service/logs/graphql-service.json.log | jq

# View logs with trace ID
cat order-service/logs/order-service.json.log | jq 'select(.traceId == "YOUR_TRACE_ID")'
```

#### Option B: Set up Promtail (See GRAFANA_GUIDE.md for details)

### 5. View Metrics

Service metrics are exposed at:
- http://localhost:8080/actuator/prometheus (GraphQL Service)
- http://localhost:8081/actuator/prometheus (Order Service)
- http://localhost:8082/actuator/prometheus (Inventory Service)
- http://localhost:8083/actuator/prometheus (Notification Service)

You can view these in your browser or curl:
```bash
curl http://localhost:8081/actuator/prometheus | grep http_server_requests
```

## 🔍 Example Workflow

1. **Create an order**:
   ```bash
   ./test_tracing_complete.sh
   ```
   
   Output will show trace ID like:
   ```
   Search for trace: 8669f1b3d058cf4cda28757c7d647140
   ```

2. **Open Grafana**: http://localhost:3000

3. **Navigate to Explore → Tempo**

4. **Search by service name** or paste the trace ID

5. **Explore the trace**:
   - Click on the trace to see the waterfall view
   - Expand spans to see details
   - Look for duration, status codes, and errors

6. **Correlate with logs**:
   ```bash
   cat */logs/*.json.log | jq 'select(.traceId == "8669f1b3d058cf4cda28757c7d647140")'
   ```

## 📊 What You'll See

### In Tempo (Traces):

1. **GraphQL Service Span**
   - Root span for the GraphQL mutation
   - HTTP call to order-service

2. **Order Service Spans**
   - Receiving the REST request
   - Saving order to H2 database
   - Publishing message to RabbitMQ

3. **Inventory Service Spans**
   - (Note: Currently no trace propagation via RabbitMQ)
   - Receiving message from RabbitMQ
   - Updating inventory

4. **Notification Service Spans**
   - (Note: Currently no trace propagation via RabbitMQ)
   - Receiving message from RabbitMQ
   - Sending email notification

### Expected Trace Flow:

```
graphql-service (HTTP POST)
    └── order-service (HTTP POST)
            ├── H2 Database Save
            └── RabbitMQ Publish
                    ├── inventory-service (async) ⚠️ No trace propagation
                    └── notification-service (async) ⚠️ No trace propagation
```

## ⚠️ Known Issues

### 1. Different Trace IDs Between Services

**Symptom:** GraphQL and Order services show different trace IDs

**Cause:** Trace context propagation may not be working correctly between services

**Solution:** Verify that:
- OpenTelemetry Java agent is configured correctly
- HTTP headers (traceparent) are being propagated
- Both services use the same trace format (W3C or B3)

### 2. No Trace IDs in RabbitMQ Consumers

**Symptom:** Inventory and Notification services have no trace IDs

**Cause:** Trace context is not propagated through RabbitMQ messages

**Solution:** Add trace context propagation to RabbitMQ:
```java
// In OrderPublisher.java, add headers:
message.getMessageProperties().setHeader("traceparent", currentTraceContext);
```

See `docs/tracing-demo/rabbitmq_trace_propagation.md` for details

### 3. OTLP Export Errors

**Symptom:** Logs show "Failed to export spans" errors

**Possible causes:**
- Tempo collector may be restarting
- Network connectivity issues
- These are usually transient and can be ignored if traces appear in Grafana

## 🎯 TraceQL Examples

Once in Tempo Explore, try these queries:

### Find traces by service:
```traceql
{ service.name="order-service" }
```

### Find slow traces (>1 second):
```traceql
{ duration > 1s }
```

### Find traces with specific HTTP method:
```traceql
{ span.http.method="POST" }
```

### Complex query:
```traceql
{ 
  service.name="order-service" 
  && span.http.method="POST" 
  && duration > 500ms 
}
```

## 📚 Additional Resources

- **Full Grafana Guide**: See `GRAFANA_GUIDE.md` for detailed instructions
- **Implementation Details**: See `docs/tracing-demo/tracing_implementation.md`
- **RabbitMQ Trace Propagation**: Coming soon

## 🛠️ Troubleshooting

### Services not running?
```bash
./run_all.sh
```

### Grafana not accessible?
```bash
docker compose ps
docker compose up -d grafana
```

### No traces in Tempo?
Check service logs:
```bash
tail -f logs/*.log | grep -i "trace\|span"
```

### Want to see new traces?
Create more orders:
```bash
# Via test script
./test_tracing_complete.sh

# Or via GraphQL UI
# Open http://localhost:8080/graphiql
# Run: mutation { createOrder(productId: "laptop", quantity: 3) { orderId status } }
```

## 🎉 Success Criteria

You'll know it's working when:
- ✅ Test script shows trace IDs
- ✅ Grafana shows traces in Tempo
- ✅ Traces show multiple spans from graphql-service and order-service
- ✅ You can see the waterfall view of operations
- ✅ Logs contain matching trace IDs

---

**Happy Tracing! 🚀**
