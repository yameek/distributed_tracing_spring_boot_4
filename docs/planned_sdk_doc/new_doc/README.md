
# Bits API SDK Generator

**BitsApiSDKGenerator** is a multi-module Gradle (Groovy) project targeting Java 21 and Spring Framework types. It automates the generation of client-side SDKs from Spring Boot REST Controllers.

## Modules
- `bits-sdk-annotations`: source-retention annotations `@BitsSdk`.
- `bits-sdk-processor`: JSR 269 annotation processor generating SDK client stubs from Rest Controller classes.

## Build & Onboarding
- Run `scripts/bootstrap-sdk.sh` to clean, build, and publish the annotations/processor to Maven Local.
- Explore the playground via `scripts/build-demo-client.sh`, which builds both `sdk-playground/demo-app` (server + generators) and `sdk-playground/demo-client` (consumer).
- For a detailed walkthrough—including architecture, environment setup, Gradle wiring (with the sample [`doc_asset/asset/build.gradle`](doc_asset/asset/build.gradle)), and SDK consumption tips—see [`doc_asset/docs/DEVELOPER_GUIDE.md`](doc_asset/docs/DEVELOPER_GUIDE.md).
- Additional references:
  - [`doc_asset/documentation.md`](doc_asset/documentation.md): Feature overview and configuration options.
  - [`doc_asset/CONTROLLER_FEATURES.md`](doc_asset/CONTROLLER_FEATURES.md): Controller support matrix and limitations.

Place an interface annotated with `@com.bracits.sdk.annotation.BitsSdk` in any module that uses this processor (as `annotationProcessor`). The processor will generate a concrete client class next to the interface package.
