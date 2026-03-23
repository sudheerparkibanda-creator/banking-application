# Docker and Postman Setup Guide

## Overview
This guide covers Docker containerization and Postman API testing for the Banking Microservices Application.

### Technology Stack
- **Spring Boot**: 4.0.4
- **Spring Cloud**: 2025.1.0
- **Java**: 17
- **OpenTelemetry**: Distributed tracing
- **Resilience4j**: Circuit breaker pattern
- **Service Discovery**: Eureka
- **API Gateway**: Spring Cloud Gateway

---

## Docker Setup

### Dockerfiles Overview
All services use **multi-stage builds** for optimized images:
- **Build Stage**: Extracts JAR and explodes dependencies
- **Runtime Stage**: Contains only necessary runtime files
- **Security**: Runs as non-root user (appuser:1000)
- **Health Checks**: Built-in health endpoint verification
- **JVM Optimization**: G1GC garbage collector, 75% max heap

### Services and Ports

| Service | Port | Container Image | Dockerfile |
|---------|------|-----------------|-----------|
| Config Server | 8888 | `config-server:latest` | `config-server/Dockerfile` |
| Service Registry (Eureka) | 8761 | `service-registry:latest` | `service-registry/Dockerfile` |
| Customer Service | 8081 | `customer-service:latest` | `customer-service/Dockerfile` |
| Account Service | 8082 | `account-service:latest` | `account-service/Dockerfile` |
| API Gateway | 8080 | `api-gateway:latest` | `api-gateway/Dockerfile` |

### Building Docker Images

Build all services:
```bash
# Build each service
cd account-service && ./mvnw clean package && docker build -t account-service:latest .
cd ../customer-service && ./mvnw clean package && docker build -t customer-service:latest .
cd ../api-gateway && ./mvnw clean package && docker build -t api-gateway:latest .
cd ../config-server && ./mvnw clean package && docker build -t config-server:latest .
cd ../service-registry && ./mvnw clean package && docker build -t service-registry:latest .
```

### Running Docker Containers

#### Option 1: Individual Container Startup

Start in order (dependencies first):

```bash
# 1. Start Config Server
docker run -d --name config-server -p 8888:8888 config-server:latest

# 2. Start Service Registry (Eureka)
docker run -d --name service-registry -p 8761:8761 service-registry:latest

# 3. Start Customer Service
docker run -d --name customer-service -p 8081:8081 customer-service:latest

# 4. Start Account Service
docker run -d --name account-service -p 8082:8082 account-service:latest

# 5. Start API Gateway
docker run -d --name api-gateway -p 8080:8080 api-gateway:latest
```

#### Option 2: Docker Compose (Recommended)

Create `docker-compose.yml` in project root:

```yaml
version: '3.8'

services:
  config-server:
    image: config-server:latest
    container_name: config-server
    ports:
      - "8888:8888"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8888/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 30s

  service-registry:
    image: service-registry:latest
    container_name: service-registry
    ports:
      - "8761:8761"
    depends_on:
      config-server:
        condition: service_healthy
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8761/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 30s

  customer-service:
    image: customer-service:latest
    container_name: customer-service
    ports:
      - "8081:8081"
    depends_on:
      service-registry:
        condition: service_healthy
    environment:
      - CONFIG_SERVER_URL=http://config-server:8888
      - EUREKA_SERVER_URL=http://service-registry:8761/eureka/
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 30s

  account-service:
    image: account-service:latest
    container_name: account-service
    ports:
      - "8082:8082"
    depends_on:
      service-registry:
        condition: service_healthy
    environment:
      - CONFIG_SERVER_URL=http://config-server:8888
      - EUREKA_SERVER_URL=http://service-registry:8761/eureka/
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8082/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 30s

  api-gateway:
    image: api-gateway:latest
    container_name: api-gateway
    ports:
      - "8080:8080"
    depends_on:
      service-registry:
        condition: service_healthy
    environment:
      - CONFIG_SERVER_URL=http://config-server:8888
      - EUREKA_SERVER_URL=http://service-registry:8761/eureka/
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 30s
```

Run with Docker Compose:
```bash
docker-compose up -d
```

Stop all containers:
```bash
docker-compose down
```

### Docker Networking

In Docker Compose, services communicate using service names:
- Config Server: `http://config-server:8888`
- Service Registry: `http://service-registry:8761`
- Customer Service: `http://customer-service:8081`
- Account Service: `http://account-service:8082`
- API Gateway: `http://api-gateway:8080`

### Health Checks

Each Dockerfile includes a health check endpoint. Verify container health:

```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
```

All containers should show `(healthy)` in status.

---

## Postman Setup

### Environment Files

#### 1. Banking Application - Local
**File**: `config-server/postman/banking-application.local.postman_environment.json`

For direct service testing (no API Gateway):
- **Config Server**: `http://localhost:8888`
- **Service Registry**: `http://localhost:8761`
- **Customer Service**: `http://localhost:8081`
- **Account Service**: `http://localhost:8082`

#### 2. Banking Application - API Gateway (Local)
**File**: `config-server/postman/banking-application-gateway.local.postman_environment.json`

For API Gateway testing:
- **Base URL**: `http://localhost:8080`
- **Routing**: `/api/customers/**` and `/api/accounts/**`

### Collections

#### 1. Banking Application APIs
**File**: `config-server/postman/banking-application.postman_collection.json`

Comprehensive collection with:
- **Infrastructure Tests**: Config Server, Service Registry health checks
- **API Gateway Routes**: Customer and Account APIs through gateway
- **Direct Service APIs**: Testing individual microservices
- **Test Scripts**: Automatic variable population (customerId, accountId)

#### 2. Banking Application - API Gateway
**File**: `config-server/postman/banking-application-gateway.postman_collection.json`

Specialized collection for API Gateway testing only:
- Customer Management endpoints
- Account Management endpoints
- Transaction operations
- Automatic test assertions

### Importing into Postman

1. Open Postman
2. Click **Import** in top-left
3. Select **Upload Files**
4. Import both collections and environment files:
   - `banking-application.postman_collection.json`
   - `banking-application-gateway.postman_collection.json`
   - `banking-application.local.postman_environment.json`
   - `banking-application-gateway.local.postman_environment.json`

5. In Postman, select the desired environment from top-right dropdown

### API Testing Workflow

#### Test via Direct Services

1. Select environment: **Banking Application - Local**
2. Go to **Customer Service (Direct)** folder
3. Run requests in order:
   - **Add Customer** (saves customerId)
   - **Get All Customers**
   - **Get Customer By Id**
   - **Update Customer**
   - **Delete Customer**

4. Go to **Account Service (Direct)** folder
5. Run account operations with the customerId from step 3

#### Test via API Gateway

1. Select environment: **Banking Application - API Gateway (Local)**
2. Go to **Customer Management** folder
3. Run requests:
   - **Add Customer** (auto-populates customerId)
   - **Get All Customers**
   - **Get Customer By Id**
   - **Update Customer**
   - **Delete Customer**

4. Go to **Account Management** folder
5. Run account operations

### Test Scripts

Collections include automated test scripts that:
- Validate response status codes
- Extract IDs from responses
- Populate collection variables automatically
- Assert response structure

Example: After "Add Customer" request, the customerId is automatically set for subsequent requests.

### Running Tests from Command Line

```bash
# Install Newman (CLI tool for Postman)
npm install -g newman

# Run collection against environment
newman run "banking-application.postman_collection.json" \
  -e "banking-application.local.postman_environment.json" \
  --reporters cli,json \
  --reporter-json-export results.json
```

---

## Service Configuration

### Config Server
Routes configuration from Git/classpath to all services:
- **Customer Service Config**: `config-server/src/main/resources/config/customer-service.yaml`
- **Account Service Config**: `config-server/src/main/resources/config/account-service.yaml`
- **API Gateway Config**: `config-server/src/main/resources/config/api-gateway.yaml`

### Service Discovery (Eureka)
All services register with Eureka on startup:
```bash
curl http://localhost:8761/eureka/apps
```

### Distributed Tracing
OpenTelemetry tracing is enabled for all services:
- **Spring Boot Starter**: `spring-boot-starter-opentelemetry`
- **Configuration**: Set in config-server YAML files
- **Sampling Probability**: 1.0 (100% tracing)

---

## Troubleshooting

### Container fails to start
```bash
# Check logs
docker logs <container-name>

# Check health
docker inspect <container-name> | grep -A 5 "HealthCheck"
```

### Services not discovering each other
1. Verify all services can reach Eureka
2. Check Eureka dashboard: `http://localhost:8761`
3. Verify service names in configuration match registration

### Postman requests failing with 500 errors
1. Verify services are running: `docker ps`
2. Check service logs: `docker logs <service-name>`
3. Verify environment variables are set correctly

### Port conflicts
```bash
# Find process using port
lsof -i :8080

# Or use Windows:
netstat -ano | findstr :8080
```

---

## Best Practices

1. **Always build before containerizing**: Run `mvn clean package` before `docker build`
2. **Use multi-stage builds**: Reduces final image size
3. **Run as non-root**: Security best practice (all Dockerfiles use appuser)
4. **Health checks**: Enable monitoring of container health
5. **Environment variables**: Override defaults for different environments
6. **Network isolation**: Use Docker networks for service communication
7. **Logging**: Monitor container logs during testing

---

## Additional Resources

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Reference](https://docs.docker.com/compose/compose-file/)
- [Postman Documentation](https://learning.postman.com/docs/)
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)
- [Spring Cloud Config](https://spring.io/projects/spring-cloud-config)

