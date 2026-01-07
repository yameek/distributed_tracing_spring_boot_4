# Bits API SDK Generator Documentation

## Overview

The **Bits API SDK Generator** (BitsApiSDKGenerator) is a powerful tool designed to automate the generation of client-side SDKs from Spring Boot REST Controllers. It bridges the gap between backend services and client applications by ensuring that client libraries are always in sync with the API definition.

> Need the full onboarding flow (toolchain setup, helper scripts, Gradle wiring, and client usage tips)? See the [Developer Guide](docs/DEVELOPER_GUIDE.md) for a step-by-step experience.

**BitsApiSDKGenerator** consists of two main components:
1.  **`bits-sdk-annotations`**: A set of annotations to mark controllers and define SDK generation behavior.
2.  **`bits-sdk-processor`**: An annotation processor that generates the client SDK code (Interfaces, Implementations, DTOs, and Configuration) at compile time.

## Prerequisites

-   Java 21 or higher
-   Gradle (or Maven)
-   Spring Boot

## Installation

### Server-Side (SDK Generation)

To enable SDK generation in your Spring Boot application (the "Server"), you need to include the annotations and the processor in your build configuration.

**Gradle (`build.gradle`):**

```groovy
dependencies {
    // Add the annotations library
    implementation project(':bits-sdk-annotations') // Or the published artifact

    // Add the annotation processor
    annotationProcessor project(':bits-sdk-processor') // Or the published artifact
}
```

### Client-Side (SDK Usage)

To use the generated SDK in a client application, you need to include the generated code (usually packaged as a library) and the necessary dependencies.

**Gradle (`build.gradle`):**

```groovy
dependencies {
    // Add the generated SDK library
    implementation 'com.example:my-generated-sdk:1.0.0' 

    // Required dependencies for the generated code
    implementation 'org.springframework.boot:spring-boot-starter-web' // For RestClient
    implementation 'io.github.resilience4j:resilience4j-circuitbreaker:2.2.0' // For Circuit Breaker
}
```

## Server-Side Implementation

To generate an SDK for a controller, simply annotate the controller class with `@BitsSdk`.

### The `@BitsSdk` Annotation

Place this annotation on your `@RestController` class.

```java
import com.bracits.sdk.annotation.BitsSdk;
import org.springframework.web.bind.annotation.RestController;

@RestController
@BitsSdk(
    sdkPackage = "com.mycompany.sdk.client",
    clientName = "MyServiceClient",
    versioningStrategy = "header"
)
public class MyController {
    // ... endpoints
}
```

**Configuration Options:**

| Property | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `sdkPackage` | `String` | `"com.bits.sdk.generated"` | The package where the generated client code will be placed. |
| `clientName` | `String` | `""` (Empty) | The name of the generated client interface. If empty, it defaults to `{ControllerName}Client`. |
| `supportedVersions` | `String[]` | `{"v1"}` | List of supported API versions. |
| `defaultVersion` | `String` | `"v1"` | The default API version to use. |
| `generateBuilder` | `boolean` | `true` | Whether to generate Builder patterns for DTOs. |
| `versioningStrategy` | `String` | `"header"` | The API versioning strategy. Options: `"header"` (uses `X-API-VERSION` header) or `"url"` (appends version to path). |

### Supported Controller Features

The **Bits API SDK Generator** supports standard Spring Web annotations:
-   `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping`
-   `@RequestBody`
-   `@PathVariable`
-   `@RequestParam` (Note: Support might vary for complex types)
-   Return types: `ResponseEntity<T>`, `T`, `List<T>`, arrays, etc.

## Client-Side Integration

The generated SDK provides a strongly-typed client interface and a robust implementation using Spring's `RestClient` and Resilience4j's `CircuitBreaker`.

### 1. Configuration

You must provide beans for `ClientConfiguration` and `CircuitBreakerConfiguration` in your Spring context. These beans are used by the generated client implementation.

```java
import com.mycompany.sdk.client.config.ClientConfiguration;
import com.mycompany.sdk.client.config.CircuitBreakerConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SdkConfig {

    @Bean
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
            .baseUrl("http://localhost:8080") // URL of the backend service
            .connectTimeout(5000)
            .readTimeout(10000)
            .maxRetries(3)
            .build();
    }

    @Bean
    public CircuitBreakerConfiguration circuitBreakerConfiguration() {
        return CircuitBreakerConfiguration.builder()
            .name("my-service-cb")
            .failureRateThreshold(50)
            .waitDurationInOpenState(10000)
            .build();
    }
}
```

### 2. Usage

Inject the generated client interface into your service or component. The implementation is automatically annotated with `@Component`, so it will be auto-detected if component scanning covers the generated package.

```java
import com.mycompany.sdk.client.MyServiceClient;
import com.mycompany.sdk.client.dto.request.CreateUserRequest;
import com.mycompany.sdk.client.dto.response.UserResponse;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final MyServiceClient myServiceClient;

    public UserService(MyServiceClient myServiceClient) {
        this.myServiceClient = myServiceClient;
    }

    public void registerUser() {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");

        UserResponse response = myServiceClient.createUser(request);
        
        System.out.println("User created: " + response.getId());
    }
}
```

## Features

### Circuit Breaker
The SDK automatically wraps all API calls with a Resilience4j Circuit Breaker. This prevents cascading failures and improves system resilience. You can customize the circuit breaker settings (thresholds, timeouts, etc.) via the `CircuitBreakerConfiguration`.

### API Versioning
The SDK supports API versioning out of the box.
-   **Header Strategy**: Sends the version in the `X-API-VERSION` header.
-   **URL Strategy**: Appends the version to the URL path (e.g., `/v1/users`).

You can configure the strategy in the `@BitsSdk` annotation and override the version at runtime via `ClientConfiguration`.

### DTO Generation
The processor generates DTOs (Data Transfer Objects) matching the request and response bodies of your controller methods. If `generateBuilder` is enabled, these DTOs will include fluent builder methods for easier instantiation.
