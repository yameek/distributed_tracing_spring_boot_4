# Bits API SDK Generator - Controller Support Matrix

This document details the REST Controller features supported by the **Bits API SDK Generator** (BitsApiSDKGenerator), with practical examples for developers.

> For onboarding instructions, helper scripts, and Gradle examples, visit the [Developer Guide](docs/DEVELOPER_GUIDE.md). Use this matrix when validating controller compatibility.

---

## ✅ Supported Features

### 1. HTTP Method Annotations

| Annotation | Status | Implementation | Notes |
|------------|--------|----------------|-------|
| `@GetMapping` | ✅ **Supported** | `ClientImplementationGenerator.java:586-587` | Fully implemented with query params and path variables |
| `@PostMapping` | ✅ **Supported** | `ClientImplementationGenerator.java:583-584` | Includes request body support for complex types |
| `@PutMapping` | ✅ **Supported** | `ClientImplementationGenerator.java:589-590` | Includes request body support |
| `@DeleteMapping` | ✅ **Supported** | `ClientImplementationGenerator.java:592-593` | Supports path variables |
| `@PatchMapping` | ✅ **Supported** | `ClientImplementationGenerator.java:595-596` | Includes request body support |
| `@RequestMapping` | ⚠️ **Partial** | `ClientImplementationGenerator.java:580-600` | Path extraction works, but `method` attribute not extracted. Defaults to GET |

#### Example: ✅ Supported - `@GetMapping` with Path Variable and Query Parameters

**Controller Code:**
```java
@RestController
@BitsSdk(sdkPackage = "com.example.sdk.client")
public class UserController {
    
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserDto> getUser(
        @PathVariable Long userId,
        @RequestParam(required = false) Boolean includeDetails
    ) {
        return ResponseEntity.ok(userService.getUser(userId, includeDetails));
    }
}
```

**Generated Client Interface:**
```java
public interface UserClient {
    UserDto getUser(Long userId, Boolean includeDetails);
}
```

**Generated Client Implementation:**
```java
public UserDto getUser(Long userId, Boolean includeDetails) {
    return circuitBreaker.executeSupplier(() ->
        restClient.get()
            .uri("/users/" + userId + (includeDetails != null ? "?includeDetails=" + includeDetails : ""))
            .retrieve()
            .body(UserDto.class)
    );
}
```

**Usage:**
```java
@Autowired
private UserClient userClient;

public void example() {
    UserDto user = userClient.getUser(123L, true);
}
```

#### Example: ✅ Supported - `@PostMapping` with Request Body

**Controller Code:**
```java
@PostMapping("/users")
public ResponseEntity<UserDto> createUser(@RequestBody CreateUserRequest request) {
    return ResponseEntity.ok(userService.createUser(request));
}
```

**Generated Client Interface:**
```java
public interface UserClient {
    UserDto createUser(CreateUserRequest request);
}
```

**Generated Client Implementation:**
```java
public UserDto createUser(CreateUserRequest request) {
    return circuitBreaker.executeSupplier(() ->
        restClient.post()
            .uri("/users")
            .body(request)
            .retrieve()
            .body(UserDto.class)
    );
}
```

**Usage:**
```java
CreateUserRequest request = new CreateUserRequest();
request.setUsername("john_doe");
request.setEmail("john@example.com");
UserDto user = userClient.createUser(request);
```

#### Example: ⚠️ Partial Support - `@RequestMapping` without method attribute

**Controller Code:**
```java
@RequestMapping(value = "/users/{id}")
public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
    return ResponseEntity.ok(userService.getUser(id));
}
```

**Current Behavior:** SDK generates this as a **GET** request (default), even if you intended POST/PUT/DELETE.

**Workaround:** Use specific mapping annotations (`@GetMapping`, `@PostMapping`, etc.) instead.

---

### 2. Path Variables

| Feature | Status | Implementation | Notes |
|---------|--------|----------------|-------|
| Single `@PathVariable` | ✅ **Supported** | `ClientImplementationGenerator.java:447-451, 561-578` | Extracted and mapped to indexed placeholders `{0}`, `{1}` |
| Multiple `@PathVariable` | ✅ **Supported** | `ClientImplementationGenerator.java:447-451` | Supports multiple path variables, converted to indexed format |
| `@PathVariable` with custom name | ✅ **Supported** | `ClientImplementationGenerator.java:561-578` | `@PathVariable("customName")` handled via `extractPathVariableInfo()` |
| `@PathVariable` with defaultValue | ❌ **Not Supported** | N/A | Spring supports this but not extracted in `extractPathVariableInfo()` |
| `@PathVariable` with required flag | ❌ **Not Supported** | N/A | Optional path variables not handled |

#### Example: ✅ Supported - Multiple Path Variables

**Controller Code:**
```java
@GetMapping("/reports/{year}/{month}")
public ResponseEntity<ReportDto> getReport(
    @PathVariable int year,
    @PathVariable int month
) {
    return ResponseEntity.ok(reportService.getReport(year, month));
}
```

**Generated Client Interface:**
```java
public interface ReportClient {
    ReportDto getReport(int year, int month);
}
```

**Generated Client Implementation:**
```java
public ReportDto getReport(int year, int month) {
    return circuitBreaker.executeSupplier(() ->
        restClient.get()
            .uri("/reports/" + year + "/" + month)
            .retrieve()
            .body(ReportDto.class)
    );
}
```

**Usage:**
```java
ReportDto report = reportClient.getReport(2024, 3);
```

#### Example: ✅ Supported - Path Variable with Custom Name

**Controller Code:**
```java
@GetMapping("/users/{userId}/orders/{orderId}")
public ResponseEntity<OrderDto> getOrder(
    @PathVariable("userId") Long user,
    @PathVariable("orderId") Long order
) {
    return ResponseEntity.ok(orderService.getOrder(user, order));
}
```

**Generated Client Interface:**
```java
public interface OrderClient {
    OrderDto getOrder(Long user, Long order);
}
```

**Note:** The custom names in `@PathVariable` are used for path matching but don't affect the generated method parameter names.

---

### 3. Query Parameters

| Feature | Status | Implementation | Notes |
|---------|--------|----------------|-------|
| Single `@RequestParam` | ✅ **Supported** | `ClientImplementationGenerator.java:418-422` | Basic query parameter support |
| Multiple `@RequestParam` | ✅ **Supported** | `ClientImplementationGenerator.java:418-422` | Multiple query parameters supported |
| `@RequestParam` with name | ✅ **Supported** | `ClientImplementationGenerator.java:493-495` | Custom query parameter names via `value` or `name` attribute |
| `@RequestParam` required flag | ⚠️ **Partial** | `ClientImplementationGenerator.java:498-500` | `required` attribute extracted but not used in generated code |
| `@RequestParam` defaultValue | ⚠️ **Partial** | `ClientImplementationGenerator.java:502-506` | Default value extracted but not used in generated code |
| `@RequestParam` with List/Array | ❌ **Not Supported** | N/A | Array/Collection query params not handled |

#### Example: ✅ Supported - Multiple Query Parameters

**Controller Code:**
```java
@GetMapping("/products")
public ResponseEntity<List<ProductDto>> searchProducts(
    @RequestParam String category,
    @RequestParam int page,
    @RequestParam int size
) {
    return ResponseEntity.ok(productService.search(category, page, size));
}
```

**Generated Client Interface:**
```java
public interface ProductClient {
    List<ProductDto> searchProducts(String category, int page, int size);
}
```

**Generated Client Implementation:**
```java
public List<ProductDto> searchProducts(String category, int page, int size) {
    return circuitBreaker.executeSupplier(() ->
        restClient.get()
            .uri("/products?category=" + category + "&page=" + page + "&size=" + size)
            .retrieve()
            .body(new ParameterizedTypeReference<List<ProductDto>>() {})
    );
}
```

**Usage:**
```java
List<ProductDto> products = productClient.searchProducts("electronics", 0, 20);
```

#### Example: ⚠️ Partial Support - `@RequestParam` with defaultValue

**Controller Code:**
```java
@GetMapping("/items")
public ResponseEntity<List<ItemDto>> getItems(
    @RequestParam(defaultValue = "10") int pageSize,
    @RequestParam(defaultValue = "0") int page
) {
    return ResponseEntity.ok(itemService.getItems(page, pageSize));
}
```

**Current Behavior:** SDK generates a method with **mandatory** `pageSize` and `page` parameters. The default values are extracted but ignored, so calling services must always provide these values.

**Workaround:** Always pass values from calling service, or create overloaded methods:
```java
// In your service
public List<ItemDto> getItems(Integer pageSize, Integer page) {
    return itemClient.getItems(
        pageSize != null ? pageSize : 10,
        page != null ? page : 0
    );
}
```

---

### 4. Request Body

| Feature | Status | Implementation | Notes |
|---------|--------|----------------|-------|
| Implicit `@RequestBody` (complex type) | ✅ **Supported** | `ClientImplementationGenerator.java:298-303, 379-385` | Automatically detected for complex types in POST/PUT/PATCH methods |
| Explicit `@RequestBody` annotation | ⚠️ **Partial** | N/A | Annotation not checked; relies on type detection only |
| Multiple request bodies | ❌ **Not Supported** | `ClientImplementationGenerator.java:298-303` | Only first complex parameter handled via `getFirstComplexParameter()` |

#### Example: ✅ Supported - Request Body with Complex DTO

**Controller Code:**
```java
@PostMapping("/orders")
public ResponseEntity<OrderDto> createOrder(@RequestBody CreateOrderRequest request) {
    return ResponseEntity.ok(orderService.createOrder(request));
}

// DTO with nested objects
public class CreateOrderRequest {
    private Long customerId;
    private List<OrderItemDto> items;
    private AddressDto shippingAddress;
    // getters/setters
}
```

**Generated Client Interface:**
```java
public interface OrderClient {
    OrderDto createOrder(CreateOrderRequest request);
}
```

**Generated Client Implementation:**
```java
public OrderDto createOrder(CreateOrderRequest request) {
    return circuitBreaker.executeSupplier(() ->
        restClient.post()
            .uri("/orders")
            .body(request)
            .retrieve()
            .body(OrderDto.class)
    );
}
```

**Generated DTOs:**
The SDK automatically generates `CreateOrderRequest`, `OrderItemDto`, `AddressDto`, and `OrderDto` in the `dto.request` and `dto.response` packages.

**Usage:**
```java
CreateOrderRequest request = new CreateOrderRequest();
request.setCustomerId(123L);
request.setItems(Arrays.asList(item1, item2));
request.setShippingAddress(address);
OrderDto order = orderClient.createOrder(request);
```

#### Example: ❌ Not Supported - Multiple Request Bodies

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

**Workaround:** Create a wrapper DTO:
```java
public class MergeUsersRequest {
    private UserDto primaryUser;
    private UserDto secondaryUser;
}

@PostMapping("/merge")
public ResponseEntity<UserDto> mergeUsers(@RequestBody MergeUsersRequest request) {
    return ResponseEntity.ok(userService.merge(
        request.getPrimaryUser(), 
        request.getSecondaryUser()
    ));
}
```

---

### 5. Request Headers

| Feature | Status | Implementation | Notes |
|---------|--------|----------------|-------|
| `@RequestHeader` | ✅ **Supported** | `ClientImplementationGenerator.java:518-550` | Header extraction and mapping to method parameters |
| Multiple `@RequestHeader` | ✅ **Supported** | `ClientImplementationGenerator.java:368-377` | Multiple headers supported |
| `@RequestHeader` with custom name | ✅ **Supported** | `ClientImplementationGenerator.java:530-533` | Custom header names via `value` or `name` attribute |
| `@RequestHeader` required flag | ⚠️ **Partial** | `ClientImplementationGenerator.java:535-537` | `required` attribute extracted but not enforced in generated code |
| `@RequestHeader` defaultValue | ⚠️ **Partial** | `ClientImplementationGenerator.java:538-542` | Default value extracted but not used in generated code |
| Header name normalization | ✅ **Supported** | `TypeUtilityService.java` | Header names normalized (e.g., `X-API-Version` -> `apiVersionValue`) |

#### Example: ✅ Supported - Request Header with Custom Name

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

**Generated Client Interface:**
```java
public interface DataClient {
    DataDto getSecureData(String serviceToken);
}
```

**Generated Client Implementation:**
```java
public DataDto getSecureData(String serviceToken) {
    return circuitBreaker.executeSupplier(() ->
        restClient.get()
            .uri("/secure/data")
            .header("X-Service-Token", serviceToken)
            .retrieve()
            .body(DataDto.class)
    );
}
```

**Usage:**
```java
String token = tokenProvider.getToken();
DataDto data = dataClient.getSecureData(token);
```

#### Example: ✅ Supported - Multiple Request Headers

**Controller Code:**
```java
@GetMapping("/api/data")
public ResponseEntity<DataDto> getData(
    @RequestHeader("X-Service-Token") String serviceToken,
    @RequestHeader("X-Correlation-ID") String correlationId,
    @RequestHeader(value = "X-API-Version", required = false) String apiVersion
) {
    return ResponseEntity.ok(dataService.getData(serviceToken, correlationId, apiVersion));
}
```

**Generated Client Interface:**
```java
public interface DataClient {
    DataDto getData(String serviceToken, String correlationId, String apiVersion);
}
```

**Generated Client Implementation:**
```java
public DataDto getData(String serviceToken, String correlationId, String apiVersion) {
    return circuitBreaker.executeSupplier(() ->
        restClient.get()
            .uri("/api/data")
            .header("X-Service-Token", serviceToken)
            .header("X-Correlation-ID", correlationId)
            .header("X-API-Version", apiVersion)
            .retrieve()
            .body(DataDto.class)
    );
}
```

**Usage:**
```java
DataDto data = dataClient.getData(
    tokenProvider.getToken(),
    UUID.randomUUID().toString(),
    "v1"
);
```

#### Example: ✅ Supported - Header Name Normalization

**Controller Code:**
```java
@GetMapping("/api/versioned")
public ResponseEntity<VersionedDto> getVersioned(
    @RequestHeader("X-API-Version") String apiVersion
) {
    return ResponseEntity.ok(service.getVersioned(apiVersion));
}
```

**Generated Client Interface:**
```java
public interface ApiClient {
    VersionedDto getVersioned(String apiVersionValue);  // Header name normalized
}
```

**Note:** Header names like `X-API-Version` are normalized to camelCase parameter names (e.g., `apiVersionValue`) for better Java naming conventions, but the actual header name `X-API-Version` is still used in the HTTP request.

---

### 6. Return Types

| Return Type | Status | Implementation | Notes |
|-------------|--------|----------------|-------|
| `ResponseEntity<CustomDTO>` | ✅ **Supported** | `ClientImplementationGenerator.java:624-640` | Response DTOs generated, type extracted from `ResponseEntity` |
| `ResponseEntity<List<T>>` | ✅ **Supported** | `ClientImplementationGenerator.java:391-394` | Uses `ParameterizedTypeReference` for generic types |
| `ResponseEntity<Map<K,V>>` | ✅ **Supported** | `ClientImplementationGenerator.java:642-671` | Generic collections supported via recursive type processing |
| `ResponseEntity<Optional<T>>` | ✅ **Supported** | `ClientImplementationGenerator.java:642-671` | Optional types supported in generic processing |
| `ResponseEntity<Void>` | ✅ **Supported** | `ClientImplementationGenerator.java:399-401` | Calls `.toBodilessEntity()` |
| `void` return type | ✅ **Supported** | `ClientImplementationGenerator.java:399-401` | Handled via `.toBodilessEntity()` |
| Direct return (non-ResponseEntity) | ❌ **Not Supported** | `ClientImplementationGenerator.java:624-640` | Only `ResponseEntity` supported; non-ResponseEntity returns "void" |

#### Example: ✅ Supported - Collection Return Types

**Controller Code:**
```java
@GetMapping("/users")
public ResponseEntity<List<UserDto>> getAllUsers() {
    return ResponseEntity.ok(userService.findAll());
}

@GetMapping("/users/by-role")
public ResponseEntity<Map<String, List<UserDto>>> getUsersByRole() {
    return ResponseEntity.ok(userService.groupByRole());
}
```

**Generated Client Interface:**
```java
public interface UserClient {
    List<UserDto> getAllUsers();
    Map<String, List<UserDto>> getUsersByRole();
}
```

**Generated Client Implementation:**
```java
public List<UserDto> getAllUsers() {
    return circuitBreaker.executeSupplier(() ->
        restClient.get()
            .uri("/users")
            .retrieve()
            .body(new ParameterizedTypeReference<List<UserDto>>() {})
    );
}

public Map<String, List<UserDto>> getUsersByRole() {
    return circuitBreaker.executeSupplier(() ->
        restClient.get()
            .uri("/users/by-role")
            .retrieve()
            .body(new ParameterizedTypeReference<Map<String, List<UserDto>>>() {})
    );
}
```

**Usage:**
```java
List<UserDto> users = userClient.getAllUsers();
Map<String, List<UserDto>> usersByRole = userClient.getUsersByRole();
```

#### Example: ✅ Supported - Void Return Type

**Controller Code:**
```java
@DeleteMapping("/users/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.delete(id);
    return ResponseEntity.ok().build();
}
```

**Generated Client Interface:**
```java
public interface UserClient {
    void deleteUser(Long id);
}
```

**Generated Client Implementation:**
```java
public void deleteUser(Long id) {
    circuitBreaker.executeSupplier(() ->
        restClient.delete()
            .uri("/users/" + id)
            .retrieve()
            .toBodilessEntity()
    );
}
```

**Usage:**
```java
userClient.deleteUser(123L);
```

#### Example: ❌ Not Supported - Direct Return (Non-ResponseEntity)

**Controller Code:**
```java
@GetMapping("/users/{id}")
public UserDto getUser(@PathVariable Long id) {
    return userService.getUser(id);
}
```

**Current Behavior:** SDK generates a method with **void** return type. The DTO return is ignored.

**Workaround:** Always wrap return types in `ResponseEntity`:
```java
@GetMapping("/users/{id}")
public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
    return ResponseEntity.ok(userService.getUser(id));
}
```

---

### 7. DTO Generation

| Feature | Status | Implementation | Notes |
|---------|--------|----------------|-------|
| Request DTO Generation | ✅ **Supported** | `DtoGenerator.java:42-148` | Separate package: `dto.request` |
| Response DTO Generation | ✅ **Supported** | `DtoGenerator.java:149-265` | Separate package: `dto.response` |
| Nested DTOs | ✅ **Supported** | `DtoGenerator.java:89-101` | Recursive generation via `generateComplexType()` |
| Generic DTOs (`List<T>`, `Map<K,V>`) | ✅ **Supported** | `DtoGenerator.java` | Type arguments preserved in generation |
| Record-based DTOs | ✅ **Supported** | `RecordDtoWriter.java` | Modern Java records supported |
| Class-based DTOs | ✅ **Supported** | `ClassDtoWriter.java` | Traditional classes supported |

#### Example: ✅ Supported - Nested DTOs

**Controller Code:**
```java
@PostMapping("/orders")
public ResponseEntity<OrderDto> createOrder(@RequestBody CreateOrderRequest request) {
    return ResponseEntity.ok(orderService.createOrder(request));
}

// Server-side DTOs
public class CreateOrderRequest {
    private Long customerId;
    private List<OrderItemDto> items;
    private AddressDto shippingAddress;
}

public class OrderItemDto {
    private Long productId;
    private Integer quantity;
    private BigDecimal price;
}

public class AddressDto {
    private String street;
    private String city;
    private String zipCode;
}
```

**Generated DTOs:**
The SDK automatically generates all DTOs in the `dto.request` and `dto.response` packages:

```java
// Generated: dto.request.CreateOrderRequest
public class CreateOrderRequest {
    private Long customerId;
    private List<OrderItemDto> items;
    private AddressDto shippingAddress;
    // getters, setters, builder methods
}

// Generated: dto.request.OrderItemDto
public class OrderItemDto {
    private Long productId;
    private Integer quantity;
    private BigDecimal price;
    // getters, setters, builder methods
}

// Generated: dto.request.AddressDto
public class AddressDto {
    private String street;
    private String city;
    private String zipCode;
    // getters, setters, builder methods
}
```

**Usage:**
```java
CreateOrderRequest request = CreateOrderRequest.builder()
    .customerId(123L)
    .items(Arrays.asList(
        OrderItemDto.builder()
            .productId(456L)
            .quantity(2)
            .price(new BigDecimal("99.99"))
            .build()
    ))
    .shippingAddress(AddressDto.builder()
        .street("123 Main St")
        .city("Springfield")
        .zipCode("12345")
        .build())
    .build();

OrderDto order = orderClient.createOrder(request);
```

---

## ❌ Not Supported / Limitations

### 1. Return Types

#### Example: ❌ Not Supported - Reactive Types

**Controller Code:**
```java
@GetMapping("/users/{id}")
public Mono<ResponseEntity<UserDto>> getUser(@PathVariable Long id) {
    return userService.getUser(id)
        .map(user -> ResponseEntity.ok(user));
}
```

**Current Behavior:** SDK cannot handle `Mono<T>` or `Flux<T>`. These will be treated as `void` or raw types.

**Impact:** WebFlux-based reactive controllers cannot be used with the SDK.

**Workaround:** Use blocking controllers with `ResponseEntity<T>` instead.

---

### 2. Parameters

#### Example: ❌ Not Supported - `@RequestParam` with List/Array

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

**Workaround:** Accept comma-separated string and parse on server side:
```java
@GetMapping("/users")
public ResponseEntity<List<UserDto>> getUsersByIds(
    @RequestParam String ids  // "1,2,3,4"
) {
    List<Long> idList = Arrays.stream(ids.split(","))
        .map(Long::parseLong)
        .collect(Collectors.toList());
    return ResponseEntity.ok(userService.findByIds(idList));
}
```

**Alternative:** Use POST with request body:
```java
@PostMapping("/users/search")
public ResponseEntity<List<UserDto>> searchUsers(@RequestBody UserSearchRequest request);

public class UserSearchRequest {
    private List<Long> ids;
}
```

---

#### Example: ❌ Not Supported - File Uploads

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

**Impact:** Common use cases in microservices:
- Document management services
- Image processing services
- CSV/Excel data import
- Backup/restore operations

All require manual RestClient configuration.

**Workaround:** Use object storage (S3, MinIO) and pass file URLs/keys in DTOs:
```java
@PostMapping("/documents")
public ResponseEntity<DocumentDto> createDocument(@RequestBody DocumentDto doc);

public class DocumentDto {
    private String fileUrl;  // Pre-uploaded to S3
    private String fileKey;   // S3 object key
}
```

---

#### Example: ❌ Not Supported - `@CookieValue`

**Controller Code:**
```java
@GetMapping("/preferences")
public ResponseEntity<PreferencesDto> getPreferences(
    @CookieValue("sessionId") String sessionId
) {
    return ResponseEntity.ok(preferenceService.getPreferences(sessionId));
}
```

**Current Behavior:** SDK ignores `@CookieValue` parameters. Generated client method will have **no parameter** for the cookie.

**Impact:** Cookie-based authentication or session management cannot be used with the SDK.

**Workaround:** Use headers instead:
```java
@GetMapping("/preferences")
public ResponseEntity<PreferencesDto> getPreferences(
    @RequestHeader("X-Session-Id") String sessionId
) {
    return ResponseEntity.ok(preferenceService.getPreferences(sessionId));
}
```

---

### 3. Excluded Types

The following types are explicitly excluded from generation and will be ignored if present in controller methods:

- `HttpServletRequest`
- `HttpServletResponse`
- `HttpSession`
- `WebRequest`
- `Locale`
- `InputStream`
- `OutputStream`
- `Principal`
- Parameters named `tracerId`

**Example:**
```java
@GetMapping("/data")
public ResponseEntity<DataDto> getData(
    @RequestParam String query,
    HttpServletRequest request,  // Ignored by SDK
    String tracerId              // Ignored by SDK
) {
    return ResponseEntity.ok(dataService.getData(query));
}
```

**Generated Client Interface:**
```java
public interface DataClient {
    DataDto getData(String query);  // Only query parameter included
}
```

---

## ⚠️ Best Practices for SDK Compatibility

1. **Always use `ResponseEntity<T>`**: Ensure all controller methods return `ResponseEntity` with a specific generic type.
   ```java
   // ✅ Good
   public ResponseEntity<UserDto> getUser(@PathVariable Long id)
   
   // ❌ Bad
   public UserDto getUser(@PathVariable Long id)
   ```

2. **Explicit Annotations**: Always annotate path variables and request params explicitly.
   ```java
   // ✅ Good
   public ResponseEntity<UserDto> getUser(
       @PathVariable Long id,
       @RequestParam String filter
   )
   
   // ❌ Bad (may not work correctly)
   public ResponseEntity<UserDto> getUser(Long id, String filter)
   ```

3. **Single Body**: Use a single DTO for the request body. If you need multiple objects, wrap them in a container DTO.
   ```java
   // ✅ Good
   public ResponseEntity<UserDto> mergeUsers(@RequestBody MergeUsersRequest request)
   
   // ❌ Bad
   public ResponseEntity<UserDto> mergeUsers(
       @RequestBody UserDto user1,
       @RequestBody UserDto user2
   )
   ```

4. **Avoid Framework Types**: Do not pass `HttpServletRequest` or similar types to controller methods if you want them exposed in the SDK (though they are safely ignored, they won't be available in the client).

5. **Use Specific HTTP Method Annotations**: Prefer `@GetMapping`, `@PostMapping`, etc. over `@RequestMapping`:
   ```java
   // ✅ Good
   @PostMapping("/users")
   
   // ⚠️ Partial (defaults to GET)
   @RequestMapping(value = "/users", method = RequestMethod.POST)
   ```

---

## Implementation Details Reference

### Core Generators
- **ClientInterfaceGenerator.java**: Interface generation with method signatures
- **ClientImplementationGenerator.java**: Implementation with RestClient calls, HTTP method extraction, path/query/body handling
- **DtoGenerator.java**: Request and response DTO generation
- **ClientConfigurationGenerator.java**: Configuration classes and RestClient setup

### Key Methods
- `extractHttpMethod()`: HTTP method extraction from annotations
- `extractPath()`: Path extraction from `@RequestMapping` annotations
- `extractPathVariableInfo()`: Path variable extraction and name mapping
- `extractQueryParamInfo()`: Query parameter extraction
- `extractHeaderInfo()`: Request header extraction
- `getFirstComplexParameter()`: Request body detection
- `extractClientReturnType()`: Return type extraction from `ResponseEntity`

---

## Quick Reference

| Feature | Status | Example |
|---------|--------|---------|
| GET with path variable | ✅ | `@GetMapping("/users/{id}")` |
| POST with request body | ✅ | `@PostMapping("/users")` with `@RequestBody` |
| Multiple query params | ✅ | Multiple `@RequestParam` |
| Collection return types | ✅ | `ResponseEntity<List<T>>` |
| Request headers | ✅ | `@RequestHeader("X-Token")` |
| File uploads | ❌ | Use object storage instead |
| Reactive types | ❌ | Use blocking controllers |
| Multiple request bodies | ❌ | Use wrapper DTO |
| Cookie values | ❌ | Use headers instead |

---

For more detailed examples and edge cases, see [`CONTROLLER_CASES_FINALIZED.md`](docs/CONTROLLER_CASES_FINALIZED.md).
