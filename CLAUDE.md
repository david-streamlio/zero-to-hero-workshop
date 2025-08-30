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
# Note: Build coinbase-flink-utils first as other modules depend on it
mvn clean install -f coinbase-flink/coinbase-flink-utils/pom.xml
mvn clean install -f coinbase-flink/pom.xml

# Build Pulsar functions (skips Docker builds by default)  
mvn clean install -f coinbase-functions/pom.xml -Ddocker.skip=true
```

### Individual Module Testing
```bash
# Test specific Flink module (build utils first)
mvn install -f coinbase-flink/coinbase-flink-utils/pom.xml
mvn test -f coinbase-flink/coinbase-aggregations/pom.xml

# Test specific Pulsar function
mvn test -f coinbase-functions/coinbase-websocket-feed-router/pom.xml

# Test configuration system
mvn test -f coinbase-flink/coinbase-flink-utils/pom.xml
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
- **Uniform Configuration System**: All jobs use `ConfigurationProvider` with fallback precedence:
  1. Command line parameters (`--pulsar.service.url=...`)
  2. Environment variables (`PULSAR_SERVICE_URL=...`)
  3. Configuration files (K8s ConfigMap, Docker mount, classpath)
  4. Default values

### Data Models
- Shared schemas in `coinbase-datatypes` using Lombok annotations
- Jackson annotations for JSON serialization
- Schema evolution support for backward compatibility

## Data Flow Pipeline

1. **Ingestion**: Coinbase WebSocket API → `coinbase-live-feed` → `persistent://feeds/realtime/coinbase-livefeed`
2. **Routing**: `coinbase-websocket-feed-router` distributes by channel type (ticker/auction/rfq_matches)
3. **Processing**: Flink jobs consume from topic-specific streams for transformations
4. **Storage**: `coinbase-ticker-sink` persists to MySQL, Grafana provides visualization

## Flink Job Configuration and Deployment

### Configuration System

All Flink jobs use a unified configuration system with the following precedence (highest to lowest):

1. **Command Line Parameters**: `--property.name=value`
2. **Environment Variables**: `PROPERTY_NAME=value`
3. **Configuration Files**: Properties files loaded from deployment-specific locations
4. **Default Values**: Embedded defaults for development

### Configuration File Locations

**Local Development (Docker Compose)**:
- File: `deployments/docker/config/pulsar.properties`
- Mount: `/opt/flink/conf/pulsar.properties`

**Kubernetes Deployment**:
- ConfigMap: `deployments/k8s/config/pulsar-configmap.yaml`
- Mount: `/etc/flink/config/pulsar.properties`

**StreamNative BYOC**:
- File: `deployments/sn-byoc/config/pulsar.properties`
- Environment variables override file values

### Core Configuration Properties

```properties
# Pulsar Connection
pulsar.service.url=pulsar://broker:6650
pulsar.admin.url=http://broker:8080
pulsar.auth.plugin=org.apache.pulsar.client.impl.auth.AuthenticationToken
pulsar.auth.params={"token":"jwt-token-here"}

# Topics (tenant/namespace prefixed automatically)
pulsar.tenant=feeds
pulsar.namespace=realtime
pulsar.topics.ticker=coinbase-ticker
pulsar.topics.moving.averages=moving-averages

# Job-specific parameters
time.window=5
output.path=/tmp/flink-output
```

### Running Flink Jobs

**With Command Line Parameters**:
```bash
java -cp flink-job.jar MainClass \
  --pulsar.service.url=pulsar://localhost:6650 \
  --time.window=10 \
  --output.path=/tmp/output
```

**With Environment Variables**:
```bash
export PULSAR_SERVICE_URL=pulsar://broker:6650
export PULSAR_AUTH_PARAMS='{"token":"jwt-token"}'
export TIME_WINDOW=5
java -cp flink-job.jar MainClass
```

**With Configuration File** (Docker Compose example):
```bash
# Configuration automatically loaded from /opt/flink/conf/pulsar.properties
docker exec flink-taskmanager flink run /opt/flink/jobs/moving-average.jar
```

### Deployment-Specific Configuration

**Docker Compose**:
```yaml
# docker-compose.yml
services:
  flink-taskmanager:
    volumes:
      - ./deployments/docker/config:/opt/flink/conf
```

**Kubernetes**:
```bash
# Deploy complete pipeline with ConfigMap
cd deployments/k8s
./deploy.sh

# Or apply components individually
kubectl apply -f config/pulsar-configmap.yaml
kubectl apply -f deployment.yaml  # Pulsar Functions
kubectl apply -f flink-jobs-deployment.yaml  # Flink Jobs
kubectl apply -f vvp-deployment.yaml  # VVP-managed jobs
```

**Pod Volume Configuration**:
```yaml
spec:
  containers:
  - name: flink-job
    env:
    - name: PULSAR_SERVICE_URL
      valueFrom:
        configMapKeyRef:
          name: pulsar-config
          key: pulsar.service.url
    volumeMounts:
    - name: config
      mountPath: /etc/flink/config
  volumes:
  - name: config
    configMap:
      name: pulsar-config
      items:
      - key: pulsar.properties
        path: pulsar.properties
```

**StreamNative BYOC**:
```bash
# Set environment variables in deployment
export PULSAR_SERVICE_URL="pulsar+ssl://broker.sn.dev:6651"
export PULSAR_AUTH_PARAMS='{"token":"'$JWT_TOKEN'"}'
```

### Authentication Configuration

**No Authentication** (Local Development):
```properties
# Leave auth properties empty or commented out
# pulsar.auth.plugin=
# pulsar.auth.params=
```

**JWT Token Authentication**:
```properties
pulsar.auth.plugin=org.apache.pulsar.client.impl.auth.AuthenticationToken
pulsar.auth.params={"token":"eyJhbGciOiJSUzI1NiIs..."}
```

**Environment Variable Authentication**:
```bash
export PULSAR_AUTH_PLUGIN=org.apache.pulsar.client.impl.auth.AuthenticationToken
export PULSAR_AUTH_PARAMS='{"token":"'$JWT_TOKEN'"}'
```

## Development Workflow

### Verification Commands

**Infrastructure Status**:
```bash
# Verify Docker infrastructure
docker ps

# Check Flink cluster status
docker exec -it flink-jobmanager ./bin/flink list

# View Flink Web UI
open http://localhost:8081
```

**Pulsar Monitoring**:
```bash
# Monitor live data ingestion
docker exec -it pulsar-broker ./bin/pulsar-client consume -n 0 -p Earliest -s test-sub persistent://feeds/realtime/coinbase-livefeed

# Check topic statistics and throughput
docker exec -it pulsar-broker ./bin/pulsar-admin topics stats persistent://feeds/realtime/coinbase-livefeed

# Validate Pulsar functions
docker exec -it pulsar-broker ./bin/pulsar-admin functions list --tenant public --namespace default
```

**Flink Job Monitoring**:
```bash
# List running Flink jobs
docker exec -it flink-jobmanager ./bin/flink list

# Submit a Flink job with custom configuration
docker exec -it flink-jobmanager ./bin/flink run \
  --class io.streamnative.coinbase.flink.aggregates.streams.AvgPriceByProductId \
  /opt/flink/jobs/coinbase-aggregations.jar \
  --time.window=10 \
  --output.path=/tmp/output

# Check job logs
docker logs flink-taskmanager
docker logs flink-jobmanager

# Cancel a running job
docker exec -it flink-jobmanager ./bin/flink cancel <JOB_ID>
```

**Configuration Validation**:
```bash
# Test configuration loading
docker exec -it flink-taskmanager cat /opt/flink/conf/pulsar.properties

# Verify environment variables
docker exec -it flink-taskmanager env | grep PULSAR

# Check Pulsar connectivity from Flink
docker exec -it flink-taskmanager ./bin/flink run \
  --class io.streamnative.flink.utils.ConfigurationProvider \
  /opt/flink/jobs/coinbase-flink-utils.jar
```

### Troubleshooting

**Common Configuration Issues**:
```bash
# Authentication failures
# Check: JWT token validity, auth plugin classpath, network connectivity

# Topic not found errors  
# Check: Topic creation, tenant/namespace permissions, topic naming

# Connection timeouts
# Check: Network connectivity, Pulsar broker status, firewall rules
```

**Debug Configuration Loading**:
```bash
# Enable debug logging for configuration
export FLINK_LOG_LEVEL=DEBUG

# Test configuration precedence
java -cp flink-job.jar MainClass --debug-config

# Validate specific property resolution
java -cp flink-job.jar MainClass --pulsar.service.url=test://debug
```

### Key Technologies
- Java 17 with Maven multi-module structure
- Apache Pulsar 4.0.5.x for messaging and Functions runtime
- Apache Flink 1.19.0 for stream processing (DataStream and Table APIs)
- Jackson 2.17.2 for JSON processing with JSR310 time module
- Lombok 1.18.30 for code generation
- Docker Compose for local orchestration