# Distributed Tracing Demo Script

Use this script to demonstrate the power of **Micrometer Tracing**, **Grafana Tempo**, and **Loki** to your team.

## 1. Preparation
1.  Ensure the system is running:
    ```bash
    cd tracing-demo
    ./run_all.sh
    ```
    *(Wait until you see logs from all 4 services)*

2.  Open **Postman** (Import `postman_collection.json`).
3.  Open **Grafana** in your browser: `http://localhost:3000`.

## 2. The Setup (Show the Code)
Explain *how* we track requests across boundaries.

*   **GraphQL Entry Point**: Open `OrderController.java` (graphql-service).
    *   Show `@NewSpan("graphql.createOrder")`.
    *   *Script*: "We explicitly named this span to track the initial customer request."
*   **Context Propagation**: Open `OrderClient.java`.
    *   *Script*: "Micrometer automatically injects trace headers into this REST Template call."
*   **Async Hand-Off**: Open `OrderPublisher.java`.
    *   *Script*: "When we publish to RabbitMQ, the trace ID travels with the message headers."

## 3. The Trigger (Generate Traffic)
Execute the flow live.

1.  In Postman, run the **"Create Order (GraphQL)"** request.
2.  Point out the `200 OK` and the returned `orderId`.
3.  *Script*: "The order has been processed by 4 services and the database asynchronously."

## 4. The Reveal (Observability)
Show the result in Grafana.

1.  Go to **Explore** > **Tempo** > **Search**.
2.  Click **Run Query** and select the most recent trace (the blue dot).
3.  **Analyze the Waterfall**:
    *   **Root**: `graphql-service` starts the clock.
    *   **Logic**: `graphql.createOrder` (Our custom span).
    *   **REST**: `POST /orders` call to `order-service`.
    *   **Database**: `insert into orders` (Show JDBC attributes).
    *   **Fan-Out**:
        *   See the trace **split** into two parallel streams.
        *   `orders.queue` -> `inventory-service` (Updating stock).
        *   `notifications.queue` -> `notification-service` (Sending email).

## 5. Log Correlation (Debugging)
Demonstrate how to fix issues.

1.  Click on the `notification.send` span.
2.  Click the small **"Logs for this span"** icon (or toggle the logs view).
3.  Show the logs from **Loki**.
4.  *Script*: "We didn't search for logs. The Trace ID automatically linked us to the exact log lines for this specific operation across distributed systems."
