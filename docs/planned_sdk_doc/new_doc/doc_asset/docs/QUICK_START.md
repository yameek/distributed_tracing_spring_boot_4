# Bits API SDK Generator - Quick Start Guide

## Step 1: Build the SDK (if not already done)

From the parent `api-sdk` directory:

```bash
cd /home/yaziz/workspace/V3_POC/api-sdk
./gradlew clean
./gradlew :bits-sdk-annotations:build
./gradlew :bits-sdk-processor:build
./gradlew publishToMavenLocal
```

This publishes the SDK to your local Maven repository (~/.m2/repository).

## Step 2: Build the Demo Application

From the `demo-app` directory:

```bash
cd /home/yaziz/workspace/V3_POC/api-sdk/demo-app
../gradlew build
```

Or if you have gradle wrapper in demo-app:

```bash
./gradlew build
```

## Step 3: View Generated SDK Files

After building, the generated SDK files will be in:

```
demo-app/build/generated/sources/annotationProcessor/java/main/com/example/sdk/generated/
```

### Expected Generated Files:

1. **Client Interfaces** (`client/`):
   - `UserClient.java`
   - `ProductClient.java`
   - `OrderClient.java`

2. **Client Implementations** (`client/`):
   - `UserClientImpl.java`
   - `ProductClientImpl.java`
   - `OrderClientImpl.java`

3. **Configuration Classes** (`config/`):
   - `ApiVersion.java`
   - `ClientConfiguration.java`
   - `RetryableRestClientInterceptor.java`
   - `ApiVersionHeaderInterceptor.java`

4. **DTOs** (`dto/`):
   - `request/CreateUserRequest.java`
   - `request/UpdateUserRequest.java`
   - `request/CreateProductRequest.java`
   - `response/UserResponse.java`
   - `response/ProductResponse.java`

## Step 4: View Generated Files

You can view the generated files using:

```bash
# List all generated files
find build/generated/sources/annotationProcessor -name "*.java" | sort

# View a specific generated file
cat build/generated/sources/annotationProcessor/java/main/com/example/sdk/generated/client/UserClient.java

# Or open in your IDE
```

## Step 5: Run the Application (Optional)

```bash
./gradlew bootRun
```

Then test the endpoints:
- `http://localhost:8080/v1/users`
- `http://localhost:8080/v1/products`
- `http://localhost:8080/v1/orders`

## Troubleshooting

### SDK not found
If you get errors about missing SDK dependencies:
1. Make sure you've published the SDK to Maven Local (Step 1)
2. Check that `mavenLocal()` is in the repositories in `build.gradle`

### No generated files
If no files are generated:
1. Make sure annotation processor is enabled
2. Check build output for errors
3. Try: `./gradlew clean build --info` to see detailed logs
4. Verify Java 21 is being used: `java -version`

### IDE not showing generated files
1. Refresh/reimport the Gradle project
2. Mark `build/generated/sources/annotationProcessor/java/main` as "Generated Sources Root"
3. In IntelliJ: File → Invalidate Caches / Restart

