# How to View Traces and Metrics in Grafana - Complete Guide

## ✅ All Issues FIXED!

All three issues have been successfully resolved:

1. ✅ **Trace Context Propagation** - Same trace ID across ALL services
2. ✅ **RabbitMQ Trace Propagation** - Async services now have trace context
3. ✅ **OTLP Export Errors** - Zero errors, traces successfully exported to Tempo

**Test Results:**
```
✓ SUCCESS! Same trace ID propagated across ALL services!
  Trace ID: 0a763aebdacc28dc27eb80dcdb92e8aa

✓ Async services:
  Inventory:    0a763aebdacc28dc27eb80dcdb92e8aa
  Notification: 0a763aebdacc28dc27eb80dcdb92e8aa
```

---

## 🎯 Accessing Traces in Grafana

### Step 1: Open Grafana

Navigate to: **http://localhost:3000**

- No login required (anonymous auth enabled)
- You'll be automatically logged in as Admin

### Step 2: Go to Explore

1. Click the **Explore** icon (compass 🧭) in the left sidebar
2. OR click the menu → Explore

### Step 3: Select Tempo Data Source

1. At the top of the page, find the **data source dropdown**
2. Select **"Tempo"**

### Step 4: Search for Traces

You have multiple search options:

#### Option A: Search by Trace ID (Most Specific)

1. Run the test script to get a trace ID:
   ```bash
   cd tracing-demo-v2
   ./test_tracing_complete.sh
   ```
   
2. Copy the trace ID from the output (e.g., `0a763aebdacc28dc27eb80dcdb92e8aa`)

3. In Grafana Explore > Tempo:
   - Click the **"Search"** tab
   - Find the **"Trace ID"** field (or use TraceQL mode)
   - Paste your trace ID
   - Click **"Run Query"**

#### Option B: Search by TraceQL (Recommended)

Use TraceQL queries to find traces:

**Find all traces:**
```traceql
{}
```

**Find by service:**
```traceql
{ resource.service.name="order-service" }
```

**Find slow traces:**
```traceql
{ duration > 100ms }
```

**Find GraphQL mutations:**
```traceql
{ name =~ ".*graphql.*" }
```

**Complex query:**
```traceql
{
  resource.service.name="order-service" && 
  span.http.method="POST"
}
```

#### Option C: Use the Search Builder (Easiest for Beginners)

1. In the Tempo query interface, click **"Search"**
2. Select filters:
   - **Service Name**: `order-service`, `graphql-service`, etc.
   - **Span Name**: `http post`, `rabbitmq send`, etc.
   - **Duration**: min/max duration
   - **Status**: ok, error, unset
3. Set time range (e.g., "Last 15 minutes")
4. Click **"Run Query"**

### Step 5: Explore the Trace

Once you click on a trace, you'll see:

#### Trace Waterfall View

```
┌──────────────────────────────────────────────────────────┐
│  graphql-service                                         │
│  ├─ http POST /graphql ───────────────── 590ms          │
│  │  ├─ graphql mutation ───────────── 480ms             │
│  │  │  ├─ graphql field createOrder ── 375ms            │
│  │  │  │  └─ http POST (to order-service) ─ 345ms       │
│  │                                                       │
│  order-service                                           │
│  └─ http POST /orders ──────────────── 322ms            │
│     └─ rabbitmq send ───── 24ms                          │
│        ├─ inventory-service                              │
│        │  └─ rabbitmq receive ─── 161ms                  │
│        │                                                 │
│        └─ notification-service                           │
│           └─ rabbitmq receive ─── 217ms                  │
└──────────────────────────────────────────────────────────┘
```

#### What You Can See:

1. **Timeline**: Visual representation of when each operation occurred
2. **Duration**: How long each span took
3. **Parent-Child Relationships**: Which operations triggered which
4. **Service Colors**: Each service has a different color
5. **Span Details**: Click any span to see:
   - HTTP method, URL, status code
   - RabbitMQ queue names, routing keys
   - GraphQL operation type and field name
   - Error messages (if any)
   - Timing information

---

## 📊 Viewing Metrics

### Service Metrics (Prometheus Format)

Access metrics directly from each service:

- **GraphQL Service**: http://localhost:8080/actuator/prometheus
- **Order Service**: http://localhost:8081/actuator/prometheus
- **Inventory Service**: http://localhost:8082/actuator/prometheus
- **Notification Service**: http://localhost:8083/actuator/prometheus

### Available Metrics

```bash
# View HTTP request metrics
curl http://localhost:8081/actuator/prometheus | grep http_server_requests

# View JVM memory
curl http://localhost:8081/actuator/prometheus | grep jvm_memory

# View RabbitMQ metrics
curl http://localhost:8081/actuator/prometheus | grep rabbitmq
```

### Key Metrics to Monitor:

1. **Request Rate**: `http_server_requests_seconds_count`
2. **Request Duration**: `http_server_requests_seconds_sum`
3. **Memory Usage**: `jvm_memory_used_bytes`
4. **Thread Count**: `jvm_threads_live_threads`
5. **GC Pauses**: `jvm_gc_pause_seconds_count`
6. **Database Connections**: `hikaricp_connections_active`

---

## 📝 Viewing Logs with Trace Correlation

### Current Setup (File-Based)

Logs are written to JSON files with trace IDs:

```bash
# View all logs for a specific trace ID
cat */logs/*.json.log | jq 'select(.traceId == "0a763aebdacc28dc27eb80dcdb92e8aa")'

# View logs from a specific service
tail -f order-service/logs/order-service.json.log | jq

# Filter by log level
cat */logs/*.json.log | jq 'select(.level == "ERROR")'

# See the request flow across all services
cat */logs/*.json.log | jq 'select(.traceId == "YOUR_TRACE_ID") | {time: ."@timestamp", service, logger, message, spanId}'
```

### Example: Correlate Logs with Traces

1. Find a trace in Grafana (trace ID: `0a763aebdacc28dc27eb80dcdb92e8aa`)
2. Copy the trace ID
3. View logs for that trace:

```bash
cat */logs/*.json.log | jq 'select(.traceId == "0a763aebdacc28dc27eb80dcdb92e8aa") | {
  time: ."@timestamp",
  service,
  message,
  spanId
}' | less
```

You'll see the complete story of the request across all services!

---

## 🎨 Creating Dashboards in Grafana

### Create a Custom Dashboard

1. Click **Dashboards** → **New Dashboard**
2. Click **Add visualization**
3. Select **Tempo** as the data source
4. Choose visualization type (Time series, Graph, Table, etc.)

### Example Panel Configurations

#### Panel 1: Service Request Rate

- **Data Source**: Tempo
- **Query Type**: TraceQL Metrics
- **Query**:
```traceql
{} | rate() by(resource.service.name)
```

#### Panel 2: Slowest Traces

- **Data Source**: Tempo
- **Query**:
```traceql
{ duration > 200ms }
```
- **Visualization**: Table
- **Sort by**: Duration (descending)

#### Panel 3: Error Rate by Service

- **Data Source**: Tempo  
- **Query**:
```traceql
{ status=error } | rate() by(resource.service.name)
```

---

## 🔍 Example Workflows

### Workflow 1: Find Slow Requests

1. **Go to Explore** → Tempo
2. **Search query**:
   ```traceql
   { duration > 500ms }
   ```
3. **Sort by** duration (longest first)
4. **Click on the slowest trace**
5. **Identify bottleneck** in the waterfall view
6. **Check logs** for that trace ID to see detailed messages

### Workflow 2: Debug a Failed Order

1. **Check logs** for errors:
   ```bash
   cat */logs/*.json.log | jq 'select(.level == "ERROR")' | tail -5
   ```

2. **Get the trace ID** from the error log

3. **Open in Grafana** → Tempo → Search by trace ID

4. **Analyze the trace**:
   - Which service failed?
   - What was the status code?
   - How long did it take before failing?

5. **View all logs** for that trace:
   ```bash
   cat */logs/*.json.log | jq 'select(.traceId == "TRACE_ID")'
   ```

### Workflow 3: Monitor RabbitMQ Message Processing

1. **Search for RabbitMQ operations**:
   ```traceql
   { name =~ ".*rabbitmq.*" }
   ```

2. **Look at consumer latency**:
   ```traceql
   { span.kind="consumer" }
   ```

3. **Find slow message processing**:
   ```traceql
   { span.kind="consumer" && duration > 150ms }
   ```

---

## 🎭 What You Should See in Grafana

### In the Trace List

- Multiple traces from different services
- Trace IDs, duration, number of spans
- Service names (graphql-service, order-service, etc.)
- Start time

### In a Trace Detail View

**Services Involved (in order):**

1. **graphql-service** 
   - Root span: `http POST /graphql`
   - GraphQL mutation span
   - Field resolver span
   - HTTP client span (calling order-service)

2. **order-service**
   - HTTP server span: `http POST /orders`
   - RabbitMQ producer span: `send to orders.exchange`

3. **inventory-service** (async)
   - RabbitMQ consumer span: `receive from orders.queue`
   - Custom span: `inventory.update` (from @NewSpan)

4. **notification-service** (async)
   - RabbitMQ consumer span: `receive from notifications.queue`
   - Custom span: `notification.send` (from @NewSpan)

**Span Attributes You'll See:**

- `http.url`: `/graphql`, `/orders`
- `http.method`: `POST`
- `http.status_code`: `200`
- `messaging.destination.name`: `orders.exchange`, `orders.queue`
- `messaging.rabbitmq.destination.routing_key`: `orders.created`
- `graphql.operation.type`: `mutation`
- `graphql.field.name`: `createOrder`

---

## 📊 TraceQL Query Examples

### Basic Queries

```traceql
# All traces
{}

# Specific service
{ resource.service.name="graphql-service" }

# Multiple services
{ resource.service.name=~".*-service" }
```

### Performance Queries

```traceql
# Slow traces (>500ms)
{ duration > 500ms }

# Very fast traces (<50ms)
{ duration < 50ms }

# Traces in a duration range
{ duration >= 100ms && duration <= 500ms }
```

### Operation Queries

```traceql
# All HTTP POST operations
{ span.http.method="POST" }

# GraphQL operations
{ name =~ "graphql.*" }

# Database operations
{ name =~ ".*database.*" }

# RabbitMQ operations
{ span.kind="producer" || span.kind="consumer" }
```

### Error Queries

```traceql
# All errors
{ status=error }

# HTTP errors
{ span.http.status_code >= 400 }

# Errors in specific service
{ resource.service.name="order-service" && status=error }
```

### Complex Queries

```traceql
# Slow POST requests in order-service
{
  resource.service.name="order-service" && 
  span.http.method="POST" &&
  duration > 300ms
}

# RabbitMQ consumer spans that are slow
{
  span.kind="consumer" &&
  duration > 150ms
}

# All GraphQL mutations
{
  resource.service.name="graphql-service" &&
  name =~ "graphql mutation"
}
```

---

## 🛠️ Troubleshooting

### No Traces Appearing in Grafana?

**Check 1: Services are exporting**
```bash
tail -f logs/*.log | grep -i "export\|otlp"
```
You should NOT see 404 or connection errors.

**Check 2: Tempo is running**
```bash
docker compose ps tempo
docker compose logs tempo --tail=20
```

**Check 3: Traces exist in Tempo**
```bash
curl -s "http://localhost:3200/api/search" | jq
```

**Check 4: Generate a new trace**
```bash
./test_tracing_complete.sh
```

### Grafana Shows "No data"?

1. **Check time range**: Set to "Last 15 minutes" or "Last 1 hour"
2. **Run a test**: `./test_tracing_complete.sh` to generate fresh traces
3. **Wait 10 seconds**: Traces take a few seconds to appear
4. **Try simpler query**: Start with `{}` to see all traces

### TraceQL Query Returns Nothing?

1. **Verify syntax**: TraceQL is case-sensitive
2. **Correct attribute names**:
   - Use `resource.service.name` NOT `service.name`
   - Use `span.http.method` NOT `http.method`
3. **Check time range**: Make sure it covers when you created traces
4. **Try broader query**: Start with `{}` then filter down

---

## 🎉 Step-by-Step: Your First Trace

### 1. Generate a Trace

```bash
cd tracing-demo-v2
./test_tracing_complete.sh
```

Copy the trace ID from the output (e.g., `0a763aebdacc28dc27eb80dcdb92e8aa`)

### 2. Open Grafana

Browser → http://localhost:3000

### 3. Navigate to Tempo

- Click **Explore** (left sidebar)
- Select **Tempo** from dropdown

### 4. Search for Your Trace

**Method 1: By Trace ID**
- Switch to **"Search"** mode
- Find **"Trace ID"** field
- Paste: `0a763aebdacc28dc27eb80dcdb92e8aa`
- Click **"Run Query"**

**Method 2: By TraceQL**
- Switch to **"TraceQL"** mode
- Enter: `{ resource.service.name="order-service" }`
- Click **"Run Query"**
- Click on any trace in the results

### 5. Explore the Trace

You'll see:

1. **Service Graph** (top): Visual map of service calls
2. **Span List** (middle): All operations in this trace
3. **Timeline** (right): Waterfall view showing durations

**Click on any span** to see:
- Span ID and Trace ID
- Start time and duration
- Attributes (HTTP headers, RabbitMQ queues, etc.)
- Events (if any)
- Parent/child relationships

### 6. Correlate with Logs

```bash
# Copy the trace ID from Grafana, then:
cat */logs/*.json.log | jq 'select(.traceId == "0a763aebdacc28dc27eb80dcdb92e8aa")'
```

You'll see ALL log messages from ALL services for this single request!

---

## 🏆 What Makes This Special

Your distributed tracing implementation now supports:

### ✅ Complete Trace Continuity

A single trace ID flows through:
1. GraphQL request → GraphQL service
2. HTTP REST call → Order service
3. RabbitMQ message → Inventory service (async)
4. RabbitMQ message → Notification service (async)

### ✅ Rich Span Data

Each span includes:
- Operation name and type
- HTTP method, URL, status code
- RabbitMQ exchange, queue, routing key
- GraphQL operation and field names
- Custom attributes

### ✅ Service Map

Grafana automatically generates a service map showing:
- All microservices
- Communication paths
- Request rates
- Error rates

### ✅ Log Correlation

Every log entry includes:
- `traceId`: Links logs to traces
- `spanId`: Links logs to specific operations
- Service name, logger, message, timestamp

---

## 🚀 Advanced Features

### Span Filtering

Click on a span and use the **"Focus on this span"** button to see only that span and its children.

### Span Search

Use the search box in the trace view to filter spans by name.

### Copy Links

Click the **"Share"** button to get a permanent link to a specific trace.

### Export Trace

Download trace data as JSON for offline analysis.

---

## 📦 Architecture Overview

### What's Running:

| Component | Port | Purpose |
|-----------|------|---------|
| Grafana | 3000 | Visualization UI |
| Tempo | 3200 (HTTP), 4317 (gRPC OTLP) | Trace storage |
| Loki | 3100 | Log aggregation |
| RabbitMQ | 5672, 15672 | Message broker |
| GraphQL Service | 8080 | API gateway |
| Order Service | 8081 | Order processing |
| Inventory Service | 8082 | Inventory management |
| Notification Service | 8083 | Email notifications |

### Trace Flow:

```
User Request → GraphQL (8080)
    ↓ [HTTP, traceparent header]
Order Service (8081)
    ↓ [RabbitMQ, trace context in headers]
    ├─→ Inventory Service (8082)
    └─→ Notification Service (8083)
```

All using the **same trace ID**!

---

## 💡 Tips & Tricks

### 1. Time Range Matters

- Default is "Last 1 hour"
- If you just created a trace, use "Last 5 minutes"
- Traces older than 1 hour are deleted (configured in tempo.yaml)

### 2. TraceQL is Powerful

- Start broad: `{}`
- Then filter: `{ resource.service.name="order-service" }`
- Then refine: `{ resource.service.name="order-service" && duration > 200ms }`

### 3. Use Span Attributes

All span attributes can be queried:
```traceql
{ span.http.status_code=200 }
{ span.messaging.destination.name="orders.queue" }
{ span.graphql.operation.type="mutation" }
```

### 4. Generate Test Data

Run the test script multiple times to generate different traces:
```bash
for i in {1..5}; do ./test_tracing_complete.sh; sleep 2; done
```

### 5. Real-World Testing

Test via GraphQL UI:
```bash
# Open http://localhost:8080/graphiql
# Run this mutation:
mutation {
  createOrder(productId: "laptop", quantity: 10) {
    orderId
    status
    message
  }
}
```

Then search for the trace in Grafana!

---

## 🎓 Learning Path

### Beginner

1. Generate a trace with `./test_tracing_complete.sh`
2. Search by trace ID in Grafana
3. Click on the trace to see the waterfall view
4. Explore different spans

### Intermediate

1. Use TraceQL to find traces by service
2. Filter by duration to find slow traces
3. Correlate traces with logs using trace IDs
4. Create a simple dashboard

### Advanced

1. Write complex TraceQL queries
2. Create dashboards with multiple panels
3. Set up alerts for high latency or errors
4. Export traces for analysis
5. Use the service graph feature

---

## 📚 Additional Resources

### Documentation Files

- **FIXES_SUMMARY.md** - Technical details of all fixes applied
- **GRAFANA_GUIDE.md** - Comprehensive Grafana features guide
- **QUICK_START.md** - Quick reference guide

### External Resources

- [Tempo Documentation](https://grafana.com/docs/tempo/latest/)
- [TraceQL Language Guide](https://grafana.com/docs/tempo/latest/traceql/)
- [OpenTelemetry Docs](https://opentelemetry.io/docs/)
- [Spring Boot Observability](https://spring.io/blog/2022/10/12/observability-with-spring-boot-3)

---

## ✨ Success Checklist

You'll know everything is working when:

- ✅ Test script shows same trace ID across all services
- ✅ Grafana Tempo shows traces when you search
- ✅ Trace waterfall displays all 4 services
- ✅ Spans show parent-child relationships correctly
- ✅ No 404 or connection errors in service logs
- ✅ Logs contain matching trace IDs
- ✅ You can click through from GraphQL → Order → Inventory/Notification

---

## 🎯 Quick Reference Commands

```bash
# Start all services
./run_all.sh

# Stop all services
./stop_all.sh

# Test tracing
./test_tracing_complete.sh

# Check service health
curl http://localhost:8080/actuator/health  # GraphQL
curl http://localhost:8081/actuator/health  # Order
curl http://localhost:8082/actuator/health  # Inventory
curl http://localhost:8083/actuator/health  # Notification

# Check Tempo
curl http://localhost:3200/status
curl -s "http://localhost:3200/api/search" | jq

# View logs with trace
cat */logs/*.json.log | jq 'select(.traceId != null)'

# Docker services
docker compose ps
docker compose logs tempo
docker compose logs grafana
```

---

## 🎊 You're All Set!

Your distributed tracing is fully functional. Open **http://localhost:3000** and start exploring your traces!

**Pro tip**: Generate multiple traces by running:
```bash
for i in {1..10}; do ./test_tracing_complete.sh; sleep 1; done
```

Then explore different patterns, durations, and service interactions in Grafana!

**Happy Tracing! 🚀📊🔍**
