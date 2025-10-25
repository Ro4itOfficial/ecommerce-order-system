# Deployment Guide

## Prerequisites

### Local Development
- Java 21+
- Docker & Docker Compose
- Gradle 8.5+
- Git

### Production
- Kubernetes cluster (1.25+)
- Helm 3+
- kubectl configured
- Container registry access
- SSL certificates

## Quick Start (Local)

### 1. Clone Repository
```bash
git clone https://github.com/yourusername/ecommerce-order-system.git
cd ecommerce-order-system
```

### 2. Configure Environment
```bash
cp .env.example .env
# Edit .env with your configuration
```

### 3. Start with Docker Compose
```bash
# Start all services
make start

# Or manually
docker-compose up -d

# Check status
docker-compose ps
```

### 4. Verify Deployment
```bash
# Health check
curl http://localhost:8080/actuator/health

# API documentation
open http://localhost:8080/swagger-ui.html
```

## Development Deployment

### Build Application
```bash
# Clean build
./gradlew clean build

# Skip tests for faster build
./gradlew build -x test

# Run tests
./gradlew test integrationTest
```

### Run Locally
```bash
# Start dependencies
docker-compose up postgres redis -d

# Run application
./gradlew bootRun

# With specific profile
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Docker Build
```bash
# Build Docker image
docker build -t ecommerce-order-service:latest .

# Run container
docker run -p 8080:8080 \
  -e DB_HOST=host.docker.internal \
  -e REDIS_HOST=host.docker.internal \
  ecommerce-order-service:latest
```

## Production Deployment

### 1. Build and Push Image

```bash
# Set variables
export REGISTRY=ghcr.io
export IMAGE_NAME=yourusername/ecommerce-order-service
export VERSION=1.0.0

# Build image
docker build -t $REGISTRY/$IMAGE_NAME:$VERSION .
docker tag $REGISTRY/$IMAGE_NAME:$VERSION $REGISTRY/$IMAGE_NAME:latest

# Login to registry
docker login $REGISTRY

# Push image
docker push $REGISTRY/$IMAGE_NAME:$VERSION
docker push $REGISTRY/$IMAGE_NAME:latest
```

### 2. Kubernetes Deployment

#### Create Namespace
```bash
kubectl create namespace order-system
```

#### Create Secrets
```bash
# Database credentials
kubectl create secret generic order-db-secret \
  --from-literal=username=admin \
  --from-literal=password=your-secure-password \
  -n order-system

# JWT secret
kubectl create secret generic order-jwt-secret \
  --from-literal=secret=your-256-bit-secret-key \
  -n order-system
```

#### Deploy Database
```bash
# PostgreSQL
kubectl apply -f k8s/postgres-deployment.yaml -n order-system

# Redis
kubectl apply -f k8s/redis-deployment.yaml -n order-system

# Wait for ready
kubectl wait --for=condition=ready pod \
  -l app=postgres -n order-system --timeout=300s
```

#### Deploy Application
```bash
# Apply all manifests
kubectl apply -f k8s/deployment.yaml -n order-system

# Check deployment
kubectl get deployments -n order-system
kubectl get pods -n order-system
kubectl get services -n order-system
```

#### Configure Ingress
```bash
# Install cert-manager for SSL
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# Apply ingress
kubectl apply -f k8s/ingress.yaml -n order-system
```

### 3. Helm Deployment (Alternative)

```bash
# Add repo
helm repo add bitnami https://charts.bitnami.com/bitnami

# Install PostgreSQL
helm install postgres bitnami/postgresql \
  --set auth.postgresPassword=secret \
  --set auth.database=orderdb \
  -n order-system

# Install Redis
helm install redis bitnami/redis \
  --set auth.enabled=false \
  -n order-system

# Install application
helm install order-service ./helm/order-service \
  --set image.tag=$VERSION \
  -n order-system
```

## Cloud Deployments

### AWS EKS
```bash
# Create cluster
eksctl create cluster --name order-cluster --region us-east-1

# Deploy application
kubectl apply -f k8s/deployment.yaml

# Create load balancer
kubectl expose deployment order-service \
  --type=LoadBalancer --port=80 --target-port=8080
```

### Google GKE
```bash
# Create cluster
gcloud container clusters create order-cluster \
  --zone us-central1-a \
  --num-nodes 3

# Get credentials
gcloud container clusters get-credentials order-cluster

# Deploy
kubectl apply -f k8s/
```

### Azure AKS
```bash
# Create resource group
az group create --name order-rg --location eastus

# Create cluster
az aks create --resource-group order-rg \
  --name order-cluster \
  --node-count 3

# Get credentials
az aks get-credentials --resource-group order-rg \
  --name order-cluster

# Deploy
kubectl apply -f k8s/
```

## Database Migration

### Flyway Migrations
```bash
# Run migrations
./gradlew flywayMigrate

# Clean database (CAUTION: Drops all objects)
./gradlew flywayClean

# Validate migrations
./gradlew flywayValidate

# Migration info
./gradlew flywayInfo
```

### Manual Migration
```bash
# Connect to database
docker exec -it order-postgres psql -U admin -d orderdb

# Run migration scripts
\i /migrations/V1__Initial_schema.sql
\i /migrations/V2__Seed_initial_data.sql
```

## Monitoring Setup

### Prometheus
```bash
# Deploy Prometheus
kubectl apply -f monitoring/prometheus-deployment.yaml

# Port forward
kubectl port-forward svc/prometheus 9090:9090
```

### Grafana
```bash
# Deploy Grafana
kubectl apply -f monitoring/grafana-deployment.yaml

# Port forward
kubectl port-forward svc/grafana 3000:3000

# Default credentials: admin/admin
```

### Import Dashboards
1. Login to Grafana
2. Go to Dashboards → Import
3. Upload JSON files from `monitoring/grafana/dashboards/`

## SSL/TLS Configuration

### Using Let's Encrypt
```yaml
# cert-manager ClusterIssuer
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@ecommerce.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
```

### Custom Certificates
```bash
# Create TLS secret
kubectl create secret tls order-tls \
  --cert=path/to/tls.crt \
  --key=path/to/tls.key \
  -n order-system
```

## Backup & Restore

### Database Backup
```bash
# Backup
docker exec order-postgres pg_dump -U admin orderdb > backup.sql

# Scheduled backup
kubectl apply -f k8s/cronjob-backup.yaml
```

### Database Restore
```bash
# Restore
docker exec -i order-postgres psql -U admin orderdb < backup.sql
```

## Scaling

### Horizontal Scaling
```bash
# Manual scaling
kubectl scale deployment order-service --replicas=5 -n order-system

# Autoscaling
kubectl autoscale deployment order-service \
  --min=2 --max=10 --cpu-percent=70 -n order-system
```

### Vertical Scaling
```yaml
# Update resources in deployment.yaml
resources:
  requests:
    memory: "1Gi"
    cpu: "500m"
  limits:
    memory: "2Gi"
    cpu: "1000m"
```

## Rolling Updates

### Update Application
```bash
# Update image
kubectl set image deployment/order-service \
  order-service=$REGISTRY/$IMAGE_NAME:$NEW_VERSION \
  -n order-system

# Check rollout status
kubectl rollout status deployment/order-service -n order-system

# Rollback if needed
kubectl rollout undo deployment/order-service -n order-system
```

## Troubleshooting

### Check Logs
```bash
# Application logs
kubectl logs -f deployment/order-service -n order-system

# Previous container logs
kubectl logs deployment/order-service --previous -n order-system

# All pods
kubectl logs -l app=order-service -n order-system
```

### Debug Pod
```bash
# Describe pod
kubectl describe pod <pod-name> -n order-system

# Execute command in pod
kubectl exec -it <pod-name> -n order-system -- /bin/sh

# Port forward for debugging
kubectl port-forward <pod-name> 8080:8080 -n order-system
```

### Common Issues

#### Pod CrashLoopBackOff
```bash
# Check logs
kubectl logs <pod-name> -n order-system

# Check events
kubectl get events -n order-system --sort-by='.lastTimestamp'
```

#### Database Connection Issues
```bash
# Test connection from pod
kubectl exec -it <pod-name> -n order-system -- \
  nc -zv postgres-service 5432
```

#### Out of Memory
```bash
# Check resource usage
kubectl top pods -n order-system

# Increase memory limits
kubectl edit deployment order-service -n order-system
```

## Health Checks

### Kubernetes Probes
```bash
# Check liveness
kubectl exec <pod-name> -n order-system -- \
  curl localhost:8080/actuator/health/liveness

# Check readiness
kubectl exec <pod-name> -n order-system -- \
  curl localhost:8080/actuator/health/readiness
```

## Cleanup

### Remove Kubernetes Resources
```bash
# Delete namespace (removes everything)
kubectl delete namespace order-system

# Or delete specific resources
kubectl delete deployment,service,ingress -l app=order-service -n order-system
```

### Docker Cleanup
```bash
# Stop and remove containers
docker-compose down -v

# Remove images
docker rmi ecommerce-order-service:latest

# System prune
docker system prune -af
```

## CI/CD Pipeline

### GitHub Actions
- Push to main branch triggers CI pipeline
- Successful CI triggers CD pipeline
- Automatic deployment to staging
- Manual approval for production

### Jenkins Pipeline
```groovy
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh './gradlew build'
            }
        }
        stage('Test') {
            steps {
                sh './gradlew test'
            }
        }
        stage('Docker Build') {
            steps {
                sh 'docker build -t order-service:${BUILD_NUMBER} .'
            }
        }
        stage('Deploy') {
            steps {
                sh 'kubectl apply -f k8s/'
            }
        }
    }
}
```

## Performance Tuning

### JVM Options
```bash
JAVA_OPTS="-Xms1g -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \
  -XX:+UseStringDeduplication \
  -Djava.security.egd=file:/dev/./urandom"
```

### Database Tuning
```sql
-- Increase connections
ALTER SYSTEM SET max_connections = 200;

-- Optimize memory
ALTER SYSTEM SET shared_buffers = '256MB';
ALTER SYSTEM SET effective_cache_size = '1GB';
```

## Security Hardening

### Network Policies
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: order-service-netpol
spec:
  podSelector:
    matchLabels:
      app: order-service
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: nginx
    ports:
    - port: 8080
```

### Pod Security Policy
```yaml
apiVersion: policy/v1beta1
kind: PodSecurityPolicy
metadata:
  name: restricted
spec:
  privileged: false
  runAsUser:
    rule: MustRunAsNonRoot
  seLinux:
    rule: RunAsAny
  fsGroup:
    rule: RunAsAny
```

## Support

- **Documentation**: https://docs.ecommerce.com
- **Issues**: https://github.com/yourusername/ecommerce-order-system/issues
- **Slack**: #order-service-support
- **Email**: devops@ecommerce.com