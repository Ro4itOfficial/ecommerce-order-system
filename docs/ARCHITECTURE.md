# Architecture Documentation

## System Architecture Overview

The E-Commerce Order Processing System is built using a microservices architecture with Spring Boot, implementing Domain-Driven Design (DDD) principles and following SOLID design patterns.

## Technology Stack

### Core Technologies
- **Java 21**: Latest LTS version with virtual threads and pattern matching
- **Spring Boot 3.2**: Framework for building microservices
- **Spring Security**: JWT-based authentication and authorization
- **Spring Data JPA**: ORM for database operations
- **PostgreSQL 15**: Primary database
- **Redis 7**: Caching and rate limiting
- **Gradle 8.7**: Build automation

### Caching Strategy
- **Caffeine**: L1 cache (in-memory, local)
- **Redis**: L2 cache (distributed)
- **Cache-aside pattern**: For read-heavy operations
- **Write-through pattern**: For critical data consistency

### Monitoring & Observability
- **Prometheus**: Metrics collection
- **Grafana**: Metrics visualization
- **Micrometer**: Application metrics
- **SLF4J + Logback**: Structured logging
- **Zipkin**: Distributed tracing

## Architecture Patterns

### 1. Layered Architecture
```
┌─────────────────────────────────┐
│     Presentation Layer          │  REST Controllers, DTOs
├─────────────────────────────────┤
│      Service Layer              │  Business Logic
├─────────────────────────────────┤
│     Repository Layer            │  Data Access
├─────────────────────────────────┤
│      Database Layer             │  PostgreSQL, Redis
└─────────────────────────────────┘
```

### 2. Design Patterns Implemented

#### Builder Pattern
- Order creation with complex initialization
- Fluent API for object construction

#### Repository Pattern
- Abstraction over data persistence
- Enables easy switching between databases

#### Factory Pattern
- Order creation based on different types
- Extensible for new order types

#### Strategy Pattern
- Different order processing strategies
- Payment method strategies

#### Observer Pattern
- Event-driven architecture
- Order status change notifications

#### Circuit Breaker Pattern
- Fault tolerance with Resilience4j
- Prevents cascading failures

## Domain Model

### Core Entities

#### Order
- Aggregate root for order management
- Contains order items, status, customer info
- Implements business rules for status transitions

#### OrderItem
- Value object within Order aggregate
- Represents individual products in an order

#### User
- Authentication and authorization entity
- Implements UserDetails for Spring Security

### Status Flow
```
PENDING → PROCESSING → SHIPPED → DELIVERED
    ↓         ↓           
CANCELLED  CANCELLED    
```

## Security Architecture

### Authentication Flow
1. User sends credentials to `/api/auth/login`
2. Server validates credentials
3. JWT tokens generated (access + refresh)
4. Tokens stored in Redis with TTL
5. Client includes token in Authorization header
6. JwtAuthenticationFilter validates token
7. SecurityContext populated with user details

### Rate Limiting
- Redis-based sliding window algorithm
- Per-user and per-IP limits
- Configurable time windows
- Headers indicate rate limit status

## Data Flow

### Order Creation Flow
```
Client Request
    ↓
Rate Limiter → JWT Filter → Controller
    ↓
Validation → Service Layer
    ↓
Business Logic → Repository
    ↓
Database → Cache Update
    ↓
Event Publishing → Response
```

### Caching Strategy
```
Request → Check Caffeine (L1)
    ↓ (miss)
Check Redis (L2)
    ↓ (miss)
Database Query
    ↓
Update Redis → Update Caffeine
    ↓
Return Response
```

## Database Design

### Tables
- `users`: User accounts and authentication
- `user_roles`: User role assignments
- `orders`: Order information
- `order_items`: Individual items in orders

### Indexes
- Customer ID for order lookups
- Status for filtering
- Created date for time-based queries
- Composite indexes for complex queries

## API Design

### RESTful Endpoints
- Resource-based URLs
- HTTP methods for operations
- Status codes for responses
- HATEOAS principles

### Versioning Strategy
- URL versioning (`/api/v1/`)
- Backward compatibility maintained
- Deprecation notices in headers

## Scalability Considerations

### Horizontal Scaling
- Stateless application design
- Session storage in Redis
- Database connection pooling
- Load balancer ready

### Performance Optimizations
- Lazy loading for relationships
- Batch operations for bulk updates
- Pagination for list endpoints
- Query optimization with indexes

### Caching Layers
1. **Browser Cache**: Static resources
2. **CDN**: API responses (future)
3. **Caffeine**: Application-level cache
4. **Redis**: Distributed cache
5. **Database**: Query result cache

## Fault Tolerance

### Circuit Breaker Configuration
- Failure threshold: 50%
- Wait duration: 10s in open state
- Sliding window: 10 calls
- Automatic recovery

### Retry Mechanism
- Max attempts: 3
- Exponential backoff
- Configurable retry exceptions

### Fallback Strategies
- Cached data for read operations
- Default responses for failures
- Graceful degradation

## Deployment Architecture

### Docker Containers
```
┌──────────────────────────────────────┐
│         Docker Network                │
├──────────────────────────────────────┤
│  ┌────────┐  ┌────────┐  ┌────────┐ │
│  │  App   │  │ Redis  │  │Postgres│ │
│  └────────┘  └────────┘  └────────┘ │
│  ┌────────┐  ┌────────┐  ┌────────┐ │
│  │Grafana │  │Prometh.│  │ Zipkin │ │
│  └────────┘  └────────┘  └────────┘ │
└──────────────────────────────────────┘
```

### Kubernetes Architecture
- Deployments with 3 replicas
- HorizontalPodAutoscaler
- ConfigMaps for configuration
- Secrets for sensitive data
- Ingress for external access
- Services for internal communication

## Monitoring & Alerting

### Metrics Collected
- JVM metrics (memory, GC, threads)
- HTTP metrics (request rate, latency, errors)
- Business metrics (orders created, cancelled)
- Cache metrics (hit rate, evictions)
- Database metrics (connection pool, query time)

### Health Checks
- Liveness probe: Application running
- Readiness probe: Ready for traffic
- Custom health indicators
- Dependency health checks

## Security Measures

### Application Security
- JWT token expiration
- Refresh token rotation
- Password encryption (BCrypt)
- SQL injection prevention
- XSS protection
- CORS configuration

### Infrastructure Security
- HTTPS enforcement
- Network policies
- Secret management
- Container scanning
- Dependency scanning

## Future Enhancements

### Planned Features
1. Event Sourcing for order history
2. CQRS for read/write separation
3. GraphQL API support
4. WebSocket for real-time updates
5. Message queue integration (RabbitMQ/Kafka)
6. Multi-tenancy support
7. A/B testing framework
8. Advanced analytics

### Performance Improvements
1. Database sharding
2. Read replicas
3. Elasticsearch for search
4. CDN integration
5. Response compression
6. Connection multiplexing

## Development Guidelines

### Code Organization
- Package by feature
- Clear separation of concerns
- Dependency injection
- Interface-based design

### Testing Strategy
- Unit tests: 80% coverage minimum
- Integration tests: Critical paths
- Contract tests: API contracts
- Performance tests: Load testing
- Security tests: Vulnerability scanning

### Documentation
- OpenAPI/Swagger for APIs
- JavaDoc for public methods
- README files for modules
- Architecture Decision Records (ADR)

## Troubleshooting Guide

### Common Issues
1. **High Memory Usage**
   - Check cache sizes
   - Review heap settings
   - Analyze memory dumps

2. **Slow Queries**
   - Check missing indexes
   - Review N+1 problems
   - Analyze query plans

3. **Connection Pool Exhaustion**
   - Review pool settings
   - Check for connection leaks
   - Monitor active connections

4. **Rate Limiting Issues**
   - Check Redis connectivity
   - Review window settings
   - Monitor key expiration

## Contact & Support

- **Team Lead**: tech-lead@ecommerce.com
- **DevOps**: devops@ecommerce.com
- **On-Call**: oncall@ecommerce.com
- **Documentation**: https://docs.ecommerce.com