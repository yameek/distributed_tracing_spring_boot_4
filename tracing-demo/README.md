# Distributed Tracing Demo with generic Micrometer Tracing

This project demonstrates distributed tracing across:
1.  **GraphQL Service** (Entry Point)
2.  **REST Service** (Order Service)
3.  **RabbitMQ Consumer** (Inventory Service)

## Prerequisites
- Docker & Docker Compose
- Java 17+
- Maven

## How to Run

1.  **Start Infrastructure**
    ```bash
    cd tracing-demo
    docker-compose up -d
    ```
    This starts RabbitMQ, Grafana, Tempo, and Loki.

2.  **Build and Run Services**
    Open 3 separate terminals:

    *Terminal 1 (GraphQL Service)*:
    ```bash
    cd tracing-demo/graphql-service
    ../gradlew bootRun
    ```

    *Terminal 2 (Order Service)*:
    ```bash
    cd tracing-demo/order-service
    ../gradlew bootRun
    ```

    *Terminal 3 (Inventory Service)*:
    ```bash
    cd tracing-demo/inventory-service
    ../gradlew bootRun
    ```

    *Terminal 4 (Notification Service)*:
    ```bash
    cd tracing-demo/notification-service
    ../gradlew bootRun
    ```

3.  **Generate Traffic**
    Send a GraphQL mutation:
    ```bash
    curl -X POST -H "Content-Type: application/json" \
         -d '{"query": "mutation { createOrder(productId: \"Laptop\", quantity: 1) { orderId status } }"}' \
         http://localhost:8080/graphql
    ```

4.  **View Traces**
    - Open Grafana: [http://localhost:3000](http://localhost:3000)
    - Go to **Explore**.
    - Select **Tempo** datasource.
    - Click "Search" and run query (or find the latest trace).
    - You will see the full waterfall: `graphql-service` -> `order-service` -> `rabbitmq` -> `inventory-service`.
    - You can also split screen and select **Loki** to see logs correlated with the trace ID.
