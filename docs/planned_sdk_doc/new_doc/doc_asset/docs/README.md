# Demo Spring Boot Application

This is a demo Spring Boot application created to test the **Bits API SDK Generator** (BitsApiSDKGenerator) annotation processor.

## Project Structure

```
demo-app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/demo/
│   │   │       ├── DemoApplication.java          # Main Spring Boot application
│   │   │       ├── controller/
│   │   │       │   ├── UserController.java       # User REST controller with @BitsSdk
│   │   │       │   ├── ProductController.java    # Product REST controller with @BitsSdk
│   │   │       │   └── OrderController.java      # Order REST controller with @BitsSdk
│   │   │       ├── service/
│   │   │       │   ├── UserService.java          # User business logic
│   │   │       │   └── ProductService.java       # Product business logic
│   │   │       └── dto/
│   │   │           ├── request/                  # Request DTOs (POJOs consumed by SDK processor)
│   │   │           └── response/                 # Response DTOs
│   │   └── resources/
│   │       └── application.properties
├── build.gradle
└── settings.gradle
```

## Prerequisites

1. **Build the SDK first** (from the parent directory):
   ```bash
   cd /home/yaziz/workspace/V3_POC/api-sdk
   ./gradlew clean
   ./gradlew :bits-sdk-annotations:build
   ./gradlew :bits-sdk-processor:build
   ./gradlew publishToMavenLocal
   ```

2. **Java 21** installed

3. **Gradle 8.x** or higher

## Building the Demo Application

1. Navigate to the demo-app directory:
   ```bash
   cd demo-app
   ```

2. Build the project (ensures SDK clients + DTOs are generated):
   ```bash
   ../gradlew build
   ```

   Or if you're in the demo-app directory:
   ```bash
   ./gradlew build
   ```

## Generated SDK Files

After building, the SDK annotation processor will generate files in:

```
demo-app/build/generated/sources/annotationProcessor/java/main/com/example/sdk/generated/
├── client/
│   ├── UserClient.java              # Generated client interface
│   ├── UserClientImpl.java          # Generated client implementation
│   ├── ProductClient.java           # Generated client interface
│   ├── ProductClientImpl.java       # Generated client implementation
│   ├── OrderClient.java             # Generated client interface
│   └── OrderClientImpl.java         # Generated client implementation
├── config/
│   ├── ApiVersion.java
│   ├── ClientConfiguration.java
│   ├── CircuitBreakerConfiguration.java
│   ├── RetryableRestClientInterceptor.java
│   └── ApiVersionHeaderInterceptor.java
└── dto/
    ├── request/
    │   ├── CreateUserRequest.java
    │   ├── UpdateUserRequest.java
    │   ├── CreateProductRequest.java
    │   ├── UpdateProductRequest.java
    │   ├── CreateOrderRequest.java
    │   └── UpdateOrderStatusRequest.java
    └── response/
        ├── UserResponse.java
        ├── ProductResponse.java
        └── OrderResponse.java
```

## Running the Application

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

## API Endpoints

### User API (`/v1/users`)
- `POST /v1/users` - Create a user
- `GET /v1/users/{userId}` - Get user by ID
- `GET /v1/users` - Get all users (returns array)
- `PUT /v1/users` - Update user (body includes `id`)
- `DELETE /v1/users/{userId}` - Delete user

### Product API (`/v1/products`)
- `POST /v1/products` - Create a product
- `GET /v1/products/{productId}` - Get product by ID
- `GET /v1/products?category={category}` - Get all products (returns array, optionally filtered)
- `PUT /v1/products` - Update product (body includes `id`)
- `DELETE /v1/products/{productId}` - Delete product

### Order API (`/v1/orders`)
- `POST /v1/orders` - Create an order
- `GET /v1/orders/{orderId}` - Get order by ID
- `GET /v1/orders` - Get all orders (returns array)
- `PATCH /v1/orders/status` - Update order status (body includes `orderId`)
- `DELETE /v1/orders/{orderId}` - Delete order

## Testing the Generated SDK

After building, you can use the generated SDK clients in another application or test them in this same application by:

1. Creating a configuration class:
   ```java
   @Configuration
   public class SdkConfiguration {
       @Bean
       public ClientConfiguration userClientConfiguration() {
           return ClientConfiguration.builder()
               .baseUrl("http://localhost:8080")
               .apiVersion(ApiVersion.V1)
               .build();
       }
   }
   ```

2. Injecting and using the client:
   ```java
   @Service
   public class MyService {
       private final UserClient userClient;
       
       public MyService(UserClient userClient) {
           this.userClient = userClient;
       }
       
       public void test() {
           CreateUserRequest request = new CreateUserRequest();
           request.setUsername("testuser");
           request.setEmail("test@example.com");
           UserResponse user = userClient.createUser(request);
       }
   }
   ```

## Notes

- The SDK processor runs during compilation.
- Generated files live under `build/generated/sources/annotationProcessor/`.
- Ensure the SDK artifacts are published to Maven Local before building this app.
- Generated clients depend on Spring `RestClient` and Resilience4j CircuitBreaker (provided via `resilience4j-circuitbreaker` dependency in `build.gradle`).

