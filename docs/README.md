# Documentation

This directory contains all documentation for the distributed tracing demo project.

## Quick Navigation

### 🚀 Start Here

| Document | Description | When to Read |
|----------|-------------|--------------|
| **[PROJECT_RECREATION_GUIDE.md](PROJECT_RECREATION_GUIDE.md)** | ⭐ **Complete project recreation from scratch** | Building this project yourself |
| **[IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md)** | Implementation concepts and patterns | Learning how distributed tracing works |
| **[ARCHITECTURE.md](ARCHITECTURE.md)** | System architecture | Understanding the system design |
| **[GOAL.md](GOAL.md)** | Project objectives | Understanding what this achieves |

### 📖 Using the System

| Document | Description | When to Read |
|----------|-------------|--------------|
| **[GRAFANA_GUIDE.md](GRAFANA_GUIDE.md)** | How to use Grafana for traces/logs | Viewing traces and debugging |
| **[tracing-demo/tracing_access_instructions.md](tracing-demo/tracing_access_instructions.md)** | Access URLs and credentials | Accessing the services |
| **[tracing-demo/tracing_visualization_guide.md](tracing-demo/tracing_visualization_guide.md)** | Understanding visualizations | Interpreting traces |

### 🔧 Reference

| Document | Description | When to Read |
|----------|-------------|--------------|
| **[STACK.md](STACK.md)** | Technology stack details | Understanding technologies used |
| **[FIX_HISTORY.md](FIX_HISTORY.md)** | Common issues and fixes | Troubleshooting problems |
| **[migration/tracing_java_25_migration.md](migration/tracing_java_25_migration.md)** | Java 25 migration notes | Migrating to Java 25 |

### 📚 Additional Resources

| Document | Description |
|----------|-------------|
| **[tracing-demo/tracing_quick_reference.md](tracing-demo/tracing_quick_reference.md)** | Quick reference for common patterns |
| **[tracing-demo/tracing_trace_ids_explanation.md](tracing-demo/tracing_trace_ids_explanation.md)** | How trace IDs work |
| **[tracing-demo/tracing_architecture_diagrams.md](tracing-demo/tracing_architecture_diagrams.md)** | Architecture diagrams |
| **[tracing-demo-v2/README.md](tracing-demo-v2/README.md)** | 🆕 Complete V2 documentation index |

## Directory Structure

```
docs/
├── IMPLEMENTATION_GUIDE.md      # ⭐ Main implementation guide
├── ARCHITECTURE.md              # System architecture
├── GOAL.md                      # Project objectives
├── STACK.md                     # Technology stack
├── GRAFANA_GUIDE.md             # Grafana usage guide
├── FIX_HISTORY.md               # Troubleshooting history
│
├── tracing-demo/                # Original demo documentation
│   ├── tracing_access_instructions.md
│   ├── tracing_architecture_diagrams.md
│   ├── tracing_quick_reference.md
│   ├── tracing_trace_ids_explanation.md
│   └── tracing_visualization_guide.md
│
├── tracing-demo-v2/             # 🆕 V2 comprehensive documentation
│   ├── README.md                # V2 documentation index
│   ├── Quick Start guides
│   ├── Architecture & Design docs
│   ├── Implementation guides
│   ├── Migration & Updates
│   ├── Testing documentation
│   └── Examples & References
│
└── migration/                   # Migration documentation
    └── tracing_java_25_migration.md
```

## Documentation Philosophy

This documentation follows these principles:

1. **Self-explanatory** - Each guide should be complete and standalone
2. **Practical** - Focus on working code examples
3. **Minimal** - Keep only essential documentation
4. **Current** - Remove obsolete or redundant files

## Recommended Reading Order

### For First-Time Users
1. Read [GOAL.md](GOAL.md) - understand what this project does
2. Read [ARCHITECTURE.md](ARCHITECTURE.md) - see how it works
3. Follow [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md) - learn implementation details
4. Use [GRAFANA_GUIDE.md](GRAFANA_GUIDE.md) - explore traces

### For Implementing in Your Project
1. Read [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md) - step-by-step guide
2. Check [tracing-demo/tracing_quick_reference.md](tracing-demo/tracing_quick_reference.md) - code snippets
3. Reference [FIX_HISTORY.md](FIX_HISTORY.md) - common issues

### For Troubleshooting
1. Check [FIX_HISTORY.md](FIX_HISTORY.md) first
2. Review [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md) section 8
3. Use [GRAFANA_GUIDE.md](GRAFANA_GUIDE.md) for debugging

## Key Concepts

### Distributed Tracing
A **trace** represents the complete journey of a request through your system. Each operation within a trace is called a **span**. Spans are connected by parent-child relationships, forming a trace tree.

### Trace Propagation
**Trace context** (trace ID and span ID) must be propagated between services. This happens:
- Via HTTP headers for REST/GraphQL calls
- Via message headers for RabbitMQ
- Automatically when using instrumented clients

### Observability Stack
- **Tempo**: Stores traces
- **Loki**: Stores logs
- **Grafana**: Visualizes both
- **OpenTelemetry**: Generates and exports telemetry

## Getting Help

- **Implementation questions**: See [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md)
- **Common issues**: See [FIX_HISTORY.md](FIX_HISTORY.md)
- **Architecture questions**: See [ARCHITECTURE.md](ARCHITECTURE.md)
- **Grafana usage**: See [GRAFANA_GUIDE.md](GRAFANA_GUIDE.md)

## Main Project

The actual code is in [`/tracing-demo-v2/`](../tracing-demo-v2/) at the project root.
