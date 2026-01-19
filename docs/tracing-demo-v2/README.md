# Tracing Demo V2 Documentation

This folder contains comprehensive documentation for the Distributed Tracing Demo V2 project.

## 📚 Documentation Index

### Quick Start Guides
- **[Quick Start: HTTP + RabbitMQ](QUICK_START_HTTP_RABBITMQ.md)** - Get started with distributed tracing in 5 minutes
- **[Gradle Quick Start](GRADLE_QUICK_START.md)** - Quick start guide for Gradle setup

### Architecture & Design
- **[Architecture with Collector](ARCHITECTURE_WITH_COLLECTOR.md)** - System architecture with OpenTelemetry Collector
- **[Trace Flow Diagram](TRACE_FLOW_DIAGRAM.md)** - Visual representation of trace propagation across services
- **[CQRS Service Summary](CQRS_SERVICE_SUMMARY.md)** - Command Query Responsibility Segregation implementation

### Implementation Guides
- **[Distributed Tracing Guide](DISTRIBUTED_TRACING_GUIDE.md)** - Comprehensive guide to distributed tracing implementation
- **[Collector Guide](COLLECTOR_GUIDE.md)** - OpenTelemetry Collector setup and configuration
- **[Collector Setup Summary](COLLECTOR_SETUP_SUMMARY.md)** - Quick summary of collector setup
- **[HTTP + RabbitMQ Tracing Summary](HTTP_RABBITMQ_TRACING_SUMMARY.md)** - How HTTP and RabbitMQ tracing works together
- **[Implementation Complete](IMPLEMENTATION_COMPLETE.md)** - Complete implementation details

### Migration & Updates
- **[Observed Annotation Migration Guide](OBSERVED_ANNOTATION_MIGRATION_GUIDE.md)** - Migrating from manual spans to @Observed annotation
- **[Observed Quick Reference](OBSERVED_QUICK_REFERENCE.md)** - Quick reference for @Observed annotation
- **[Migration Summary](MIGRATION_SUMMARY.md)** - Summary of all migrations performed
- **[Updates Summary](UPDATES_SUMMARY.md)** - Latest changes and improvements

### Testing Documentation
- **[Testing Guide](TESTING_GUIDE.md)** - Complete testing documentation and strategies
- **[Testing Summary](TESTING_SUMMARY.md)** - Summary of testing approach and results
- **[Test Results](TEST_RESULTS.md)** - Detailed test results
- **[Test Results: Observed Migration](TEST_RESULTS_OBSERVED_MIGRATION.md)** - Test results after @Observed migration
- **[CQRS Test Results](CQRS_TEST_RESULTS.md)** - CQRS service test results

### Examples & References
- **[Example Trace Output](EXAMPLE_TRACE_OUTPUT.md)** - Sample trace outputs and what they mean

## 🔗 Related Documentation

- **[Main Project README](../../tracing-demo-v2/README.md)** - Project overview and setup instructions
- **[Original Tracing Demo Docs](../tracing-demo/)** - Documentation for the original tracing demo
- **[Migration Notes](../migration/)** - Java 25 migration documentation

## 📖 Recommended Reading Order

### For New Users
1. Start with [Quick Start: HTTP + RabbitMQ](QUICK_START_HTTP_RABBITMQ.md)
2. Read [Architecture with Collector](ARCHITECTURE_WITH_COLLECTOR.md)
3. Follow [Distributed Tracing Guide](DISTRIBUTED_TRACING_GUIDE.md)
4. Review [Testing Guide](TESTING_GUIDE.md)

### For Developers
1. [Observed Quick Reference](OBSERVED_QUICK_REFERENCE.md) - Daily reference
2. [Distributed Tracing Guide](DISTRIBUTED_TRACING_GUIDE.md) - Implementation patterns
3. [Trace Flow Diagram](TRACE_FLOW_DIAGRAM.md) - Understanding trace propagation
4. [Testing Guide](TESTING_GUIDE.md) - Testing strategies

### For Migration
1. [Observed Annotation Migration Guide](OBSERVED_ANNOTATION_MIGRATION_GUIDE.md)
2. [Migration Summary](MIGRATION_SUMMARY.md)
3. [Test Results: Observed Migration](TEST_RESULTS_OBSERVED_MIGRATION.md)

## 🎯 Key Features Documented

- ✅ Distributed tracing across HTTP and RabbitMQ
- ✅ OpenTelemetry Collector integration
- ✅ @Observed annotation usage
- ✅ Trace ID propagation
- ✅ Log correlation
- ✅ CQRS with Event Sourcing
- ✅ GraphQL tracing
- ✅ Comprehensive testing strategies

## 🛠️ Technology Stack

- Spring Boot 4.0.1
- Java 25 LTS
- Micrometer Tracing
- OpenTelemetry
- Grafana Tempo
- Grafana Loki
- RabbitMQ
- GraphQL

---

**Last Updated**: January 2026
