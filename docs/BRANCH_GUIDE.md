# Branch Guide: Distributed Tracing Implementations

This document explains the different branches in this repository and what distributed tracing implementation each contains. Use this guide to understand which branch to use for your specific needs.

## 📋 Quick Reference

| Branch | Build System | Tracing Approach | Key Features | Status |
|--------|--------------|------------------|--------------|--------|
| **master** | Gradle | @Observed annotation | CQRS, Collector, Java 25 | ✅ **Current** |
| **feature/cqrs-service** | Gradle | @Observed annotation | CQRS service added | ✅ Merged to master |
| **gradle+collector** | Gradle | Manual spans | OpenTelemetry Collector | ✅ Merged to master |
| **groovy_java25** | Gradle | Manual spans | Java 25 migration | ✅ Merged to master |
| **maven_basics_working_branch** | Maven | Manual spans | Basic working implementation | 📦 Baseline |
| **springboot-3.4.1-downgrade** | Gradle | Manual spans | Spring Boot 3.4.1, JDK 21 | 🔧 Stability fix |

---

## 🌿 Branch Details

### 1. **master** (Current Branch)
**Status**: ✅ Active development branch  
**Last Updated**: January 2026

#### Key Characteristics
- **Build System**: Gradle (multi-module project)
- **Java Version**: Java 25 LTS
- **Spring Boot Version**: 4.0.1
- **Tracing Approach**: `@Observed` annotation (declarative)
- **Architecture**: Microservices with CQRS pattern

#### Services Included
1. **graphql-service** - GraphQL entry point (port 8080)
2. **order-service** - Order processing (port 8081)
3. **inventory-service** - Inventory management (port 8082)
4. **notification-service** - Notifications (port 8083)
5. **cqrs-service** - CQRS with Command/Event/Query buses (port 8084)
6. **orchestrator-service** - Service orchestration (port 8085)

#### Infrastructure
- ✅ OpenTelemetry Collector (otel-collector)
- ✅ Grafana Tempo (trace storage)
- ✅ Grafana Loki (log aggregation)
- ✅ Grafana (visualization)
- ✅ RabbitMQ (message broker)
- ✅ PostgreSQL (for CQRS service)

#### Tracing Implementation
- Uses `@Observed` annotation for automatic span creation
- Automatic trace context propagation via Spring Boot
- OTLP export through OpenTelemetry Collector
- Full trace correlation with logs

#### When to Use
- **Primary branch** for all new development
- Best practices implementation
- Most complete feature set
- Production-ready patterns

#### Documentation
- See `docs/tracing-demo-v2/` for comprehensive guides
- `docs/tracing-demo-v2/OBSERVED_ANNOTATION_MIGRATION_GUIDE.md` - Migration details
- `docs/tracing-demo-v2/CQRS_SERVICE_SUMMARY.md` - CQRS implementation

---

### 2. **feature/cqrs-service**
**Status**: ✅ Merged into master  
**Purpose**: Added CQRS service implementation

#### Key Characteristics
- **Build System**: Gradle
- **Tracing Approach**: `@Observed` annotation
- **Main Addition**: CQRS service with Command Bus, Event Bus, Query Bus, and Outbox pattern

#### What Was Added
- CQRS service module
- Command/Event/Query handler implementations
- Outbox pattern for reliable event publishing
- PostgreSQL integration
- Comprehensive test scripts

#### When to Use
- **No longer needed** - merged into master
- Reference for CQRS implementation details

#### Related Commits
- `4c610bd` - Add CQRS service with Command Bus, Event Bus, and Outbox pattern
- `e04588d` - Refactor tracing implementation to use @Observed annotation
- `59c56ce` - Enhance documentation and update service configurations for V2

---

### 3. **gradle+collector**
**Status**: ✅ Merged into master  
**Purpose**: Added OpenTelemetry Collector integration

#### Key Characteristics
- **Build System**: Gradle
- **Tracing Approach**: Manual span creation
- **Main Addition**: OpenTelemetry Collector in Docker Compose

#### What Was Added
- OpenTelemetry Collector service in `docker-compose.yml`
- Collector configuration (`config/otel-collector-config.yaml`)
- Services configured to send traces to collector instead of directly to Tempo
- Collector metrics endpoints

#### Architecture Change
```
Before: Services → Tempo (direct)
After:  Services → Collector → Tempo
```

#### When to Use
- **No longer needed** - merged into master
- Reference for collector setup

#### Related Commits
- `8844a4c` - Add OpenTelemetry Collector to Docker Compose and update services

---

### 4. **groovy_java25**
**Status**: ✅ Merged into master  
**Purpose**: Migrated from Maven to Gradle and upgraded to Java 25

#### Key Characteristics
- **Build System**: Gradle (migrated from Maven)
- **Java Version**: Java 25 LTS
- **Tracing Approach**: Manual span creation

#### What Changed
- Converted Maven `pom.xml` files to Gradle `build.gradle` files
- Multi-module Gradle project structure
- Updated Java version to 25
- Maintained all existing functionality

#### When to Use
- **No longer needed** - merged into master
- Reference for Gradle migration patterns

#### Related Commits
- `32fe764` - gradle migrate

---

### 5. **maven_basics_working_branch**
**Status**: 📦 Baseline branch  
**Purpose**: Original working Maven-based implementation

#### Key Characteristics
- **Build System**: Maven
- **Java Version**: Java 21
- **Spring Boot Version**: 3.x
- **Tracing Approach**: Manual span creation (`@NewSpan`, `Observation.createNotStarted()`)

#### Services Included
1. graphql-service
2. order-service
3. inventory-service
4. notification-service

#### Infrastructure
- Grafana Tempo (direct connection, no collector)
- Grafana Loki
- Grafana
- RabbitMQ

#### Tracing Implementation
- Manual span creation using Micrometer Observation API
- Direct OTLP export to Tempo
- Custom `TraceIdFilter` for log correlation
- Manual trace context propagation

#### When to Use
- Reference for **Maven-based** implementations
- Understanding the **original approach** before migrations
- Baseline for comparing changes
- If you need Maven instead of Gradle

#### Key Differences from master
- ❌ No OpenTelemetry Collector
- ❌ No CQRS service
- ❌ No `@Observed` annotation
- ❌ Maven instead of Gradle
- ❌ Java 21 instead of Java 25
- ❌ Spring Boot 3.x instead of 4.0.1

---

### 6. **springboot-3.4.1-downgrade**
**Status**: 🔧 Stability fix branch  
**Purpose**: Downgrade to Spring Boot 3.4.1 and JDK 21 for stability

#### Key Characteristics
- **Build System**: Gradle
- **Java Version**: JDK 21
- **Spring Boot Version**: 3.4.1
- **Tracing Approach**: Manual spans

#### Why This Branch Exists
Spring Boot 4.0.1 is a milestone release that may have stability issues. This branch provides a more stable alternative using Spring Boot 3.4.1.

#### When to Use
- If you encounter **stability issues** with Spring Boot 4.0.1
- If you need **JDK 21** instead of Java 25
- For **production deployments** requiring proven stability
- When **compatibility** with older Spring Boot ecosystem is needed

#### Trade-offs
- ✅ More stable Spring Boot version
- ✅ Better ecosystem compatibility
- ❌ Missing Spring Boot 4.0.1 features
- ❌ Older Java version (JDK 21 vs Java 25)

#### Related Commits
- `0f3926f` - feat: Downgrade to Spring Boot 3.4.1 and JDK 21 for stable tracing

---

## 🔄 Migration Path

### Understanding the Evolution

```
maven_basics_working_branch (baseline)
    ↓
groovy_java25 (Gradle migration + Java 25)
    ↓
gradle+collector (OpenTelemetry Collector added)
    ↓
feature/cqrs-service (@Observed migration + CQRS)
    ↓
master (current - all features combined)
```

### Alternative Path (Stability)
```
maven_basics_working_branch
    ↓
springboot-3.4.1-downgrade (stability fix)
```

---

## 🎯 Choosing the Right Branch

### Use **master** if you want:
- ✅ Latest features and best practices
- ✅ Complete implementation (CQRS, Collector, etc.)
- ✅ Modern Spring Boot 4.0.1
- ✅ Java 25 LTS
- ✅ `@Observed` annotation approach

### Use **maven_basics_working_branch** if you:
- ✅ Need Maven build system
- ✅ Want simpler baseline implementation
- ✅ Prefer manual span control
- ✅ Need Java 21 compatibility

### Use **springboot-3.4.1-downgrade** if you:
- ✅ Need maximum stability
- ✅ Require Spring Boot 3.x compatibility
- ✅ Prefer JDK 21
- ✅ Have production stability concerns

---

## 📊 Feature Comparison Matrix

| Feature | master | maven_basics | springboot-3.4.1-downgrade |
|---------|--------|--------------|----------------------------|
| Build System | Gradle | Maven | Gradle |
| Java Version | 25 LTS | 21 | 21 |
| Spring Boot | 4.0.1 | 3.x | 3.4.1 |
| Tracing Method | @Observed | Manual | Manual |
| OpenTelemetry Collector | ✅ | ❌ | ❌ |
| CQRS Service | ✅ | ❌ | ❌ |
| Orchestrator Service | ✅ | ❌ | ❌ |
| PostgreSQL | ✅ | ❌ | ❌ |
| Documentation | ✅ Extensive | ✅ Basic | ✅ Basic |

---

## 🔍 Branch Inspection Commands

### View all branches
```bash
git branch -a
```

### Checkout a specific branch
```bash
git checkout <branch-name>
```

### Compare branches
```bash
# Compare master with maven branch
git diff maven_basics_working_branch..master --stat

# See what's in a branch
git log <branch-name> --oneline -10
```

### View branch differences
```bash
# See files changed between branches
git diff maven_basics_working_branch..master --name-only

# See specific file differences
git diff maven_basics_working_branch..master -- tracing-demo-v2/docker-compose.yml
```

---

## 📝 Notes

### Branch Naming Convention
- **master**: Main development branch
- **feature/***: Feature branches (usually merged)
- **gradle+collector**: Descriptive name for collector addition
- **groovy_java25**: Descriptive name for Gradle/Java 25 migration
- **maven_basics_working_branch**: Baseline Maven implementation
- **springboot-3.4.1-downgrade**: Stability branch

### Merged Branches
The following branches have been merged into master and can be safely deleted:
- `feature/cqrs-service` ✅
- `gradle+collector` ✅
- `groovy_java25` ✅

### Active Branches
- `master` - Active development
- `maven_basics_working_branch` - Baseline reference
- `springboot-3.4.1-downgrade` - Stability alternative

---

## 🚀 Quick Start by Branch

### master
```bash
git checkout master
cd tracing-demo-v2
docker compose up -d
./run_all.sh
./test_system.sh
```

### maven_basics_working_branch
```bash
git checkout maven_basics_working_branch
cd tracing-demo-v2
docker compose up -d
./run_all.sh
./test_system.sh
```

### springboot-3.4.1-downgrade
```bash
git checkout springboot-3.4.1-downgrade
cd tracing-demo-v2
docker compose up -d
./run_all.sh
./test_system.sh
```

---

## 📚 Related Documentation

- **Main README**: `README.md`
- **Implementation Guide**: `docs/IMPLEMENTATION_GUIDE.md`
- **Tracing V2 Docs**: `docs/tracing-demo-v2/README.md`
- **Migration Guide**: `docs/tracing-demo-v2/OBSERVED_ANNOTATION_MIGRATION_GUIDE.md`
- **CQRS Guide**: `docs/tracing-demo-v2/CQRS_SERVICE_SUMMARY.md`
- **Collector Guide**: `docs/tracing-demo-v2/COLLECTOR_GUIDE.md`

---

**Last Updated**: January 2026  
**Maintained By**: Repository maintainers
