# Gradle Quick Start Guide

## Project Overview
- **Build System:** Gradle 9.2.1 with Groovy DSL
- **Java Version:** 25.0.1
- **Groovy Version:** 4.0.28 (bundled with Gradle)
- **Spring Boot:** 4.0.1
- **Project Type:** Multi-module Spring Boot application

## Common Commands

### Building

```bash
# Clean and build all services
./gradlew clean build

# Build without tests
./gradlew build -x test

# Build a specific service
./gradlew :order-service:build

# List all projects
./gradlew projects
```

### Running Services

```bash
# Run a specific service
./gradlew :order-service:bootRun
./gradlew :graphql-service:bootRun
./gradlew :inventory-service:bootRun
./gradlew :notification-service:bootRun

# Run with JVM debug port
./gradlew :order-service:bootRun --debug-jvm
```

### Testing

```bash
# Run all tests
./gradlew test

# Run tests for specific service
./gradlew :order-service:test

# Run tests with verbose output
./gradlew test --info
```

### Cleaning

```bash
# Clean all build artifacts
./gradlew clean

# Clean specific service
./gradlew :order-service:clean
```

### Dependency Management

```bash
# Show dependencies for all projects
./gradlew dependencies

# Show dependencies for specific service
./gradlew :order-service:dependencies

# Check for dependency updates
./gradlew dependencyUpdates
```

## Using the Scripts

### Start All Services
```bash
./run_all.sh
```
This will:
1. Stop any existing services
2. Start Docker infrastructure (RabbitMQ, Tempo, Loki, Grafana)
3. Build all services with Gradle
4. Start all 4 microservices in background
5. Tail logs from all services

### Stop All Services
```bash
./stop_all.sh
```
This will:
1. Stop all Gradle processes
2. Stop all Java service processes
3. Free up ports 8080-8083

### Test the System
```bash
./SIMPLE_TEST.sh
```
This will:
1. Create a test order
2. Display logs with trace IDs
3. Verify tracing is working

## Service Ports

| Service | Port | Health Check |
|---------|------|--------------|
| GraphQL Service | 8080 | http://localhost:8080/actuator/health |
| Order Service | 8081 | http://localhost:8081/actuator/health |
| Inventory Service | 8082 | http://localhost:8082/actuator/health |
| Notification Service | 8083 | http://localhost:8083/actuator/health |

## Project Structure

```
tracing-demo-v2/
├── build.gradle              # Root build configuration
├── settings.gradle           # Multi-project settings
├── gradle.properties         # Gradle properties
├── gradlew                   # Gradle wrapper (Unix)
├── gradlew.bat              # Gradle wrapper (Windows)
├── gradle/
│   └── wrapper/             # Gradle wrapper files
├── order-service/
│   ├── build.gradle         # Order service build config
│   └── src/
├── graphql-service/
│   ├── build.gradle         # GraphQL service build config
│   └── src/
├── inventory-service/
│   ├── build.gradle         # Inventory service build config
│   └── src/
└── notification-service/
    ├── build.gradle         # Notification service build config
    └── src/
```

## Build Output

JAR files are generated in:
```
<service-name>/build/libs/<service-name>-0.0.1-SNAPSHOT.jar
```

## Gradle Daemon

Gradle uses a daemon process to speed up builds. You can:

```bash
# Check daemon status
./gradlew --status

# Stop all daemons
./gradlew --stop
```

## IDE Integration

### IntelliJ IDEA
1. Open the root directory
2. IntelliJ will auto-detect the Gradle project
3. Import should happen automatically
4. Use the Gradle tool window to run tasks

### VS Code
1. Install "Gradle for Java" extension
2. Open the root directory
3. Use the Gradle sidebar to run tasks

## Troubleshooting

### Port Already in Use
```bash
# Find and kill process on specific port
lsof -ti:8081 | xargs kill -9

# Or use the stop script
./stop_all.sh
```

### Gradle Build Fails
```bash
# Clean and retry
./gradlew clean build --refresh-dependencies

# Check Gradle version
./gradlew --version

# Check Java version
java -version
```

### Service Won't Start
```bash
# Check logs in logs/ directory
tail -f logs/order-service.log

# Check if Docker infrastructure is running
docker compose ps

# Restart Docker infrastructure
docker compose down && docker compose up -d
```

## Performance Tips

1. **Enable Configuration Cache** (even faster builds):
   ```bash
   ./gradlew build --configuration-cache
   ```

2. **Parallel Execution**: Already enabled in `gradle.properties`

3. **Build Cache**: Already enabled in `gradle.properties`

4. **Increase JVM Memory**: Edit `gradle.properties` if needed:
   ```properties
   org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
   ```

## Environment Variables

If using SDKMAN (recommended):

```bash
# Ensure correct versions are active
sdk current java    # Should show 25.0.1-open
sdk current gradle  # Should show 9.2.1
```

## Additional Resources

- [Gradle Documentation](https://docs.gradle.org/9.2.1/userguide/userguide.html)
- [Spring Boot Gradle Plugin](https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/htmlsingle/)
- [Gradle Multi-Project Builds](https://docs.gradle.org/9.2.1/userguide/multi_project_builds.html)
