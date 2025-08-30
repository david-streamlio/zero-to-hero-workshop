# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Architecture Overview

This is a real-time crypto data processing workshop demonstrating stream processing with Apache Pulsar and Apache Flink. The codebase is organized into two main processing frameworks:

### Core Components

**Pulsar Functions** (`coinbase-functions/`):
- `coinbase-live-feed`: WebSocket source connector for ingesting live Coinbase market data
- `coinbase-websocket-feed-router`: Content-based routing function for message distribution
- `coinbase-ticker-sink`: Data persistence layer for ticker information
- `coinbase-datatypes`: Shared data models (Ticker, Auction, RfqMatch schemas)

**Flink Jobs** (`coinbase-flink/`):
- `coinbase-aggregations`: Price averaging and aggregation jobs (Stream API and Table API)
- `coinbase-filters`: Data filtering based on product IDs and other criteria
- `coinbase-joins`: Stream-to-stream and stream-to-table join operations
- `coinbase-flink-utils`: Shared utilities and topic providers
- `coinbase-schemas`: Flink-specific data schemas
- `trades-moving-average`: Moving average calculations with state management

The workshop follows a progressive lab structure (Lab 1-9) in `docs/lab-guides/`, building from basic infrastructure setup to advanced stream processing patterns.

## Build Commands

### Build All Components
```bash
# Build Flink jobs and copy artifacts to Docker mount points
./bin/flink/build-flink-jobs.sh

# Build Pulsar functions and copy NAR files to Docker mount points
./bin/pulsar/build-functions.sh
```

### Maven Commands
```bash
# Build Flink components (requires Java 17)
mvn clean install -f coinbase-flink/pom.xml

# Build Pulsar functions (skips Docker builds by default)
mvn clean install -f coinbase-functions/pom.xml -Ddocker.skip=true
```

### Individual Module Testing
```bash
# Test specific Flink module
mvn test -f coinbase-flink/coinbase-aggregations/pom.xml

# Test specific Pulsar function
mvn test -f coinbase-functions/coinbase-websocket-feed-router/pom.xml
```

## Infrastructure Management

### Full Environment
```bash
# Start complete stack (Pulsar, MySQL, Grafana, data feeds)
./bin/start-everything.sh

# Stop all services and clean Docker volumes
./bin/stop-everything.sh
```

### Individual Services
```bash
# Infrastructure components
./bin/pulsar/start-pulsar.sh    # Apache Pulsar cluster (broker, bookie, zookeeper)
./bin/mysql/start-mysql.sh      # MySQL database for sink operations
./bin/grafana/start-grafana.sh  # Grafana dashboards for monitoring
./bin/flink/start-flink.sh      # Apache Flink cluster

# Data pipeline functions
./bin/pulsar/start-coinbase-feed.sh         # Live WebSocket data ingestion
./bin/pulsar/start-coinbase-feed-router.sh  # Content-based message routing
./bin/pulsar/start-coinbase-ticker-tracker.sh  # Ticker data sink to MySQL
```

## Deployment Architecture

**Docker Compose** (`deployments/docker/`):
- Local development with containerized infrastructure
- Service definitions in `cluster.yaml` files per component
- Mount points for artifacts: `deployments/docker/infrastructure/flink/jobs/` (JAR files), `deployments/docker/infrastructure/pulsar/functions/lib/` (NAR files)

**Kubernetes** (`deployments/k8s/`):
- Standard K8s deployments for production
- VVP (Ververica Platform) integration for Flink job management

**StreamNative BYOC** (`deployments/sn-byoc/`):
- Cloud deployment using StreamNative's platform
- Requires CLI tools: `pulsarctl`, `snctl`
- Resource definitions in `workspace.yaml` and `flink-deployment.yaml`

## Code Patterns and Conventions

### Pulsar Functions
- Implement `Function<String, Void>` interface for processing logic
- Use `@Override public Void process(String input, Context ctx)` pattern
- JSON deserialization with Jackson ObjectMapper
- Output routing via `ctx.newOutputMessage(topic, schema).value(data).send()`
- NAR packaging using nifi-nar-maven-plugin
- WebSocket reconnection with configurable parameters:
  - `maxReconnectAttempts`: Maximum reconnection attempts (default: 10)
  - `reconnectBaseDelayMs`: Base delay with exponential backoff (default: 1000ms)

### Flink Jobs
- Stream processing: Extend DataStream API with source/sink connectors
- Table API: SQL-like operations for complex aggregations
- State management: Keyed streams for windowed operations
- Pulsar integration via `PulsarSource` and `PulsarSink`
- JAR packaging for Flink cluster deployment

### Data Models
- Shared schemas in `coinbase-datatypes` using Lombok annotations
- Jackson annotations for JSON serialization
- Schema evolution support for backward compatibility

## Data Flow Pipeline

1. **Ingestion**: Coinbase WebSocket API → `coinbase-live-feed` → `persistent://feeds/realtime/coinbase-livefeed`
2. **Routing**: `coinbase-websocket-feed-router` distributes by channel type (ticker/auction/rfq_matches)
3. **Processing**: Flink jobs consume from topic-specific streams for transformations
4. **Storage**: `coinbase-ticker-sink` persists to MySQL, Grafana provides visualization

## Development Workflow

### Verification Commands
```bash
# Verify Docker infrastructure
docker ps

# Monitor live data ingestion
docker exec -it pulsar-broker ./bin/pulsar-client consume -n 0 -p Earliest -s test-sub persistent://feeds/realtime/coinbase-livefeed

# Check topic statistics and throughput
docker exec -it pulsar-broker ./bin/pulsar-admin topics stats persistent://feeds/realtime/coinbase-livefeed

# Validate Pulsar functions
docker exec -it pulsar-broker ./bin/pulsar-admin functions list --tenant public --namespace default
```

### Key Technologies
- Java 17 with Maven multi-module structure
- Apache Pulsar 4.0.5.x for messaging and Functions runtime
- Apache Flink 1.19.0 for stream processing (DataStream and Table APIs)
- Jackson 2.17.2 for JSON processing with JSR310 time module
- Lombok 1.18.30 for code generation
- Docker Compose for local orchestration