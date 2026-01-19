# Quick Start: HTTP + RabbitMQ Distributed Tracing

## 🎯 Goal
Demonstrate how a **single trace ID** flows through HTTP and RabbitMQ calls to help you observe the complete lifecycle of a request.

## 🚀 Quick Start (5 minutes)

### 1. Start Infrastructure (1 min)
```bash
cd tracing-demo-v2
docker-compose up -d
```

### 2. Start Services (2 min)

**Terminal 1:**
```bash
./gradlew :cqrs-service:bootRun
```

**Terminal 2:**
```bash
./gradlew :orchestrator-service:bootRun
```

### 3. Run Test (1 min)
```bash
./test_distributed_tracing.sh
```

### 4. View Trace (1 min)
1. Copy the trace ID from test output
2. Open http://localhost:3000
3. Go to Explore → Tempo
4. Paste trace ID
5. 🎉 See the complete trace!

## 📊 What You'll See

```
Single Trace showing:
├─ HTTP: Create product
├─ RabbitMQ: Update price
├─ RabbitMQ: Update stock
└─ HTTP: Query product

All with the SAME trace ID!
```

## 🔍 The Workflow

```bash
POST http://localhost:8085/api/workflows/product
```

Executes:
1. **HTTP** → Create product in CQRS service
2. **RabbitMQ** → Send price update message
3. **RabbitMQ** → Send stock update message
4. **HTTP** → Query product from CQRS service

## 💡 Key Insight

The trace shows how one request spans:
- ✅ Multiple services
- ✅ Multiple protocols (HTTP + RabbitMQ)
- ✅ Sync and async operations
- ✅ All connected by ONE trace ID

## 📚 Learn More

- **Detailed Guide**: [DISTRIBUTED_TRACING_GUIDE.md](DISTRIBUTED_TRACING_GUIDE.md)
- **Implementation Details**: [HTTP_RABBITMQ_TRACING_SUMMARY.md](HTTP_RABBITMQ_TRACING_SUMMARY.md)
- **Orchestrator Service**: [orchestrator-service/README.md](orchestrator-service/README.md)

## 🛠️ Manual Test

```bash
curl -X POST http://localhost:8085/api/workflows/product \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Gaming Laptop",
    "description": "High-performance laptop",
    "price": 2499.99,
    "initialStock": 10,
    "updatedPrice": 2299.99,
    "updatedStock": 15
  }'
```

Response includes:
- `traceId` - Use this to find the trace in Grafana
- `productId` - The created product ID
- `steps` - Summary of what happened

## 🎓 What This Demonstrates

**Problem**: How do I trace a request that uses both HTTP and messaging?

**Solution**: Spring Boot's OpenTelemetry automatically propagates trace context across:
- HTTP headers (`traceparent`)
- RabbitMQ message headers

**Result**: One trace ID for the entire request lifecycle!

## 🔧 Troubleshooting

**Services won't start?**
```bash
# Check if ports are free
netstat -tuln | grep -E '8084|8085'

# Check Docker services
docker-compose ps
```

**Trace not visible?**
- Wait 5-10 seconds for ingestion
- Check trace ID is correct
- Verify OpenTelemetry Collector: `docker logs otel-collector`

**RabbitMQ issues?**
- Check RabbitMQ UI: http://localhost:15672 (guest/guest)
- Verify queues exist: `cqrs.commands.queue`

## 📍 Ports

| Service | Port | URL |
|---------|------|-----|
| Orchestrator | 8085 | http://localhost:8085 |
| CQRS Service | 8084 | http://localhost:8084 |
| Grafana | 3000 | http://localhost:3000 |
| RabbitMQ UI | 15672 | http://localhost:15672 |

## ✅ Success Criteria

You'll know it's working when:
1. ✅ Test script completes successfully
2. ✅ You get a trace ID in the response
3. ✅ You can find the trace in Grafana
4. ✅ The trace shows both HTTP and RabbitMQ operations
5. ✅ All operations share the same trace ID

## 🎯 Next Steps

1. Run the demo
2. Explore the trace in Grafana
3. Look at the logs with trace IDs
4. Read the detailed guide
5. Implement in your own services!

---

**Questions?** See [DISTRIBUTED_TRACING_GUIDE.md](DISTRIBUTED_TRACING_GUIDE.md) for complete details.
