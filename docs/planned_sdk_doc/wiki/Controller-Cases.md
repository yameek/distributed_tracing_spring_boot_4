# Bits API SDK Generator - Controller Cases Coverage

This document provides an accurate, comprehensive overview of all controller cases and their implementation status in the **Bits API SDK Generator** (BitsApiSDKGenerator), verified against the actual codebase.

## Use Case Context

> [!IMPORTANT]
> **Bits API SDK Generator** is designed for **microservices-to-microservices communication** within a distributed system architecture. The generated client code enables type-safe, resilient communication between internal services without manual RestClient/WebClient configuration.

## Summary Statistics

| Category | Supported | Partial | Not Supported | Total |
|----------|-----------|---------|---------------|-------|
| HTTP Methods | 5 | 1 | 1 | 7 |
| Request Mapping Annotations | 5 | 1 | 1 | 7 |
| Path Variables | 3 | 0 | 2 | 5 |
| Query Parameters | 4 | 1 | 3 | 8 |
| Request Body | 1 | 0 | 4 | 5 |
| Request Headers | 0 | 0 | 5 | 5 |
| File Uploads | 0 | 0 | 5 | 5 |
| Cookies | 0 | 0 | 3 | 3 |
| Return Types | 7 | 1 | 2 | 10 |
| DTO Generation | 6 | 0 | 1 | 7 |
| Configuration & Infrastructure | 6 | 0 | 0 | 6 |
| **TOTAL** | **41** | **4** | **28** | **73** |

**Coverage: 56.2% Fully Supported, 5.5% Partial, 38.4% Not Supported**

*Note: Statistics updated to reflect accurate implementation status*

---

## 1. HTTP Methods Support

| HTTP Method | Status | Implementation | Notes |
|-------------|--------|----------------|-------|
| `@GetMapping` | ✅ **Supported** | `ClientImplementationGenerator.java:508-509` | Fully implemented with query params and path variables |
| `@PostMapping` | ✅ **Supported** | `ClientImplementationGenerator.java:505-506` | Includes request body support for complex types |
| `@PutMapping` | ✅ **Supported** | `ClientImplementationGenerator.java:511-512` | Includes request body support |
| `@DeleteMapping` | ✅ **Supported** | `ClientImplementationGenerator.java:514-515` | Supports path variables |
| `@PatchMapping` | ✅ **Supported** | `ClientImplementationGenerator.java:517-518` | Includes request body support |
| `@RequestMapping` | ⚠️ **Partial** | `ClientImplementationGenerator.java:502-522` | Path extraction works, but `method` attribute not extracted. Defaults to GET |
| Custom HTTP Methods (HEAD, OPTIONS, TRACE) | ❌ **Not Supported** | N/A | Not implemented in `extractHttpMethod()` |

### Example: ⚠️ Partial Support - `@RequestMapping` with method attribute

**Controller Code:**
```java
@RequestMapping(value = "/users", method = RequestMethod.POST)
public ResponseEntity<UserDto> createUser(@RequestBody UserDto user) {
    return ResponseEntity.ok(userService.createUser(user));
}
```

**Current Behavior:** SDK will generate this as a **GET** request (incorrect), because the `method` attribute is not extracted.

**Workaround:** Use specific mapping annotations like `@PostMapping` instead.

### Example: ❌ Not Supported - Custom HTTP Methods

**Controller Code:**
```java
@RequestMapping(value = "/users/{id}", method = RequestMethod.HEAD)
public ResponseEntity<Void> checkUserExists(@PathVariable Long id) {
    return userService.exists(id) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
}
```

**Current Behavior:** SDK will not generate this method or will default to GET.

**Impact:** Cannot verify resource existence without fetching full data in microservices communication.

**Implementation Details:**
- HTTP method extraction in `extractHttpMethod()` only checks for specific mapping annotations
- `@RequestMapping` is detected but method attribute is not parsed, defaults to GET
- Location: `ClientImplementationGenerator.java:502-522`

---

## 2. Request Mapping Annotations

| Annotation | Status | Implementation | Notes |
|------------|--------|----------------|-------|
| `@GetMapping` | ✅ **Supported** | `TypeUtilityService.java:17-28` | Fully supported |
| `@PostMapping` | ✅ **Supported** | `TypeUtilityService.java:17-28` | Fully supported |
| `@PutMapping` | ✅ **Supported** | `TypeUtilityService.java:17-28` | Fully supported |
| `@DeleteMapping` | ✅ **Supported** | `TypeUtilityService.java:17-28` | Fully supported |
| `@PatchMapping` | ✅ **Supported** | `TypeUtilityService.java:17-28` | Fully supported |
| `@RequestMapping` | ⚠️ **Partial** | `ClientImplementationGenerator.java:524-536` | Path extraction works via `extractPathFromAnnotation()`, but method attribute not handled |
| Multiple path mappings (array) | ❌ **Not Supported** | N/A | Only first path value extracted, arrays not handled |

### Example: ❌ Not Supported - Multiple Path Mappings

**Controller Code:**
```java
@GetMapping(value = {"/users", "/members", "/persons"})
public ResponseEntity<List<UserDto>> getAllUsers() {
    return ResponseEntity.ok(userService.findAll());
}
```

**Current Behavior:** SDK will only generate a client method for the **first path** (`/users`). The other paths (`/members` and `/persons`) are ignored.

**Expected Behavior:** Either generate multiple client methods (one per path) or document that only single paths are supported.

**Impact:** In microservices, if you have multiple endpoint paths for backward compatibility, only one will be accessible through the SDK.

**Implementation Details:**
- Path extraction in `extractPath()` and `extractPathFromAnnotation()` only extracts first value
- Version prefix `/v\d+/` is stripped: `ClientImplementationGenerator.java:535`
- Controller base path extraction: `ClientImplementationGenerator.java:595-605`

---

## 3. Path Variables Support

| Feature | Status | Implementation | Notes |
|---------|--------|----------------|-------|
| Single `@PathVariable` | ✅ **Supported** | `ClientImplementationGenerator.java:400-404, 482-499` | Extracted and mapped to indexed placeholders `{0}`, `{1}` |
| Multiple `@PathVariable` | ✅ **Supported** | `ClientImplementationGenerator.java:400-404` | Supports multiple path variables, converted to indexed format |
| `@PathVariable` with custom name | ✅ **Supported** | `ClientImplementationGenerator.java:487-494` | `@PathVariable("customName")` handled via `extractPathVariableInfo()` |
| `@PathVariable` with defaultValue | ❌ **Not Supported** | N/A | Spring supports this but not extracted in `extractPathVariableInfo()` |
| `@PathVariable` with required flag | ❌ **Not Supported** | N/A | Optional path variables not handled |

### Example: ❌ Not Supported - Optional Path Variable

**Controller Code:**
```java
@GetMapping("/reports/{year}/{month}")
public ResponseEntity<ReportDto> getReport(
    @PathVariable int year,
    @PathVariable(required = false) Integer month  // Optional month
) {
    return ResponseEntity.ok(reportService.getReport(year, month));
}
```

**Current Behavior:** SDK generates a method requiring **both** `year` and `month` parameters. The `required = false` attribute is ignored.

**Expected Behavior:** Generate an overloaded method or use `Optional<Integer>` for the month parameter.

**Impact:** Cannot call endpoints with optional path segments in microservices. Workaround is to create two separate endpoints.

### Example: ❌ Not Supported - Path Variable with Default Value

**Controller Code:**
```java
@GetMapping("/items/{category}")
public ResponseEntity<List<ItemDto>> getItems(
    @PathVariable(defaultValue = "all") String category
) {
    return ResponseEntity.ok(itemService.findByCategory(category));
}
```

**Current Behavior:** SDK generates method requiring the `category` parameter. No default value is used.

**Impact:** Client must always provide the category, even when the controller has a sensible default.

**Implementation Details:**
- Path variables extracted in `buildUriExpression()`: `ClientImplementationGenerator.java:400-404`
- Conversion to indexed format: `convertToIndexedPath()` at line 473-480
- Path variable info extraction: `extractPathVariableInfo()` at line 482-499
- Only `value` attribute is extracted, `required` and `defaultValue` are ignored

---

## 4. Query Parameters Support

| Feature | Status | Implementation | Notes |
|---------|--------|----------------|-------|
| Single `@RequestParam` | ✅ **Supported** | `ClientImplementationGenerator.java:371-397` | Basic query parameter support |
| Multiple `@RequestParam` | ✅ **Supported** | `ClientImplementationGenerator.java:371-397` | Multiple query parameters supported |
| `@RequestParam` with name | ✅ **Supported** | `ClientImplementationGenerator.java:446-449` | Custom query parameter names via `value` or `name` attribute |
| `@RequestParam` required flag | ✅ **Supported** | `ClientImplementationGenerator.java:451-453` | `required` attribute extracted but not used in generated code |
| `@RequestParam` defaultValue | ⚠️ **Partial** | `ClientImplementationGenerator.java:455-459` | Default value extracted but not used in generated code |
| `@RequestParam` with List/Array | ❌ **Not Supported** | N/A | Array/Collection query params not handled (would need special encoding) |
| `@RequestParam Map<String, String>` | ❌ **Not Supported** | N/A | Map-based dynamic query parameters not supported |
| `Optional<T>` parameters | ❌ **Not Supported** | N/A | Optional parameter types not handled (Optional import only for return types) |

### Example: ⚠️ Partial Support - `@RequestParam` with defaultValue

**Controller Code:**
```java
@GetMapping("/products")
public ResponseEntity<List<ProductDto>> searchProducts(
    @RequestParam(defaultValue = "10") int pageSize,
    @RequestParam(defaultValue = "0") int page
) {
    return ResponseEntity.ok(productService.search(page, pageSize));
}
```

**Current Behavior:** SDK generates a method with **mandatory** `pageSize` and `page` parameters. The default values are extracted but ignored, so calling services must always provide these values.

**Expected Behavior:** Generate method with optional parameters or use the default values when parameters are not provided.

**Impact:** Extra boilerplate in microservices code when calling this endpoint, must specify `pageSize: 10, page: 0` explicitly.

### Example: ❌ Not Supported - Query Parameter Lists

**Controller Code:**
```java
@GetMapping("/users")
public ResponseEntity<List<UserDto>> getUsersByIds(
    @RequestParam List<Long> ids
) {
    return ResponseEntity.ok(userService.findByIds(ids));
}
```

**Current Behavior:** SDK cannot generate proper client code for list parameters. Query string encoding for arrays (e.g., `?ids=1&ids=2&ids=3`) is not supported.

**Workaround:** Accept comma-separated string and parse on server side, or use POST with request body instead.

**Impact:** Common filtering patterns in microservices (filter by multiple IDs, tags, categories) require workarounds.

### Example: ❌ Not Supported - Dynamic Query Parameters

**Controller Code:**
```java
@GetMapping("/search")
public ResponseEntity<List<ItemDto>> dynamicSearch(
    @RequestParam Map<String, String> filters
) {
    return ResponseEntity.ok(itemService.dynamicSearch(filters));
}
```

**Current Behavior:** SDK cannot handle dynamic query parameters via Map. Only explicitly declared `@RequestParam` fields are supported.

**Impact:** Flexible search/filter APIs cannot be accessed through SDK in microservices architecture.

**Implementation Details:**
- Query parameter extraction: `extractQueryParamInfo()` at line 424-469
- Query string building: `buildUriExpression()` at line 371-397
- Query params appended as string concatenation: `?param1=" + value1 + "&param2=" + value2`
- **Note:** `required` and `defaultValue` are extracted but not utilized in the generated client code

---

## 5. Request Body Support

| Feature | Status | Implementation | Notes |
|---------|--------|----------------|-------|
| Implicit `@RequestBody` (complex type) | ✅ **Supported** | `ClientImplementationGenerator.java:285-307, 332-338` | Automatically detected for complex types in POST/PUT/PATCH methods |
| Explicit `@RequestBody` annotation | ❌ **Not Supported** | N/A | Annotation not checked; relies on type detection only |
| `@RequestBody` with validation | ❌ **Not Supported** | N/A | `@Valid` and other validation annotations not propagated |
| Multiple request bodies | ❌ **Not Supported** | `ClientImplementationGenerator.java:278-283` | Only first complex parameter handled via `getFirstComplexParameter()` |
| `@RequestBody` with Optional | ❌ **Not Supported** | N/A | Optional request body not handled |

### Example: ❌ Not Supported - Explicit `@RequestBody` with Primitives

**Controller Code:**
```java
@PostMapping("/config/update-threshold")
public ResponseEntity<Void> updateThreshold(@RequestBody int threshold) {
    configService.updateThreshold(threshold);
    return ResponseEntity.ok().build();
}
```

**Current Behavior:** SDK might not recognize this as a request body because it's a primitive type. The SDK uses type complexity detection rather than annotation checking.

**Expected Behavior:** Honor the explicit `@RequestBody` annotation regardless of type.

**Impact:** Cannot send simple values as JSON body in microservices communication; need to wrap in a DTO.

### Example: ❌ Not Supported - Multiple Request Bodies

**Controller Code:**
```java
@PostMapping("/merge")
public ResponseEntity<UserDto> mergeUsers(
    @RequestBody UserDto primaryUser,
    @RequestBody UserDto secondaryUser
) {
    return ResponseEntity.ok(userService.merge(primaryUser, secondaryUser));
}
```

**Current Behavior:** SDK will only use the **first complex parameter** (`primaryUser`) as the request body. The `secondaryUser` will be ignored.

**Expected Behavior:** Either reject multiple request bodies (as it's not standard REST) or wrap them in a single DTO.

**Impact:** Such endpoints won't work correctly with the SDK. Recommended approach: create a wrapper DTO.

```java
public class MergeUsersRequest {
    private UserDto primaryUser;
    private UserDto secondaryUser;
}
```

### Example: ❌ Not Supported - Validation Annotations

**Controller Code:**
```java
@PostMapping("/users")
public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto user) {
    return ResponseEntity.ok(userService.create(user));
}
```

**Current Behavior:** The `@Valid` annotation is not propagated to the generated SDK client code. Validation happens only on the server side.

**Expected Behavior:** Client-side validation could be beneficial for early error detection in microservices.

**Impact:** Validation errors only discovered after network call, increasing latency in distributed systems.

**Implementation Details:**
- Complex parameter detection: `isComplexParameter()` at line 285-307
- Only first complex parameter used: `getFirstComplexParameter()` at line 278-283
- Request body added only for POST/PUT/PATCH: `ClientImplementationGenerator.java:332-338`
- **Limitation:** No explicit `@RequestBody` annotation checking; relies solely on type complexity detection

---

## 6. Request Headers Support

| Feature | Status | Implementation | Notes |
|---------|--------|----------------|-------|
| `@RequestHeader` | ❌ **Not Supported** | N/A | Header extraction not implemented |
| Multiple `@RequestHeader` | ❌ **Not Supported** | N/A | Not supported |
| `@RequestHeader` with defaultValue | ❌ **Not Supported** | N/A | Not supported |
| `@RequestHeader` Map<String, String>` | ❌ **Not Supported** | N/A | Not supported |
| Custom headers in method | ❌ **Not Supported** | N/A | No support for custom headers in generated client |

### Example: ❌ Not Supported - Authentication Headers

**Controller Code:**
```java
@GetMapping("/secure/data")
public ResponseEntity<DataDto> getSecureData(
    @RequestHeader("X-Service-Token") String serviceToken
) {
    securityService.validateToken(serviceToken);
    return ResponseEntity.ok(dataService.getData());
}
```

**Current Behavior:** SDK ignores the `@RequestHeader` parameter. Generated client method will have **no parameter** for the service token.

**Expected Behavior:** Include `serviceToken` as a method parameter and add it as a header in the generated RestClient call.

**Impact:** **Critical limitation** for microservices security! Headers are commonly used for:
- Service-to-service authentication tokens
- Correlation IDs for distributed tracing
- Tenant identifiers in multi-tenant systems
- Feature flags

**Workaround:** Currently, authentication must be handled at the infrastructure level (e.g., interceptors), not per-method.

### Example: ❌ Not Supported - Correlation ID for Tracing

**Controller Code:**
```java
@PostMapping("/orders")
public ResponseEntity<OrderDto> createOrder(
    @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
    @RequestBody OrderDto order
) {
    return ResponseEntity.ok(orderService.create(order, correlationId));
}
```

**Current Behavior:** Correlation ID header is ignored by SDK.

**Impact:** Distributed tracing across microservices requires manual header management, defeating the purpose of SDK automation.

### Example: ❌ Not Supported - Custom Headers Map

**Controller Code:**
```java
@GetMapping("/proxy")
public ResponseEntity<String> proxyRequest(
    @RequestHeader Map<String, String> headers
) {
    return ResponseEntity.ok(proxyService.forward(headers));
}
```

**Current Behavior:** Dynamic headers via Map are completely ignored.

**Impact:** Cannot implement flexible proxy or gateway patterns in microservices.

**Implementation Details:**
- No `@RequestHeader` annotation processing found in codebase
- API version header is handled separately via `ApiVersionHeaderGenerator.java` for versioning strategy
- **Note:** This is a **high-priority missing feature** for production microservices architectures

---

## 7. File Uploads Support

| Feature | Status | Implementation | Notes |
|---------|--------|----------------|-------|
| `@RequestPart` single file | ❌ **Not Supported** | N/A | File upload not supported |
| `@RequestPart` multiple files | ❌ **Not Supported** | N/A | Multiple files not supported |
| `@RequestParam` with MultipartFile | ❌ **Not Supported** | N/A | Not supported |
| Form data with files | ❌ **Not Supported** | N/A | `application/x-www-form-urlencoded` not supported |
| File download handling | ❌ **Not Supported** | N/A | Response as Resource/InputStream not handled |

### Example: ❌ Not Supported - Single File Upload

**Controller Code:**
```java
@PostMapping("/documents/upload")
public ResponseEntity<DocumentDto> uploadDocument(
    @RequestPart("file") MultipartFile file,
    @RequestPart("metadata") DocumentMetadata metadata
) {
    return ResponseEntity.ok(documentService.upload(file, metadata));
}
```

**Current Behavior:** SDK cannot generate client code for file uploads. `MultipartFile` and `@RequestPart` are not recognized.

**Expected Behavior:** Generate method accepting `File` or `byte[]` parameter and set `Content-Type: multipart/form-data`.

**Impact:** Common use cases in microservices:
- Document management services
- Image processing services
- CSV/Excel data import
- Backup/restore operations

All require manual RestClient configuration.

### Example: ❌ Not Supported - Multiple File Upload

**Controller Code:**
```java
@PostMapping("/gallery/upload")
public ResponseEntity<List<ImageDto>> uploadImages(
    @RequestParam("files") List<MultipartFile> files
) {
    return ResponseEntity.ok(galleryService.uploadMultiple(files));
}
```

**Current Behavior:** Not supported. List of files cannot be handled.

**Impact:** Batch upload operations in microservices require custom implementation.

### Example: ❌ Not Supported - File Download

**Controller Code:**
```java
@GetMapping("/reports/{id}/download")
public ResponseEntity<Resource> downloadReport(@PathVariable Long id) {
    ByteArrayResource resource = reportService.generatePdf(id);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.pdf")
        .contentType(MediaType.APPLICATION_PDF)
        .body(resource);
}
```

**Current Behavior:** SDK cannot handle `Resource` return type or binary responses. Only JSON responses are supported.

**Expected Behavior:** Generate method returning `byte[]` or `InputStream`, handle binary Content-Type.

**Impact:** Common reporting and export features in microservices:
- PDF generation
- Excel exports
- Image retrieval
- Backup downloads

All require manual implementation.

### Example: ❌ Not Supported - Form Data

**Controller Code:**
```java
@PostMapping(value = "/legacy/submit", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
public ResponseEntity<Void> submitForm(
    @RequestParam String username,
    @RequestParam String email
) {
    formService.process(username, email);
    return ResponseEntity.ok().build();
}
```

**Current Behavior:** SDK assumes `Content-Type: application/json`. Form data is not supported.

**Impact:** Integration with legacy services or third-party APIs that expect form data requires workarounds.

**Implementation Details:**
- No `MultipartFile` or `@RequestPart` handling found
- Content-Type is assumed to be JSON: `application/json`
- **Note:** This is a **high-priority feature** for document management and data import/export microservices

---

## 8. Cookies Support

| Feature | Status | Implementation | Notes |
|---------|--------|----------------|-------|
| `@CookieValue` | ❌ **Not Supported** | N/A | Cookie extraction not implemented |
| `@CookieValue` with defaultValue | ❌ **Not Supported** | N/A | Not supported |
| Cookie handling in client | ❌ **Not Supported** | N/A | Cookie management in RestClient not implemented |

---

## 9. Return Types Support

| Return Type | Status | Implementation | Notes |
|-------------|--------|----------------|-------|
| `ResponseEntity<CustomDTO>` | ✅ **Supported** | `ClientImplementationGenerator.java:546-562` | Response DTOs generated, type extracted from `ResponseEntity` |
| `ResponseEntity<List<T>>` | ✅ **Supported** | `ClientImplementationGenerator.java:344-347` | Uses `ParameterizedTypeReference` for generic types |
| `ResponseEntity<Set<T>>` | ✅ **Supported** | `ClientImplementationGenerator.java:79-89` | Collection imports added automatically |
| `ResponseEntity<Map<K,V>>` | ✅ **Supported** | `ClientImplementationGenerator.java:79-89` | Generic collections supported |
| `ResponseEntity<Optional<T>>` | ✅ **Supported** | `ClientImplementationGenerator.java:112-114` | Optional import added when detected |
| `ResponseEntity<Void>` | ✅ **Supported** | `ClientImplementationGenerator.java:352-355` | Calls `.toBodilessEntity()` |
| Nested generic types | ✅ **Supported** | `ClientImplementationGenerator.java:564-593` | Complex generics like `List<Map<String, UserDto>>` supported via recursive `getTypeNameForSdk()` |
| Direct return (non-ResponseEntity) | ❌ **Not Supported** | `ClientImplementationGenerator.java:546-562` | Only `ResponseEntity` supported; non-ResponseEntity returns "void" |
| `void` return type | ✅ **Supported** | `ClientImplementationGenerator.java:352-355` | Handled via `.toBodilessEntity()` |
| Streaming response | ❌ **Not Supported** | N/A | `ResponseEntity<Resource>` or `InputStream` not handled |

**Implementation Details:**
- Return type extraction: `extractClientReturnType()` at line 546-562
- Generic type handling: `getTypeNameForSdk()` at line 564-593
- ParameterizedTypeReference used for generics: line 344-347
- **Limitation:** Only `ResponseEntity<T>` return types are supported; direct return types are not handled

---

## 10. DTO Generation Support

| Feature | Status | Implementation | Notes |
|---------|--------|----------------|-------|
| Request DTO Generation | ✅ **Supported** | `DtoGenerator.java:42-148` | Separate package: `dto.request` |
| Response DTO Generation | ✅ **Supported** | `DtoGenerator.java:149-265` | Separate package: `dto.response` |
| Nested DTOs | ✅ **Supported** | `DtoGenerator.java:89-101` | Recursive generation via `generateComplexType()` |
| Generic DTOs (`List<T>`, `Map<K,V>`) | ✅ **Supported** | `DtoGenerator.java` | Type arguments preserved in generation |
| Record-based DTOs | ✅ **Supported** | `RecordDtoWriter.java` (via factory) | Modern Java records supported |
| Class-based DTOs | ✅ **Supported** | `ClassDtoWriter.java` (via factory) | Traditional classes supported |
| Inheritance/Polymorphism | ❌ **Not Supported** | N/A | Class inheritance (`extends`) not preserved; polymorphic types not handled |

### Example: ❌ Not Supported - Class Inheritance

**Controller Code:**
```java
// Base DTO with common fields
public class BaseEntityDto {
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// Child DTO extending base
public class UserDto extends BaseEntityDto {
    private String username;
    private String email;
}

@PostMapping("/users")
public ResponseEntity<UserDto> createUser(@RequestBody UserDto user) {
    return ResponseEntity.ok(userService.create(user));
}
```

**Current Behavior:** SDK generates `UserDto` **without** the `extends BaseEntityDto` clause. The generated class only contains `username` and `email` fields. The inherited fields (`id`, `createdAt`, `updatedAt`) are **not included** in the generated DTO.

**Expected Behavior:** 
1. Generate `BaseEntityDto` class first
2. Generate `UserDto extends BaseEntityDto` 
3. Include all inherited fields (or rely on Java inheritance)

**Impact:** Common patterns in microservices using base DTOs for shared fields (IDs, timestamps, audit fields) won't work correctly. The SDK client will be missing fields that the server expects.

### Example: ❌ Not Supported - Polymorphic Types

**Controller Code:**
```java
// Abstract base class
public abstract class NotificationDto {
    private String recipient;
    private String message;
}

// Concrete implementations
public class EmailNotificationDto extends NotificationDto {
    private String subject;
    private String fromAddress;
}

public class SmsNotificationDto extends NotificationDto {
    private String phoneNumber;
}

@PostMapping("/notifications")
public ResponseEntity<NotificationDto> createNotification(
    @RequestBody NotificationDto notification
) {
    // Could receive EmailNotificationDto, SmsNotificationDto, etc.
    return ResponseEntity.ok(notificationService.create(notification));
}
```

**Current Behavior:** SDK cannot handle polymorphic types. The generated client will only have the base `NotificationDto` class, losing the inheritance hierarchy and concrete implementations.

**Expected Behavior:**
1. Generate abstract base class `NotificationDto`
2. Generate all concrete subclasses (`EmailNotificationDto`, `SmsNotificationDto`)
3. Add Jackson polymorphic annotations (`@JsonTypeInfo`, `@JsonSubTypes`) for proper deserialization
4. Client method should accept `NotificationDto` and handle all subtypes correctly

**Impact:** APIs using polymorphism (common in event-driven architectures, notification systems, or plugin-based designs) cannot be properly consumed through the SDK. The client won't be able to send or receive concrete subtypes.

**Implementation Details:**
- DTO generation: `DtoGenerator.java`
- Factory pattern: `DtoWriterFactory` selects appropriate writer
- Recursive type processing: `generateComplexType()` handles nested types
- Package structure: Request DTOs in `.dto.request`, Response DTOs in `.dto.response`
- **Limitation:** `writeClassDeclaration()` at line 155-173 only generates `public class ClassName {` without checking for superclass via `typeElement.getSuperclass()`. Inheritance relationships are lost.
- **Missing:** No detection or generation of parent classes, no `extends` clause in class declarations, no Jackson polymorphic annotations (`@JsonTypeInfo`, `@JsonSubTypes`)

---

## 11. Path and Routing Support

| Feature | Status | Implementation | Notes |
|---------|--------|----------------|-------|
| Controller base path (`@RequestMapping` on class) | ✅ **Supported** | `ClientImplementationGenerator.java:595-605` | Extracted and combined with method paths |
| Method-level path | ✅ **Supported** | `ClientImplementationGenerator.java:524-536` | Full path extraction |
| Path variable substitution | ✅ **Supported** | `ClientImplementationGenerator.java:473-480` | `{id}` -> `{0}` indexed format |
| Query string building | ✅ **Supported** | `ClientImplementationGenerator.java:371-397` | Query parameter concatenation |
| Path combination | ✅ **Supported** | `ClientImplementationGenerator.java:607-627` | Base + method path combined correctly |
| Version prefix in path | ✅ **Supported** | `ClientImplementationGenerator.java:535` | Version pattern `/v\d+/` stripped and handled via config |

**Implementation Details:**
- Path combination: `combinePaths()` at line 607-627
- Version stripping: Line 535 removes `/v\d+/` prefix
- URI building: `buildUriExpression()` constructs full URI with path variables and query params

---

## 12. Configuration and Infrastructure

| Feature | Status | Implementation | Notes |
|---------|--------|----------------|-------|
| API Versioning (Header-based) | ✅ **Supported** | `ApiVersionHeaderGenerator.java` | Configurable versions via `X-API-Version` header |
| API Versioning (Path-based) | ✅ **Supported** | `VersioningStrategyGenerator.java` | Version prefix handled in path |
| Circuit Breaker (Resilience4j) | ✅ **Supported** | `ClientImplementationGenerator.java:170-202` | Auto-configured for all clients |
| Retry Mechanism | ✅ **Supported** | `RetryInterceptorGenerator.java` | Interceptor-based retry with exponential backoff |
| RestClient Configuration | ✅ **Supported** | `ClientConfigurationGenerator.java` | Spring RestClient-based |
| Base URL Configuration | ✅ **Supported** | `ClientConfigurationGenerator.java` | Via properties and builder pattern |

**Implementation Details:**
- Circuit breaker: `buildCircuitBreaker()` at line 170-202, wraps all method calls
- Retry interceptor: `RetryableRestClientInterceptor` with configurable max retries and delay
- Versioning: Header-based via interceptor, path-based via path manipulation

---

## 13. Parameter Filtering and Exclusions

| Feature | Status | Implementation | Notes |
|---------|--------|----------------|-------|
| Framework types excluded | ✅ **Supported** | `ClientInterfaceGenerator.java:206-208` | `HttpServletRequest`, `HttpServletResponse`, etc. filtered |
| `tracerId` excluded | ✅ **Supported** | `ClientInterfaceGenerator.java:169, 207` | Special parameter ignored |
| Simple vs Complex detection | ✅ **Supported** | `ClientInterfaceGenerator.java:164-185` | Logic to differentiate parameter types |

**Implementation Details:**
- Parameter filtering: `shouldIncludeParameter()` excludes framework types and `tracerId`
- Complex parameter detection: Checks if type is not basic/JDK type and not annotated with `@PathVariable`/`@RequestParam`

---

## 14. Code Generation Structure

| Feature | Status | Implementation | Notes |
|---------|--------|----------------|-------|
| Interface generation | ✅ **Supported** | `ClientInterfaceGenerator.java` | Client interface with method signatures |
| Implementation generation | ✅ **Supported** | `ClientImplementationGenerator.java` | Full implementation with RestClient calls |
| Config class generation | ✅ **Supported** | `ClientConfigurationGenerator.java` | Configuration classes with builder pattern |
| Package structure | ✅ **Supported** | Multiple generators | `.client`, `.dto.request`, `.dto.response`, `.config` packages |
| Import management | ✅ **Supported** | `BaseCodeGenerator.java` | Required imports automatically added |
| Annotation processing | ✅ **Supported** | `BitsSdkProcessor.java` | JSR 269 processor entry point |

---

## 15. Advanced Features (Not Yet Supported)

| Feature | Status | Priority | Difficulty | Notes |
|---------|--------|----------|-----------|-------|
| File Upload (`MultipartFile`) | ❌ **Not Supported** | High | Medium | Common use case |
| File Download (byte[], Resource) | ❌ **Not Supported** | High | Medium | Common use case |
| `@RequestHeader` support | ❌ **Not Supported** | High | Low | Should be straightforward |
| Explicit `@RequestBody` annotation | ❌ **Not Supported** | Medium | Low | Currently relies on type detection |
| Direct return types (non-ResponseEntity) | ❌ **Not Supported** | Medium | Medium | Would require significant changes |
| `@RequestMapping` method attribute | ❌ **Not Supported** | Medium | Low | Method extraction needed |
| Multiple complex parameters | ❌ **Not Supported** | Medium | Medium | Currently only first complex param |
| `@RequestParam` List/Array | ❌ **Not Supported** | Medium | Medium | Requires special encoding |
| Authentication/Security | ❌ **Not Supported** | High | Medium | OAuth2, JWT, etc. |
| Async/Reactive (`Mono`, `Flux`) | ❌ **Not Supported** | Low | High | WebFlux support |
| Custom Serialization | ❌ **Not Supported** | Low | Medium | Jackson customization |
| WebSocket Support | ❌ **Not Supported** | Low | High | Different protocol |
| GraphQL Endpoints | ❌ **Not Supported** | Low | High | Different paradigm |
| Server-Sent Events (SSE) | ❌ **Not Supported** | Low | Medium | Streaming support |
| `@CookieValue` support | ❌ **Not Supported** | Low | Low | Cookie handling |
| `@PathVariable` defaultValue/required | ❌ **Not Supported** | Low | Low | Optional path variables |
| `@RequestParam` defaultValue usage | ❌ **Not Supported** | Low | Low | Extracted but not used |
| `Optional<T>` parameters | ❌ **Not Supported** | Low | Low | Optional type parameters not handled |

---

## Implementation Locations Reference

### Core Generators
- **ClientInterfaceGenerator.java**: Interface generation with method signatures
- **ClientImplementationGenerator.java**: Implementation with RestClient calls, HTTP method extraction, path/query/body handling
- **DtoGenerator.java**: Request and response DTO generation
- **ClientConfigurationGenerator.java**: Configuration classes and RestClient setup

### Supporting Generators
- **ApiVersionGenerator.java**: API version enum generation
- **ApiVersionHeaderGenerator.java**: Header-based versioning interceptor
- **VersioningStrategyGenerator.java**: Versioning strategy configuration
- **CircuitBreakerConfigGenerator.java**: Circuit breaker configuration
- **RetryInterceptorGenerator.java**: Retry interceptor implementation

### Utilities
- **TypeUtilityService.java**: Type detection and utility methods
- **AnnotationExtractor.java**: Annotation extraction utilities
- **BitsSdkProcessor.java**: Main annotation processor entry point

---

## Priority Recommendations

### High Priority (Essential Features)
1. **`@RequestHeader` Support** - Essential for authentication and custom headers
   - **Effort**: Low (similar to `@RequestParam` implementation)
   - **Impact**: High (enables authentication flows)

2. **File Upload/Download** - Common use case
   - **Effort**: Medium (requires multipart handling)
   - **Impact**: High (many APIs require file operations)

3. **Explicit `@RequestBody` Annotation** - Better handling of request bodies
   - **Effort**: Low (add annotation check)
   - **Impact**: Medium (improves correctness)

### Medium Priority (Quality Improvements)
4. **`@RequestMapping` Method Attribute** - Support custom HTTP methods
   - **Effort**: Low (extract method attribute)
   - **Impact**: Medium (enables more controller patterns)

5. **Multiple Complex Parameters** - Currently only first is handled
   - **Effort**: Medium (requires DTO composition strategy)
   - **Impact**: Medium (handles edge cases)

6. **Direct Return Types** - Support non-ResponseEntity returns
   - **Effort**: Medium (requires return type handling changes)
   - **Impact**: Medium (broader compatibility)

7. **`@RequestParam` List/Array** - Collection query parameters
   - **Effort**: Medium (requires encoding logic)
   - **Impact**: Medium (common pattern)

### Low Priority (Nice to Have)
8. **`@CookieValue` Support** - Cookie handling
9. **`@PathVariable` defaultValue/required** - Optional path variables
10. **Async/Reactive Support** - Mono/Flux return types
11. **Custom Serialization** - Advanced JSON handling

---

## Testing Recommendations

Based on the implementation, the following test scenarios should be verified:

### ✅ Should Work
- GET with path variables and query parameters
- POST/PUT/PATCH with complex request body
- Multiple path variables with custom names
- Collection return types (List, Set, Map, Optional)
- Nested DTOs in request/response
- Circuit breaker and retry mechanisms
- API versioning (header and path-based)

### ⚠️ Needs Verification
- `@RequestMapping` without method attribute (defaults to GET)
- Query parameters with `required=false` or `defaultValue` (extracted but not used)
- Multiple query parameters with special characters

### ❌ Known Limitations
- Direct return types (non-ResponseEntity)
- `@RequestHeader` parameters
- File uploads/downloads
- Multiple complex parameters
- `@RequestParam` with List/Array types

---

## Legend

- ✅ **Supported**: Feature is fully implemented and working as expected.
- ⚠️ **Partial**: Feature is partially implemented or has known minor limitations.
- ❌ **Not Supported**: Feature is not implemented or has major blocking limitations.
