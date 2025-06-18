# Fever Plans API

A Spring Boot + Kotlin implementation of the Fever Challenge using Hexagonal Architecture and DDD patterns.

## Architecture Overview

This project implements a modular monolith with:

- **Hexagonal Architecture** with separation of concerns
- **Domain-Driven Design (DDD)**
- **CQRS pattern** separating sync operations from query operations
- **Enterprise resilience patterns** (Circuit Breaker, Retry, TimeLimiter)

### Modules Structure

```
evens-api-kotlin-spring/
├── sync/          # Plan synchronization bounded context
├── query/         # Plan search bounded context  
├── shared/        # Shared domain kernel and infrastructure
└── config/        # Application configuration
```

## Quick Start

### Prerequisites

- **Java 17+**
- **Gradle 8.x**

### Running the Application

```bash
# Build and run the application
./gradlew bootRun

# Or using the Makefile
make run
```

The application will start on **http://localhost:8080**

### Using the API

**Main endpoint:**

```bash
curl "http://localhost:8080/search?starts_at=2021-02-01&ends_at=2022-07-03"
```

**Response format:**
```json
{
  "error": null,
  "data": {
    "events": [
      {
        "id": "evt-291-demo",
        "title": "Camela en concierto", 
        "start_date": "2021-06-30",
        "start_time": "21:00:00",
        "end_date": "2021-06-30",
        "end_time": "22:00:00",
        "min_price": 15.00,
        "max_price": 30.00
      }
    ]
  }
}
```

## Configuration

### Application Profiles

The application is configured in the `application.yml` Spring configuration file:

```yaml
spring:
  profiles:
    active: dev
  
# H2 Database (Development)
spring:
  datasource:
    url: jdbc:h2:file:./data/plans
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: update
```

### Resilience Configuration

Resilience patterns are configured via `application.yml` as:

#### Circuit Breaker
```yaml
resilience4j:
  circuitbreaker:
    instances:
      providerApi:
        failureRateThreshold: 50          # Open if 50% fail
        slowCallDurationThreshold: 2s     # Calls >2s are "slow"  
        slowCallRateThreshold: 50         # Open if 50% are slow
        minimumNumberOfCalls: 5           # Need 5 calls to calculate rates
        slidingWindowSize: 10             # Look at last 10 calls
        waitDurationInOpenState: 30s      # Stay open for 30s
```

#### Retry Mechanism
```yaml
resilience4j:
  retry:
    instances:
      providerApi:
        maxAttempts: 3                    # Total retry attempts
        waitDuration: 500ms               # Initial wait time
        enableExponentialBackoff: true    # 500ms -> 1s -> 2s
        exponentialBackoffMultiplier: 2.0
```

#### TimeLimiter
```yaml
resilience4j:
  timelimiter:
    instances:
      providerApi:
        timeoutDuration: 8s               # Maximum call duration
        cancelRunningFuture: true         # Cancel on timeout
```

### Scheduled Sync Configuration

Background synchronization is configured via:

```yaml
fever:
  sync:
    interval: 30000                       # Sync every 30 seconds
    batch-size: 100                       # Process 100 plans per batch
    enabled: true                         # Enable/disable sync
  
  provider:
    url: "https://provider.code-challenge.feverup.com/api/events"
    timeout: 8000                         # 8 second timeout
```

## Testing

### Running Tests

```bash
# Run all tests
./gradlew test

# Run tests with coverage
./gradlew test jacocoTestReport

# Run only unit tests (fast)
./gradlew test --tests "*Test" --exclude-task integrationTest

# Run only integration tests
./gradlew integrationTest
```

### Sample Test Commands

```bash
# Test domain logic only (fast)
./gradlew test --tests "*.domain.*"

# Test infrastructure adapters
./gradlew test --tests "*.infrastructure.*"  

# Test API endpoints
./gradlew test --tests "*Controller*"

# Test resilience patterns
./gradlew test --tests "*Resilience*"
```

## Monitoring & Health Checks

### Spring Actuator Endpoints

**Health Check:**
```bash
curl http://localhost:8080/actuator/health
```

**Circuit Breaker Status:**
```bash
curl http://localhost:8080/actuator/circuitbreakers
```

**Application Metrics:**
```bash  
curl http://localhost:8080/actuator/metrics
```

### Database Console (Development)

Access H2 console at: **http://localhost:8080/h2-console**

- **JDBC URL**: `jdbc:h2:file:./data/plans`
- **Username**: `sa`
- **Password**: *(empty)*

## Architecture Details

### Hexagonal Architecture Layers

```
┌─────────────────────────────────────────┐
│           Web Layer                     │
│        (REST Controllers)               │
├─────────────────────────────────────────┤
│        Application Layer                │
│         (Use Cases/Services)            │
├─────────────────────────────────────────┤
│         Domain Layer                    │
│    (Business Logic & Rules)             │
├─────────────────────────────────────────┤
│      Infrastructure Layer               │
│  (Database, External APIs, Config)      │
└─────────────────────────────────────────┘
```

### Key Design Patterns Implemented

1. **Repository Pattern**: Domain-driven data access through ports
2. **Circuit Breaker**: Fail-fast for external service degradation
3. **Observer Pattern**: Domain events for cross-cutting concerns
4. **CQRS**: Separate models for command and query operations
5. **Dependency Inversion**: All dependencies point inward to domain

### Module Boundaries

- **Sync Module**: Handles plan synchronization from external provider (writes)
- **Query Module**: Optimized read operations for search API
- **Shared Module**: Common domain objects and infrastructure

## Circuit Breaker Response Time Solution

**Problem**: External provider becomes slow (10+ seconds per call)

**Solution**:
1. **Initial calls**: TimeLimiter cuts calls at 8s instead of waiting forever
2. **Circuit Breaker**: Detects pattern of slow calls (>50% slow)
3. **Circuit opens**: Blocks further calls to external provider
4. **Result**: Fast responses (sub-100ms) for subsequent calls

## API Documentation

### OpenAPI/Swagger

Interactive API documentation available at:
**http://localhost:8080/swagger-ui.html**

### Main Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/search` | Search plans by date range |
| `GET` | `/actuator/health` | Application health status |
| `GET` | `/actuator/metrics` | Application metrics |
| `GET` | `/h2-console` | Database console (dev only) |

## Key Business Features

### Plan Synchronization
- **Automated sync** from external provider every 30 seconds
- **Resilient sync** with retry logic and circuit breaker protection
- **Batch processing** for efficient data handling
- **Optimistic locking** with database-managed concurrency control
- **Coroutines** for efficient non-blocking I/O operations

### Plan Search
- **Fast search** by date range with sub-200ms response times
- **Input validation** for date formats and ranges
- **Error handling** with meaningful error codes
- **Optional filtering** by availability status

### Operational Features
- **Health monitoring** through Spring Actuator
- **Performance metrics** collection and monitoring
- **Graceful degradation** when external services fail
- **Comprehensive logging** for debugging and monitoring

## Data Flow

### Sync Flow
```
External Provider → Circuit Breaker → Retry Logic → TimeLimiter → 
Domain Service → Repository → Database
```

### Query Flow
```
HTTP Request → Controller → Validation → Use Case → 
Query Port → Database → Response Mapping → JSON Response
```

## Assumptions Made

1. **Provider API Stability**: External provider may have intermittent slowness/failures
2. **Data Volume**: Moderate data volume suitable for single database
3. **Query Patterns**: Primarily date-range based searches
4. **Availability Requirements**: High availability preferred over strict consistency
5. **Database Choice**: H2 for development, easily replaceable with PostgreSQL/MySQL for production

## Implementation Notes

### Domain Event Publishing System

#### SpringDomainEventPublisher
**Implementation**: Functional Spring ApplicationEvent-based publisher
**Current State**: Architecture ready for scaling requirements

The `SpringDomainEventPublisher` implements proper domain event handling through Spring's event mechanism.
The implementation follows the port/adapter pattern with `DomainEventPublisher` as the domain port, allowing
infrastructure changes without affecting domain logic.

### Cache Infrastructure

#### CacheInvalidationHandler
**Implementation**: Event-driven invalidation with integration points
**Current State**: Framework established, cache provider integration deferred

The handler responds to domain events (`PlanSyncedEvent`, `PlanUpdatedEvent`) with proper invalidation logic.
Comments indicate "Here could be integrated with Redis/Caffeine cache - Skipped due to time constraint."
The architectural foundation supports cache integration without domain coupling.

### Metrics and Observability

#### SyncMetricsHandler
**Implementation**: Structured event logging with metrics hooks
**Current State**: Observability patterns established, metrics provider integration deferred

The handler captures business events with structured logging. Integration points are documented: "Here can integrate with
Micrometer metrics - Skipped due to time constraints." The event-driven approach ensures metrics collection doesn't impact
domain logic.
