# Bits API SDK Generator - Feature Adaptation Report

Date: 2025-11-18  
Scope: `sdk-playground` (demo-app & demo-client). `bits-sdk-test-app` was not modified.

### 1. Incoming SDK Changes
- **Version-aware URI builder** – `ClientConfiguration` now appends the API version itself when `VersioningStrategy.PATH` is active and therefore expects consumer base URLs without resource segments.
- **Stricter mapping parsing** – The processor now captures controller-level paths verbatim; multi-value `@RequestMapping`/`@PostMapping` declarations bleed into the generated URI strings instead of being normalized the way older processors did.
- **Record-friendly DTO writer** – `DtoGenerator` detects source records and routes them through a dedicated writer so SDK DTOs stay consistent without scattering `if (record)` checks.

### 2. Playground Updates
1. `demo-app`  
   - Simplified `OrderController` mappings to single values so generated URIs remain clean (`/v1/orders` instead of the previous concatenated `,/` artifacts).  
2. `demo-client`  
   - Re-pointed `demo.app.order-base-url` to the service root (`http://localhost:8080`) so that the SDK appends `/v1/orders` only once when composing calls with the refreshed generator.

#### Required Demo-App Changes for Future Adopters
- Ensure each `@RequestMapping`/`@PostMapping` annotation declares a single canonical path (let `NormalizePathFilter` handle duplicates/trailing slashes).
- Keep controller methods free of duplicate path variants such as `{"", "/"}` to prevent literal string combinations leaking into generated clients.
- Rebuild the SDK (`./gradlew :demo-app:buildSdk`) whenever controller mappings change so the regenerated clients carry the normalized paths.
- When switching a request/response DTO to a Java record, update controllers to call component accessors (`record.field()`) instead of bean getters; records no longer inherit the `getX()` methods the old classes provided.

#### Required Demo-Client Changes for Future Adopters
- Configure `demo.app.order-base-url` (or the consuming app’s equivalent property) to the service root only (e.g., `http://localhost:8080`); avoid embedding resource paths because `ClientConfiguration` now appends API version + controller segments automatically.
- If multiple controllers live under different base paths, provide separate `ClientConfiguration` beans per client to avoid hard-coded path fragments.
- After updating configuration, republish the SDK from demo-app to Maven Local and rebuild demo-client so it pulls the refreshed artifacts.

### 3. Test Run
| Step | Result |
| --- | --- |
| `./sdk-playground/scripts/run_sdk_playground.sh` (before fixes) | `500 Internal Server Error` from `demo-client` verification |
| Same script after fixes | `200 OK` with created order payload echoed from demo-app |

Artifacts: `sdk-playground/build/logs/demo-app.log`, `sdk-playground/build/logs/demo-client.log`.

### 4. Failure Analysis & Fix Guidance
- **Symptom**: SDK client hit `/v1/orders/v1/orders` which demo-app normalizer expanded even further, leading to `404` and surfacing to demo-client as `500`.
- **Root Causes**:
  1. Multi-value controller mappings were emitted literally (`",/"`) into generated clients after the new parser stopped collapsing them.
  2. The new version-aware URI builder expects the base URL to be the service root, so embedding `/v1/orders` in `application.properties` duplicated the path segment.
- **Resolution**:  
  - Keep controller annotations to single path strings (rely on `NormalizePathFilter` for trailing slashes).  
  - Configure `ClientConfiguration` with only the host base URL; allow the generated client to append controller paths and API versions.

### 5. Verification
- Demo client successfully created an order via the generated SDK (see script output timestamped `2025-11-18T17:46:22Z`).
- No additional changes required outside `sdk-playground`.

### 6. Record/Class DTO Coverage (Current Work)
| Area | Request DTO | Response DTO | Notes |
| --- | --- | --- | --- |
| `OrderController` | `CreateOrderRequest` (record), `UpdateOrderStatusRequest` (record) | `OrderResponse` (class) | Validates record requests + class responses in one controller. |
| `ProductController` | `CreateProductRequest`, `UpdateProductRequest` (classes) | `ProductResponse` (class) | Ensures legacy class flow keeps working. |
| `UserController` | `CreateUserRequest`, `UpdateUserRequest` (classes) | `UserResponse` (class) | Verifies nested mutations remain compatible. |

| Command | Purpose | Result |
| --- | --- | --- |
| `./gradlew :demo-app:compileJava` (run inside `sdk-playground`) | Triggers annotation processor to generate SDK DTOs/clients from mixed record/class definitions. | ✅ Success (records + classes generated without errors). |
| `./gradlew :demo-client:compileJava` (run inside `sdk-playground`) | Confirms the generated client compiles against the new DTO outputs. | ✅ Success (no failures). |
