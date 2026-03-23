# Banking Microservices - Quick Start Guide

**Last Updated**: March 24, 2026

---

## Overview

This document provides instructions on how to run the Banking Microservices application and sample URLs to test each service endpoint.

### Architecture
- **Config Server** (8888) - Centralized configuration management
- **Service Registry/Eureka** (8761) - Service discovery
- **Customer Service** (8081) - Customer management APIs
- **Account Service** (8082) - Account management APIs  
- **API Gateway** (8080) - Single entry point for all APIs

---

## Prerequisites

- **Java 17+** installed
- **Maven 3.6+** installed
- **Docker & Docker Compose** (optional, for containerized deployment)
- **Postman** or **curl** (for testing APIs)
- **Git** (for cloning the repository)

---

## Method 1: Running Locally (Without Docker)

### Step 1: Start Services in Order

Each service depends on previous ones. Start them in this exact order:

#### 1. Config Server (Must Start First!)
```bash
cd config-server
./mvnw spring-boot:run
```
**Wait for message**: `Started ConfigServerApplication in X seconds`

#### 2. Service Registry (Eureka)
Open a new terminal:
```bash
cd service-registry
./mvnw spring-boot:run
```
**Wait for message**: `Started ServiceRegistryApplication in X seconds`

#### 3. Customer Service
Open a new terminal:
```bash
cd customer-service
./mvnw spring-boot:run
```
**Wait for message**: `Started CustomerServiceApplication in X seconds`

#### 4. Account Service
Open a new terminal:
```bash
cd account-service
./mvnw spring-boot:run
```
**Wait for message**: `Started AccountServiceApplication in X seconds`

#### 5. API Gateway
Open a new terminal:
```bash
cd api-gateway
./mvnw spring-boot:run
```
**Wait for message**: `Started ApiGatewayApplication in X seconds`

### Step 2: Verify Services Are Running

Check if all services registered with Eureka:
```bash
curl http://localhost:8761/eureka/apps
```

Check individual service health:
```bash
curl http://localhost:8888/actuator/health    # Config Server
curl http://localhost:8761/actuator/health    # Service Registry
curl http://localhost:8081/actuator/health    # Customer Service
curl http://localhost:8082/actuator/health    # Account Service
curl http://localhost:8080/actuator/health    # API Gateway
```

---

## Method 2: Running with Docker (Recommended)

### Step 1: Build Docker Images

From the project root directory, run:

```bash
# Build all services
cd account-service && mvn clean package && docker build -t account-service:latest .
cd ../customer-service && mvn clean package && docker build -t customer-service:latest .
cd ../config-server && mvn clean package && docker build -t config-server:latest .
cd ../service-registry && mvn clean package && docker build -t service-registry:latest .
cd ../api-gateway && mvn clean package && docker build -t api-gateway:latest .

# Return to project root
cd ..
```

### Step 2: Start All Services

```bash
docker-compose up -d
```

### Step 3: Check Container Status

```bash
docker-compose ps
```

All services should show `(healthy)` status after ~30-60 seconds.

### Step 4: View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker logs -f config-server
docker logs -f customer-service
```

### Step 5: Stop Services

```bash
docker-compose down
```

---

## Service Endpoints & Sample URLs

### 1. Config Server (http://localhost:8888)

**Get Customer Service Configuration:**
```bash
curl http://localhost:8888/customer-service/default
```

**Get Account Service Configuration:**
```bash
curl http://localhost:8888/account-service/default
```

**Get API Gateway Configuration:**
```bash
curl http://localhost:8888/api-gateway/default
```

---

### 2. Service Registry/Eureka (http://localhost:8761)

**View All Registered Services:**
```bash
curl http://localhost:8761/eureka/apps
```

**Web Dashboard:**
```
http://localhost:8761/
```

---

### 3. Customer Service (Direct - http://localhost:8081)

#### Add Customer
```bash
curl -X POST http://localhost:8081/customers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john.doe@example.com",
    "phone": "9876543210",
    "address": "123 Main Street, Hyderabad"
  }'
```
**Response**: Returns customer with auto-generated ID (e.g., 1)

#### Get All Customers
```bash
curl http://localhost:8081/customers
```

#### Get Customer by ID
```bash
curl http://localhost:8081/customers/1
```

#### Update Customer
```bash
curl -X PUT http://localhost:8081/customers/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Updated",
    "email": "john.updated@example.com",
    "phone": "9123456789",
    "address": "456 New Street, Bengaluru"
  }'
```

#### Delete Customer (Cascades to Accounts)
```bash
curl -X DELETE http://localhost:8081/customers/1
```

---

### 4. Account Service (Direct - http://localhost:8082)

#### Create Account
```bash
curl -X POST http://localhost:8082/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "balance": 1000.0,
    "accountType": "SAVINGS"
  }'
```
**Response**: Returns account with auto-generated ID (e.g., 1)

#### Get Account Details
```bash
curl http://localhost:8082/accounts/1
```

#### Deposit Money
```bash
curl -X POST "http://localhost:8082/accounts/1/add?amount=500.0&customerId=1"
```

#### Withdraw Money
```bash
curl -X POST "http://localhost:8082/accounts/1/withdraw?amount=200.0&customerId=1"
```

#### Delete Account
```bash
curl -X DELETE http://localhost:8082/accounts/1
```

#### Delete All Accounts for Customer
```bash
curl -X DELETE http://localhost:8082/accounts/deleteByCustomer/1
```

---

### 5. API Gateway (http://localhost:8080)

The API Gateway routes all requests to downstream services. All endpoints use the `/api` prefix.

#### Customer APIs via Gateway

**Add Customer**
```bash
curl -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jane Smith",
    "email": "jane.smith@example.com",
    "phone": "9988776655",
    "address": "789 Park Avenue, Pune"
  }'
```

**Get All Customers**
```bash
curl http://localhost:8080/api/customers
```

**Get Customer by ID**
```bash
curl http://localhost:8080/api/customers/1
```

**Update Customer**
```bash
curl -X PUT http://localhost:8080/api/customers/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jane Updated",
    "email": "jane.updated@example.com",
    "phone": "9988776656",
    "address": "789 Park Avenue, Mumbai"
  }'
```

**Delete Customer**
```bash
curl -X DELETE http://localhost:8080/api/customers/1
```

#### Account APIs via Gateway

**Create Account**
```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "balance": 5000.0,
    "accountType": "CURRENT"
  }'
```

**Get Account Details**
```bash
curl http://localhost:8080/api/accounts/1
```

**Deposit Money**
```bash
curl -X POST "http://localhost:8080/api/accounts/1/add?amount=1000.0&customerId=1"
```

**Withdraw Money**
```bash
curl -X POST "http://localhost:8080/api/accounts/1/withdraw?amount=500.0&customerId=1"
```

**Delete Account**
```bash
curl -X DELETE http://localhost:8080/api/accounts/1
```

---

## Using Postman for Testing

### Import Postman Collections

1. Open Postman
2. Click **Import** → **Upload Files**
3. Select and import:
   - `banking-application.postman_collection.json`
   - `banking-application-gateway.postman_collection.json`
   - `banking-application.local.postman_environment.json`
   - `banking-application-gateway.local.postman_environment.json`

### Select Environment

In Postman, select environment from top-right dropdown:
- **Banking Application - Local** (for direct service testing)
- **Banking Application - API Gateway (Local)** (for gateway testing)

### Run Requests

Collections are organized by service:
- **Infrastructure** - Config Server & Service Registry checks
- **API Gateway - Customer APIs** - Customer operations via gateway
- **API Gateway - Account APIs** - Account operations via gateway
- **Customer Service (Direct)** - Direct customer service calls
- **Account Service (Direct)** - Direct account service calls

---

## Typical Workflow

### 1. Create a Customer
```bash
curl -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Johnson",
    "email": "alice@example.com",
    "phone": "9999999999",
    "address": "Bangalore"
  }'
```
**Note the returned `id` value (e.g., 1)**

### 2. Create an Account for that Customer
```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "balance": 10000.0,
    "accountType": "SAVINGS"
  }'
```
**Note the returned account `id` value (e.g., 1)**

### 3. Deposit Money
```bash
curl -X POST "http://localhost:8080/api/accounts/1/add?amount=5000.0&customerId=1"
```

### 4. Check Account Balance
```bash
curl http://localhost:8080/api/accounts/1
```

### 5. Withdraw Money
```bash
curl -X POST "http://localhost:8080/api/accounts/1/withdraw?amount=2000.0&customerId=1"
```

### 6. Check Account Again
```bash
curl http://localhost:8080/api/accounts/1
```

---

## Troubleshooting

### Services Not Starting

**Check logs:**
```bash
# Local
cat account-service/account-service-run.log

# Docker
docker logs <service-name>
```

**Common issues:**
- Port already in use: Kill process on port or change port in config
- Config Server not starting: Ensure it starts first
- Service discovery failing: Wait longer for services to register with Eureka

### Port Conflicts

**Find process using port:**
```bash
# Windows
netstat -ano | findstr :8081

# Linux/Mac
lsof -i :8081
```

**Kill process:**
```bash
# Windows
taskkill /PID <PID> /F

# Linux/Mac
kill -9 <PID>
```

### Database Connection Issues

If using database:
- Verify database is running
- Check connection strings in config-server YAML files
- Review logs for SQL errors

### Service Registration Timeout

Services may take 30-60 seconds to appear in Eureka:
```bash
# Wait and check again
sleep 30
curl http://localhost:8761/eureka/apps
```

---

## Performance Testing

### Load Test with Apache Bench

```bash
# Create customers (concurrency: 10, requests: 100)
ab -n 100 -c 10 -p customer.json -T application/json \
  http://localhost:8080/api/customers

# Get customers (concurrency: 50, requests: 1000)
ab -n 1000 -c 50 http://localhost:8080/api/customers
```

### Monitor Container Resources (Docker)

```bash
docker stats
```

---

## Useful Links

- **Config Server**: http://localhost:8888
- **Service Registry**: http://localhost:8761
- **Customer Service Health**: http://localhost:8081/actuator/health
- **Account Service Health**: http://localhost:8082/actuator/health
- **API Gateway Health**: http://localhost:8080/actuator/health

---

## Additional Resources

- See `DOCKER_AND_POSTMAN_SETUP.md` for detailed Docker and Postman setup
- See `DOCKER_POSTMAN_UPDATES.md` for recent updates
- See `UPDATE_COMPLETE.md` for complete change summary

---

## Support

For issues or questions:
1. Check logs: `docker-compose logs -f`
2. Verify all services are healthy: Check `/actuator/health` endpoints
3. Review service registration: `curl http://localhost:8761/eureka/apps`
4. Check configuration: `curl http://localhost:8888/<service-name>/default`

