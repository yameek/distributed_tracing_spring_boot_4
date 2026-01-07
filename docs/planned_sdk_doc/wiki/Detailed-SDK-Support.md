# Bits API SDK Generator - Detailed Support & Limitations

This document provides an in-depth technical breakdown of supported features, type mappings, and known limitations of the **Bits API SDK Generator** (BitsApiSDKGenerator).

## 1. Type Support Matrix

The **Bits API SDK Generator** categorizes types into **Basic**, **JDK**, and **Complex**.

### ✅ Basic Types (Passed Through)
These types are used directly in the generated SDK without modification.
*   **Primitives**: `int`, `long`, `double`, `boolean`, `float`, `byte`, `short`, `char`
*   **Wrappers**: `Integer`, `Long`, `Double`, `Boolean`, `Float`, `Byte`, `Short`, `Character`
*   **String**: `String`
*   **Void**: `void`, `Void`
*   **Object**: `Object`

### ✅ JDK Types (Imported)
Types starting with `java.`, `javax.`, `jakarta.` are considered JDK types. They are **not** generated as DTOs but are imported if referenced.
*   **Collections**: `List<T>`, `Set<T>`, `Map<K,V>`, `ArrayList`, `HashMap`, `Optional<T>`
*   **Date/Time**: `java.time.LocalDate`, `java.time.LocalDateTime`, `java.time.ZonedDateTime`, `java.util.Date` (Assuming Jackson can serialize them).
*   **UUID**: `java.util.UUID`

### 🧩 Complex Types (Generated)
Any type that is not Basic or JDK is considered **Complex** and triggers DTO generation.
*   **Classes**: Generated as POJOs with getters, setters (via Lombok or manual), and builder pattern.
*   **Records**: Supported. Generated as standard Classes with getters (no setters) to ensure immutability in the client where possible, or just standard POJOs.
*   **Enums**: Fully supported. Generated as Java `enum` types.
*   **Generics**: Supported (e.g., `Page<UserDto>`). The generator recursively processes type arguments.
*   **Nested Classes**: Supported. Inner classes are generated as standalone classes in the package.

## 2. DTO Generation Specifics

### Package Structure
*   **Request DTOs**: Placed in `[sdkPackage].dto.request`
*   **Response DTOs**: Placed in `[sdkPackage].dto.response`

### ⚠️ Code Duplication (Shared DTOs)
If a DTO class `UserDto` is used in both a `@RequestBody` (Request) and a `ResponseEntity<UserDto>` (Response), the generator will create **two separate classes**:
1.  `...dto.request.UserDto`
2.  `...dto.response.UserDto`
This is a known design choice to decouple request and response contracts, but it means they are not interchangeable types in the client code.

### Inheritance
*   **Support**: ✅ Yes.
*   If `ManagerDto extends EmployeeDto`, the generated `ManagerDto` will `extend EmployeeDto`.
*   The parent class `EmployeeDto` is also generated.

### JSON Configuration
All generated DTOs are annotated with:
```java
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
```
This ensures resilience against missing fields or extra fields sent by the server.

## 3. Controller Mapping Details

### Path Construction
The client combines the Controller's `@RequestMapping` path with the Method's mapping path.
*   **Logic**: `Base Path` + `Method Path`
*   **Version Stripping**: The generator attempts to strip version prefixes matching `/v\d+/` from the **method path** (not the controller path).
    *   *Example*: `@GetMapping("/v1/users")` -> Client calls `/users` (relative to base URL).
    *   *Warning*: Ensure your `ClientConfiguration` base URL includes the version if you rely on this stripping, or use the Header Versioning strategy.

### Query Parameters (`@RequestParam`)
*   **Support**: ✅ Yes.
*   **Implementation**: Manually constructs the query string.
*   **⚠️ Limitation (Null Safety)**: The current implementation appends variables directly. If a parameter is `null`, it may be appended as the literal string `"null"` (e.g., `?param=null`).
    *   *Workaround*: Ensure primitive types are used where possible, or validate non-null values before calling the SDK.

### Path Variables (`@PathVariable`)
*   **Support**: ✅ Yes.
*   **Mapping**: Matches `@PathVariable("name")` or falls back to the parameter name.
*   **Replacement**: Replaces `{name}` placeholders in the URL with the argument value.

### Headers (`@RequestHeader`)
*   **Support**: ✅ Yes.
*   **Naming**: Converts kebab-case headers to camelCase arguments (e.g., `X-Correlation-ID` -> `xCorrelationIdValue`).

## 4. Client Configuration & Resilience

### Circuit Breaker
Every SDK method is automatically wrapped in a Resilience4j Circuit Breaker.
*   **Configurable**: Yes, via `CircuitBreakerConfiguration` bean.
*   **Defaults**:
    *   Failure Threshold: 50%
    *   Wait Duration: 10s
    *   Sliding Window: 10 calls

### Retries
*   **Configurable**: Yes, via `ClientConfiguration`.
*   **Default**: 3 retries.

## 5. Known Limitations & Edge Cases

| Feature | Limitation | Impact |
| :--- | :--- | :--- |
| **Null Query Params** | Null objects sent as "null" string | Server might parse "null" as a value instead of missing. |
| **Shared DTOs** | Duplicated in Request/Response packages | Cannot pass a Response object directly to a Request method without mapping. |
| **Complex Headers** | Only simple string conversion | Complex objects in headers might rely on `toString()`, which is often insufficient. |
| **File Uploads** | `MultipartFile` not supported | Cannot generate SDKs for file upload endpoints. |
| **Polymorphism** | Jackson `@JsonSubTypes` not generated | Polymorphic deserialization might not work out-of-the-box in the client. |
| **Validation** | `@Valid`, `@NotNull` ignored | Validation annotations are not copied to the generated DTOs. Client-side validation is manual. |
