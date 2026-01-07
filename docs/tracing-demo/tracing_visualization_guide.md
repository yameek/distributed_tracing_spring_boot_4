# Distributed Tracing Visualization Guide

## Overview
This guide explains how to access and view distributed traces and spans for monitoring the microservices system in real-time.

## Accessing Grafana Dashboard

### 1. Open Grafana
- **URL**: http://localhost:3000
- **Authentication**: None required (anonymous access enabled)
- The dashboard will open automatically

### 2. Navigate to the Dashboard
- Look for "**Distributed Tracing Dashboard**" in the Grafana sidebar
- Or navigate to: **Dashboards → Tracing Demo → Distributed Tracing Dashboard**

## Understanding the Dashboard

### Panel 1: Log Volume by Service
- **Purpose**: Shows the activity level of each microservice over time
- **What to look for**:
  - Spikes indicate high activity
  - Gaps indicate no activity or service downtime
  - Compare volumes across services to see load distribution

### Panel 2: Service Logs (with Trace IDs)
- **Purpose**: Real-time logs from all microservices
- **Features**:
  - Each log entry contains a trace ID linking it to distributed traces
  - Click on a trace ID to jump to the full trace visualization
  - Filter by service name, log level, or search for specific text
- **Color coding**:
  - Services are color-coded for easy identification
  - Error logs are highlighted in red

### Panel 3: Trace Explorer
- **Purpose**: Search and visualize distributed traces across all services
- **How to use**:
  1. Use the search box to find traces by trace ID, service name, or time range
  2. Click on any trace to see the full span timeline
  3. See how requests flow through all services (graphql → order → inventory + notification)

## Exploring Individual Traces

### Trace Timeline View
When you click on a trace, you'll see:
- **Waterfall diagram**: Shows each span and how long it took
- **Service flow**: See which services were called and in what order
- **Timing information**: Total duration, time spent in each service
- **Parent-child relationships**: How spans are nested

### What Each Span Represents:
- **graphql-service**: Initial GraphQL mutation (createOrder)
- **order-service**: Order processing and database save
- **inventory-service**: Inventory update (async via RabbitMQ)
- **notification-service**: Email notification (async via RabbitMQ)

## Monitoring Best Practices

### For Real-Time Monitoring:
1. Keep the dashboard open with auto-refresh enabled (5 seconds)
2. Watch for:
   - **Error spikes**: Check logs panel for error messages
   - **Slow traces**: Look for traces with unusually long durations
   - **Failed requests**: Missing spans in a trace indicate failures

### For Troubleshooting:
1. **Find the trace ID** from logs or error reports
2. Search for it in the Trace Explorer
3. Analyze the span waterfall to find:
   - Which service is slow
   - Where errors occurred
   - Missing or incomplete spans

### For Performance Analysis:
1. Compare trace durations over time
2. Identify bottlenecks by looking at span durations
3. Check if async operations (RabbitMQ) are completing

## Service Architecture Flow

```
User Request
    ↓
GraphQL Service (port 8080)
    ↓
Order Service (port 8081)
    ├─ Save to H2 Database
    └─ Publish to RabbitMQ
        ├─→ Inventory Service (port 8082) [async]
        └─→ Notification Service (port 8083) [async]
```

## Additional Resources

### Service Endpoints:
- **GraphQL UI**: http://localhost:8080/graphiql
- **Grafana**: http://localhost:3000
- **RabbitMQ Management**: http://localhost:15672 (guest/guest)

### Generating Test Traffic:
Run from the project directory:
```bash
./test_system.sh
```

This will create a new order and generate traces across all services.

## Common Trace Patterns

### Successful Request:
- 1 span from graphql-service
- 1 span from order-service
- 1 span from inventory-service (async)
- 1 span from notification-service (async)
- All spans complete successfully
- Total duration: ~300-500ms

### Async Processing:
- Inventory and notification services process independently
- Their spans may appear slightly delayed (RabbitMQ delivery time)
- This is normal and expected behavior

## Troubleshooting

### No Traces Appearing:
1. Verify all services are running: `docker ps` and check ports 8080-8083
2. Generate traffic using `./test_system.sh`
3. Check service logs in the `logs/` directory
4. Verify Tempo is running: `docker ps | grep tempo`

### Incomplete Traces:
- One service may have failed
- Check the logs panel for error messages
- Look for connection errors or timeouts

### Slow Performance:
- Check span durations to identify the slowest service
- Look for database query times in order-service spans
- Check RabbitMQ message queue lengths

## Contact & Support
For issues or questions about the tracing system, check:
- Service logs in `./logs/` directory
- Docker container logs: `docker logs tracing-demo-v2-<service>-1`
- Application configuration in `*/src/main/resources/application.yml`
