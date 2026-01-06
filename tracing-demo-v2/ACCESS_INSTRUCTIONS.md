# 🎯 Quick Access Guide for Managers

## 📊 Main Dashboard - Start Here!

### **Grafana Dashboard (Recommended)**
**URL**: http://localhost:3000

**What you'll see**:
- ✅ Real-time logs from all services
- ✅ Service activity graphs
- ✅ Trace search and visualization
- ✅ Live system monitoring

**No login required** - Dashboard opens immediately!

---

## 🔍 Other Useful Interfaces

### GraphQL API Playground
**URL**: http://localhost:8080/graphiql

**What it does**: Test the system by creating orders
**How to use**:
1. Paste this query in the editor:
```graphql
mutation {
  createOrder(productId: "laptop", quantity: 2) {
    orderId
    status
  }
}
```
2. Click the "Execute" button
3. See the response with order ID
4. Check Grafana dashboard to see the trace!

### RabbitMQ Management Console
**URL**: http://localhost:15672
**Login**: guest / guest

**What it shows**:
- Message queues
- Message rates
- Queue depths
- Connection details

---

## 🧪 Testing the System

### Option 1: Use the Test Script (Easiest)
```bash
cd tracing-demo-v2
./test_system.sh
```

Then refresh the Grafana dashboard to see the new traces and logs!

### Option 2: Use GraphQL Playground
1. Go to http://localhost:8080/graphiql
2. Run the mutation (see above)
3. Check Grafana for traces

---

## 📈 What to Look For in the Dashboard

### 1. Log Volume Graph (Top Panel)
- **Steady line**: Normal operation
- **Spikes**: High activity (lots of orders)
- **Flat/gaps**: No activity or service down
- **Multiple colors**: Different services active

### 2. Service Logs (Middle Panel)
- **Real-time log stream**: See every action
- **Color-coded by service**:
  - graphql-service
  - order-service
  - inventory-service
  - notification-service
- **Click any trace ID**: Jump to full trace visualization

### 3. Trace Explorer (Bottom Panel)
- **Search traces**: By time or trace ID
- **Click a trace**: See detailed waterfall
- **Understand flow**: Which service took how long

---

## 🎬 Quick Demo Steps

### For a Live Demo:
1. **Open Grafana**: http://localhost:3000
2. **Show the dashboard**: Point out the three panels
3. **Generate activity**: Run `./test_system.sh` or use GraphQL playground
4. **Watch in real-time**: Logs appear immediately
5. **Explore a trace**: Click on a recent log entry, find trace ID
6. **Show the waterfall**: Demonstrate how request flows through all services

### Expected Flow:
```
User creates order
    ↓ (50ms)
GraphQL Service receives it
    ↓ (150ms)
Order Service saves to database
    ↓ (sends to message queue)
    ├─→ Inventory Service updates inventory (100ms)
    └─→ Notification Service sends email (150ms)
```

Total time: ~450ms for complete flow!

---

## 🚨 Troubleshooting

### "Connection refused" when accessing URLs:
**Solution**: Start the system
```bash
cd tracing-demo-v2
./run_all.sh
```
Wait 30 seconds for everything to start.

### Grafana shows no data:
**Solution**: Generate some activity
```bash
./test_system.sh
```

### Dashboard doesn't look right:
**Solution**: Navigate to the dashboard
1. Click on "Dashboards" (left sidebar)
2. Go to "Tracing Demo" folder
3. Click "Distributed Tracing Dashboard"

---

## 📝 Key Metrics to Monitor

### Service Health:
- ✅ All services showing logs = healthy
- ❌ No logs from a service = potential issue

### Performance:
- 🟢 Requests < 500ms = Good
- 🟡 Requests 500ms - 1s = Monitor
- 🔴 Requests > 1s = Investigate

### Message Processing:
- Check RabbitMQ for queue buildup
- Both inventory and notification should complete within 200ms

---

## 📞 Quick Reference

| What | URL | Credentials |
|------|-----|-------------|
| **Main Dashboard** | http://localhost:3000 | None |
| **GraphQL API** | http://localhost:8080/graphiql | None |
| **RabbitMQ** | http://localhost:15672 | guest/guest |

### Service Ports:
- GraphQL: 8080
- Order: 8081
- Inventory: 8082
- Notification: 8083

### Infrastructure Ports:
- Grafana: 3000
- Loki: 3100
- Tempo: 3200
- RabbitMQ: 5672, 15672

---

## 🎓 Understanding the Visualization

### Colors in Logs:
Each service has its own color for easy identification

### Trace Waterfall:
- **Width of bars**: How long each operation took
- **Vertical alignment**: Shows parent-child relationships
- **Gaps between bars**: Waiting time (queuing, network)

### Service Dependencies:
The system automatically shows which services call which, making it easy to understand the architecture!

---

## ✨ Best Features to Demonstrate

1. **Real-time monitoring**: Run test script and watch logs appear instantly
2. **Trace correlation**: Show how one request spans multiple services
3. **Performance analysis**: Point out which service is slowest
4. **Error tracking**: If something fails, the trace shows exactly where
5. **Async processing**: Show how inventory and notification run in parallel

---

## 🎯 Success Indicators

Your system is working correctly if you see:
- ✅ All 4 services in different colors in the logs
- ✅ "Email sent" messages in notification service logs
- ✅ "Inventory updated" messages in inventory service logs
- ✅ "Saved order to H2 database" in order service logs
- ✅ GraphQL requests completing successfully

---

## 📖 For More Details

See the complete guides:
- **VISUALIZATION_GUIDE.md** - Detailed dashboard usage
- **SUMMARY.md** - Technical details and architecture
- **README.md** - Project overview and setup

---

**Need Help?** Check the logs directory for detailed service logs:
```bash
tail -f tracing-demo-v2/logs/*.log
```
