# E-Commerce Order Processing System

A production-ready microservices-based order processing system built with Spring Boot 3, Java 21, featuring JWT authentication, rate limiting, caching, circuit breakers, and comprehensive monitoring.

## 🌟 Features

- ✅ RESTful API with JWT Authentication
- ✅ Rate Limiting (Redis Sliding Window Algorithm)
- ✅ Dual Caching Strategy (Caffeine + Redis)
- ✅ Circuit Breakers (Resilience4j)
- ✅ Database (PostgreSQL with Flyway migrations)
- ✅ Monitoring (Prometheus + Grafana)
- ✅ API Documentation (OpenAPI 3.0/Swagger)
- ✅ Docker & Docker Compose
- ✅ Health Checks & Readiness Probes
- ✅ Distributed Tracing (Zipkin)
- ✅ Comprehensive Testing (Unit + Integration)

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose (for running)
- Java 21+ (for development)
- Gradle 8+ (for development)

### One-Command Setup

```bash
# Clone the repository
git clone https://github.com/yourusername/ecommerce-order-system.git
cd ecommerce-order-system

# Start all services
make start

# Or using docker-compose directly
docker-compose up -d

# The application will be available at http://localhost:8080
```

### Access Points

| Service | URL | Credentials |
|---------|-----|-------------|
| API | http://localhost:8080 | Use JWT token |
| Swagger UI | http://localhost:8080/swagger-ui.html | - |
| Grafana | http://localhost:3000 | admin/admin |
| Prometheus | http://localhost:9090 | - |
| PostgreSQL | localhost:5432 | admin/secret |
| Redis | localhost:6379 | - |

## 📝 API Documentation

### Authentication

1. **Register User**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Password123!",
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User"
  }'
```

2. **Login**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Password123!"
  }'
```

Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1...",
  "refreshToken": "eyJhbGciOiJIUzI1...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### Order Operations

1. **Create Order**
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST123",
    "items": [
      {
        "productId": "PROD001",
        "productName": "Laptop",
        "quantity": 1,
        "unitPrice": 999.99
      }
    ]
  }'
```

2. **Get Order**
```bash
curl -X GET http://localhost:8080/api/v1/orders/{orderId} \
  -H "Authorization: Bearer <your-jwt-token>"
```

3. **List Orders**
```bash
curl -X GET http://localhost:8080/api/v1/orders?status=PENDING&page=0&size=10 \
  -H "Authorization: Bearer <your-jwt-token>"
```

4. **Update Order Status**
```bash
curl -X PATCH http://localhost:8080/api/v1/orders/{orderId}/status \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "PROCESSING"
  }'
```

5. **Cancel Order**
```bash
curl -X POST http://localhost:8080/api/v1/orders/{orderId}/cancel \
  -H "Authorization: Bearer <your-jwt-token>"
```

## 🏗️ Architecture

The system follows a microservices architecture with the following components:

- **API Gateway**: Nginx for load balancing and routing
- **Authentication Service**: JWT-based authentication
- **Order Service**: Core business logic for order processing
- **Caching Layer**: Dual caching with Caffeine (L1) and Redis (L2)
- **Database**: PostgreSQL with connection pooling
- **Monitoring**: Prometheus metrics and Grafana dashboards

See [ARCHITECTURE.md](docs/ARCHITECTURE.md) for detailed architecture documentation.

## 🧪 Testing

```bash
# Run unit tests
./gradlew test

# Run integration tests
./gradlew integrationTest

# Run all tests with coverage
./gradlew test jacocoTestReport

# View test coverage report
open build/reports/jacoco/test/html/index.html
```

## 📊 Monitoring

### Grafana Dashboards
Access at http://localhost:3000 (admin/admin)

Available dashboards:
- Order Processing Metrics
- API Performance Dashboard
- JVM Metrics
- Rate Limiting Statistics
- Cache Hit/Miss Ratios

### Health Endpoints
```bash
# Health check
curl http://localhost:8080/actuator/health

# Detailed metrics
curl http://localhost:8080/actuator/metrics

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus
```

## 🔧 Configuration

Configuration can be customized via environment variables or `.env` file:

```env
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=orderdb
DB_USER=admin
DB_PASSWORD=secret

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT
JWT_SECRET=your-256-bit-secret-key-for-jwt-token-generation
JWT_EXPIRATION=3600
JWT_REFRESH_EXPIRATION=86400

# Rate Limiting
RATE_LIMIT_REQUESTS_PER_MINUTE=100
RATE_LIMIT_WINDOW_SIZE_MINUTES=1

# Caching
CAFFEINE_CACHE_MAX_SIZE=1000
CAFFEINE_CACHE_EXPIRE_MINUTES=5

# Monitoring
METRICS_ENABLED=true
TRACING_ENABLED=true
```

## 📦 Development

### Building the Project
```bash
# Build without tests
./gradlew build -x test

# Build with tests
./gradlew build

# Run locally (development mode)
./gradlew bootRun

# Run with specific profile
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Docker Build
```bash
# Build Docker image
docker build -t ecommerce-order-service:latest .

# Run with docker-compose
docker-compose up --build
```

## 🤝 Contributing

Please read [CONTRIBUTING.md](docs/CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## 📚 Documentation

- [Architecture Guide](docs/ARCHITECTURE.md)
- [API Documentation](docs/API.md)
- [Deployment Guide](docs/DEPLOYMENT.md)
- [Security Guide](docs/SECURITY.md)
- [Monitoring Guide](docs/MONITORING.md)
- [Troubleshooting](docs/TROUBLESHOOTING.md)

## 🔒 Security

- JWT-based authentication with refresh tokens
- Rate limiting to prevent abuse
- Input validation and sanitization
- SQL injection prevention with parameterized queries
- XSS protection
- CORS configuration

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Authors

- Rohit NEGI - Initial work

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- Resilience4j for circuit breaker implementation
- Caffeine for high-performance caching
- All contributors who help improve this project