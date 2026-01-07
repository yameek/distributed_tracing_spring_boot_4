# Bits API SDK Generator - Developer Guide

Welcome to the **Bits API SDK Generator** (BitsApiSDKGenerator) developer guide. This document explains how the repository is structured, how to bootstrap the toolchain, and how to consume the generated SDKs in downstream services.

## Architecture at a Glance
- `bits-sdk-annotations`: Source-retention annotations (such as `@BitsSdk`) that mark controllers for SDK generation.
- `bits-sdk-processor`: A JSR 269 annotation processor that inspects annotated controllers and emits client interfaces/implementations, DTOs, configuration helpers, and interceptors.
- `sdk-playground`: A demo workspace that houses a Spring Boot server (`demo-app`) and a consumer (`demo-client`) to validate the end-to-end SDK generation pipeline.
- `doc_asset`: Living documentation, reference assets (including a sample Gradle build), and onboarding material.

The annotation processor hooks into the server build, translates REST controllers into typed client surfaces, and writes the generated code to `build/generated/sources/annotationProcessor/...`. These classes can then be packaged and shipped to consumers.

## Onboarding Checklist
1. Install Java 21, Gradle 8+, and ensure `~/.m2` is writable for `publishToMavenLocal`.
2. Clone the repository and run `scripts/bootstrap-sdk.sh` (details below) to build and publish the annotations and processor locally.
3. Explore `doc_asset/documentation.md` for feature-level details and `doc_asset/CONTROLLER_FEATURES.md` for the controller support matrix.
4. Open `sdk-playground/demo-app` in your IDE to see working controller examples.
5. Use the generated artifacts (or `sdk-playground/demo-client`) to verify client consumption.

## Environment Setup
- **Java Toolchain**: The repo enforces Java 21 via Gradle toolchains.
- **Gradle Wrapper**: Use `./gradlew` at the repository root to guarantee consistent builds.
- **Local Maven**: `publishToMavenLocal` is required so sample projects can resolve `bits-sdk-annotations` and `bits-sdk-processor` without hitting an external registry.

## Build Workflow
1. Clean and rebuild modules:
   ```bash
   ./gradlew clean
   ./gradlew :bits-sdk-annotations:build
   ./gradlew :bits-sdk-processor:build
   ./gradlew publishToMavenLocal
   ```
2. Generate SDKs in a host project (e.g., `sdk-playground/demo-app`) by running its `./gradlew build`.
3. Package and publish the generated SDK as a standalone artifact (sample task provided in the asset Gradle file below).

## Helper Scripts
Two helper scripts live under `scripts/`:

- `scripts/bootstrap-sdk.sh`: Runs the canonical build/publish sequence for annotations and processor modules, validates publication to Maven Local, and surfaces errors early.
- `scripts/build-demo-client.sh`: Builds the demo playground (server + client) to ensure generators and consumers remain in sync.

Each script prints the Gradle commands it executes, sets `set -euo pipefail` for safety, and can be chained in CI or run locally.

## Sample Gradle Configuration (`doc_asset/asset/build.gradle`)
The `doc_asset/asset/build.gradle` file serves as a starting point for teams wiring the processor into their Spring Boot applications. Highlights:

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.7'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'maven-publish'
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    compileOnly 'com.bracits:bits-sdk-annotations:1.0.1'
    annotationProcessor 'com.bracits:bits-sdk-annotations:1.0.1'
    annotationProcessor 'com.bracits:bits-sdk-processor:1.0.1'

    implementation 'io.github.resilience4j:resilience4j-circuitbreaker:2.1.0'
    implementation 'io.github.resilience4j:resilience4j-core:2.1.0'
}
```

Additional tasks (`buildSdk`, `verifySdk`) illustrate how to package only the generated SDK classes and publish them via Gradle's `publishing` block. Consult the file for task-level logging and customization points.

## Using the Generated SDK
1. **Configuration Beans**: Provide `ClientConfiguration` and `CircuitBreakerConfiguration` beans that set base URLs, timeouts, retries, and circuit breaker policies.
2. **Injection**: Spring automatically wires the generated `*ClientImpl` components; inject the interface (`FooClient`) into your services.
3. **Versioning Strategy**: Control header vs. URL versioning through the `@BitsSdk` annotation and override defaults through `ClientConfiguration`.
4. **Error Handling**: Resilience4j Circuit Breakers wrap every outbound call; configure thresholds to match your SLA appetite.

## Troubleshooting
- No generated classes? Verify that `@BitsSdk` is on the controller, the module depends on the annotations, and the processor is registered as an `annotationProcessor`.
- Missing DTO fields? Ensure Spring annotations (`@RequestBody`, `@PathVariable`, `@RequestParam`) are explicitly applied so the processor can categorize parameters.
- Client bean not found? Confirm the generated package is within the component scan (or register it manually via `@Import`).

## Additional Resources
- [`doc_asset/documentation.md`](../documentation.md) – High-level overview and configuration options.
- [`doc_asset/CONTROLLER_FEATURES.md`](../CONTROLLER_FEATURES.md) – Supported annotations, parameter rules, and limitations.
- [`doc_asset/docs/README.md`](README.md) – Demo playground walkthrough (controllers, endpoints, and generated code locations).

