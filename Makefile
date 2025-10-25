.PHONY: help build start stop restart logs clean test test-integration test-all dev prod health

# Colors for output
GREEN := \033[0;32m
YELLOW := \033[1;33m
RED := \033[0;31m
NC := \033[0m # No Color

# Default target
help:
	@echo "${GREEN}Available commands:${NC}"
	@echo "  ${YELLOW}make build${NC}         - Build the application and Docker images"
	@echo "  ${YELLOW}make start${NC}         - Start all services"
	@echo "  ${YELLOW}make stop${NC}          - Stop all services"
	@echo "  ${YELLOW}make restart${NC}       - Restart all services"
	@echo "  ${YELLOW}make logs${NC}          - Show logs from all services"
	@echo "  ${YELLOW}make clean${NC}         - Clean up everything (containers, volumes, images)"
	@echo "  ${YELLOW}make test${NC}          - Run unit tests"
	@echo "  ${YELLOW}make test-integration${NC} - Run integration tests"
	@echo "  ${YELLOW}make test-all${NC}      - Run all tests with coverage"
	@echo "  ${YELLOW}make dev${NC}           - Start in development mode"
	@echo "  ${YELLOW}make prod${NC}          - Start in production mode"
	@echo "  ${YELLOW}make health${NC}        - Check health of all services"
	@echo "  ${YELLOW}make db-migrate${NC}    - Run database migrations"
	@echo "  ${YELLOW}make db-seed${NC}       - Seed database with sample data"

# Build the application
build:
	@echo "${GREEN}Building application...${NC}"
	./gradlew clean build -x test
	@echo "${GREEN}Building Docker images...${NC}"
	docker-compose build --no-cache
	@echo "${GREEN}Build completed!${NC}"

# Start all services
start:
	@echo "${GREEN}Starting all services...${NC}"
	docker-compose up -d
	@echo "${GREEN}Waiting for services to be ready...${NC}"
	@sleep 10
	@echo "${GREEN}Services started!${NC}"
	@echo "${YELLOW}Application: http://localhost:8080${NC}"
	@echo "${YELLOW}Swagger UI: http://localhost:8080/swagger-ui.html${NC}"
	@echo "${YELLOW}Grafana: http://localhost:3000 (admin/admin)${NC}"
	@echo "${YELLOW}Prometheus: http://localhost:9090${NC}"

# Stop all services
stop:
	@echo "${RED}Stopping all services...${NC}"
	docker-compose down
	@echo "${GREEN}Services stopped!${NC}"

# Restart all services
restart: stop start

# Show logs
logs:
	docker-compose logs -f --tail=100

# Show logs for specific service
logs-service:
	docker-compose logs -f order-service --tail=100

# Clean up everything
clean:
	@echo "${RED}Cleaning up everything...${NC}"
	docker-compose down -v
	docker system prune -f
	./gradlew clean
	@echo "${GREEN}Cleanup completed!${NC}"

# Run unit tests
test:
	@echo "${GREEN}Running unit tests...${NC}"
	./gradlew test
	@echo "${GREEN}Test report: build/reports/tests/test/index.html${NC}"

# Run integration tests
test-integration:
	@echo "${GREEN}Running integration tests...${NC}"
	./gradlew integrationTest
	@echo "${GREEN}Test report: build/reports/tests/integrationTest/index.html${NC}"

# Run all tests with coverage
test-all:
	@echo "${GREEN}Running all tests with coverage...${NC}"
	./gradlew test integrationTest jacocoTestReport
	@echo "${GREEN}Coverage report: build/reports/jacoco/test/html/index.html${NC}"

# Start in development mode
dev:
	@echo "${GREEN}Starting in development mode...${NC}"
	docker-compose up postgres redis -d
	@sleep 5
	./gradlew bootRun --args='--spring.profiles.active=dev'

# Start in production mode
prod:
	@echo "${GREEN}Starting in production mode...${NC}"
	docker-compose --profile prod up -d
	@echo "${GREEN}Production environment started!${NC}"

# Start with debug tools (pgAdmin, Redis Commander)
debug:
	@echo "${GREEN}Starting with debug tools...${NC}"
	docker-compose --profile debug up -d
	@echo "${YELLOW}pgAdmin: http://localhost:5050 (admin@ecommerce.com/admin)${NC}"
	@echo "${YELLOW}Redis Commander: http://localhost:8081${NC}"

# Check health of all services
health:
	@echo "${GREEN}Checking health of services...${NC}"
	@curl -s http://localhost:8080/actuator/health | jq '.' || echo "${RED}Order Service: Not responding${NC}"
	@docker-compose ps
	@echo "${GREEN}Health check completed!${NC}"

# Run database migrations
db-migrate:
	@echo "${GREEN}Running database migrations...${NC}"
	docker exec order-service ./gradlew flywayMigrate
	@echo "${GREEN}Migrations completed!${NC}"

# Seed database with sample data
db-seed:
	@echo "${GREEN}Seeding database...${NC}"
	docker exec -it order-postgres psql -U admin -d orderdb -f /docker-entrypoint-initdb.d/init.sql
	@echo "${GREEN}Database seeded!${NC}"

# Connect to PostgreSQL
db-connect:
	docker exec -it order-postgres psql -U admin -d orderdb

# Connect to Redis
redis-connect:
	docker exec -it order-redis redis-cli

# Build JAR only
jar:
	@echo "${GREEN}Building JAR file...${NC}"
	./gradlew clean bootJar
	@echo "${GREEN}JAR file created: build/libs/*.jar${NC}"

# Run SonarQube analysis
sonar:
	@echo "${GREEN}Running SonarQube analysis...${NC}"
	./gradlew sonarqube
	@echo "${GREEN}SonarQube analysis completed!${NC}"

# Format code
format:
	@echo "${GREEN}Formatting code...${NC}"
	./gradlew spotlessApply
	@echo "${GREEN}Code formatted!${NC}"

# Check code style
checkstyle:
	@echo "${GREEN}Checking code style...${NC}"
	./gradlew checkstyleMain checkstyleTest
	@echo "${GREEN}Code style check completed!${NC}"

# Generate API documentation
api-docs:
	@echo "${GREEN}Generating API documentation...${NC}"
	./gradlew generateOpenApiDocs
	@echo "${GREEN}API docs generated: build/openapi/openapi.json${NC}"

# Docker compose with specific service
up-%:
	docker-compose up -d $*

# Docker compose logs for specific service
logs-%:
	docker-compose logs -f $* --tail=100

# Stop specific service
stop-%:
	docker-compose stop $*

# Restart specific service
restart-%:
	docker-compose restart $*