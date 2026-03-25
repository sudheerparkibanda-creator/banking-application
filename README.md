# Banking Microservices Application

A Spring Cloud microservices-based banking application demonstrating service discovery, API gateway routing, and distributed configuration management.

## Quick Start

### Prerequisites
- Docker & Docker Compose installed
- Internet connection for pulling base images

### Run in Docker (Recommended)

```bash
# Navigate to config-server directory
cd config-server

# Start all services
docker-compose up -d

# Verify services are running
docker-compose ps

# View logs of a specific service
docker-compose logs -f api-gateway
```

Services will be available at:
- **API Gateway**: http://localhost:8080
- **Eureka Dashboard**: http://localhost:8761
- **Config Server**: http://localhost:8888

---

## Architecture

### Services Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      Client/Postman                         │
└────────────────────┬────────────────────────────────────────┘
                     │ REST Calls (JSON)
                     ▼
        ┌────────────────────────┐
        │    API Gateway         │
        │  (Load Balancer)       │
        │  Port: 8080            │
        └────────┬───────────────┘
                 │ Service Discovery
                 ▼
        ┌────────────────────────┐
        │ Service Registry       │
        │ (Eureka Server)        │
        │ Port: 8761             │
        └────────────────────────┘
                 │ Registers Services
        ┌────────┴────────────────┐
        │                         │
        ▼                         ▼
    ┌─────────────────┐   ┌──────────────────┐
    │ Customer Service │  │ Account Service  │
    │ Port: 8081      │  │ Port: 8082       │
    │ Manages:        │  │ Manages:         │
    │ - Customers     │  │ - Accounts       │
    │ - Email Unique  │  │ - Validations    │
    └─────────────────┘  └──────────────────┘
            │                      │
            ▼                      ▼
        ┌──────────────────────────────┐
        │  Config Server               │
        │  Port: 8888                  │
        │  Centralized Configuration   │
        └──────────────────────────────┘
```

### Component Details

| Service | Port | Role | Database |
|---------|------|------|----------|
| **API Gateway** | 8080 | Single entry point, route to services | N/A |
| **Service Registry** | 8761 | Service discovery (Eureka) | In-memory |
| **Config Server** | 8888 | Centralized configuration | File-based |
| **Customer Service** | 8081 | Customer CRUD, Email validation | H2/File |
| **Account Service** | 8082 | Account CRUD, Customer validation | H2/File |

---

## API Endpoints

### Customer Service (via API Gateway)

**Base URL**: `http://localhost:8080/api`

#### Create Customer
```bash
POST /customers
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "9876543210",
  "address": "123 Main St"
}
```

#### Get All Customers
```bash
GET /customers
```

#### Get Customer by ID
```bash
GET /customers/{id}
```

#### Update Customer
```bash
PUT /customers/{id}
Content-Type: application/json

{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "phone": "9876543210",
  "address": "456 Oak Ave"
}
```

#### Delete Customer (also deletes associated accounts)
```bash
DELETE /customers/{id}
```

---

### Account Service (via API Gateway)

**Base URL**: `http://localhost:8080/api`

#### Create Account
```bash
POST /accounts
Content-Type: application/json

{
  "customerId": 1,
  "balance": 1000.0,
  "accountType": "SAVINGS"
}
```

#### Get All Accounts
```bash
GET /accounts
```

#### Get Account by ID
```bash
GET /accounts/{id}
```

#### Get Accounts by Customer ID
```bash
GET /accounts/customer/{customerId}
```

#### Update Account
```bash
PUT /accounts/{id}
Content-Type: application/json

{
  "balance": 1500.0,
  "accountType": "SAVINGS"
}
```

#### Delete Account
```bash
DELETE /accounts/{id}
```

---

## Data Models

### Customer
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "9876543210",
  "address": "123 Main St"
}
```

**Validations:**
- Email must be unique
- Email is case-insensitive and trimmed
- All fields are required

### Account
```json
{
  "id": 1,
  "customerId": 1,
  "balance": 1000.0,
  "accountType": "SAVINGS"
}
```

**Validations:**
- Customer must exist before creating account
- Account type must be valid (SAVINGS, CHECKING, etc.)
- Deleting a customer deletes all associated accounts

---

## Docker Deployment

### Run Services
```bash
cd config-server
docker-compose up -d
```

### Check Status
```bash
docker-compose ps
docker-compose logs api-gateway
```

### Stop Services
```bash
docker-compose down
```

### Remove All Data
```bash
docker-compose down -v
```

---

## Key Features Implemented

### ✅ Service Discovery (Eureka)
- Automatic service registration
- Client-side service discovery
- Health checks and instance refresh

### ✅ API Gateway
- Routes requests to appropriate services
- Load balancing across instances
- Single point of entry for clients

### ✅ Centralized Configuration
- Spring Cloud Config Server
- Native profile with file-based configuration
- Dynamic configuration updates

### ✅ Data Validation
- Unique email constraint for customers
- Customer existence validation before account creation
- Cascade delete of accounts when customer is deleted

### ✅ Error Handling
- Global exception handlers
- Consistent error response format
- Proper HTTP status codes

### ✅ Docker Integration
- Multi-stage Docker builds for optimized images
- Docker Compose orchestration
- Network isolation and service communication

---

## Monitoring & Debugging

### Eureka Dashboard
View registered services:
```
http://localhost:8761
```

### Service Health Checks
```bash
# API Gateway
curl http://localhost:8080/actuator/health

# Customer Service
curl http://localhost:8081/actuator/health

# Account Service
curl http://localhost:8082/actuator/health

# Service Registry
curl http://localhost:8761/actuator/health
```

### View Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f customer-service
docker-compose logs -f account-service
docker-compose logs -f api-gateway
```

### Access Service Registry API
```bash
# Get all registered apps
curl http://localhost:8761/eureka/apps

# Get specific app
curl http://localhost:8761/eureka/apps/customer-service
```

---

## Troubleshooting

### Services Not Registering

**Symptom**: Services show as DOWN in Eureka

**Solutions**:
1. Verify containers are running: `docker-compose ps`
2. Check service logs: `docker-compose logs service-name`
3. Ensure services can reach Eureka: `docker-compose exec service-name curl http://service-registry:8761`
4. Check network connectivity: `docker network inspect banking-network`

### API Gateway Not Working

**Symptom**: Cannot connect to API Gateway

**Solutions**:
1. Verify API Gateway is running: `docker-compose ps api-gateway`
2. Check if port 8080 is already in use
3. View logs: `docker-compose logs api-gateway`
4. Test health endpoint: `curl http://localhost:8080/actuator/health`

### Services Cannot Communicate

**Symptom**: Service-to-service calls timeout or fail

**Root Cause**: Using localhost instead of container hostnames inside Docker

**Solution**: This is already fixed in the updated configuration. Services use container DNS names.

---

## Important Configuration Notes

### Docker Network Communication

Inside Docker containers:
- ✅ **Correct**: `http://service-registry:8761` (uses container hostname)
- ❌ **Wrong**: `http://localhost:8761` (refers to the container itself)

### Environment Variables

These are automatically set by docker-compose:
```
CONFIG_SERVER_URL=http://config-server:8888
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://service-registry:8761/eureka/
EUREKA_INSTANCE_HOSTNAME=service-name
```

### Port Mapping

Local machine access:
```
API Gateway: localhost:8080
Service Registry: localhost:8761
Config Server: localhost:8888
Customer Service: localhost:8081
Account Service: localhost:8082
```

---

## Development & Testing

### Using Postman

Import the provided Postman collection:
- File: `Banking_Microservices_API.postman_collection.json`
- Contains all API endpoints with example requests

### Sample Test Workflow

1. **Create Customer**
   ```bash
   POST http://localhost:8080/api/customers
   ```

2. **Get Customer ID** from response (e.g., 1)

3. **Create Account** for that customer
   ```bash
   POST http://localhost:8080/api/accounts
   Body: {"customerId": 1, "balance": 1000.0, "accountType": "SAVINGS"}
   ```

4. **Verify** both entities were created
   ```bash
   GET http://localhost:8080/api/customers/1
   GET http://localhost:8080/api/accounts/customer/1
   ```

---

## Files Structure

```
banking-application/
├── config-server/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── pom.xml
│   └── src/
├── service-registry/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
├── api-gateway/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
├── customer-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
├── account-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
├── DEPLOYMENT_GUIDE.md      (Detailed deployment steps)
├── FIX_SUMMARY.md            (Problems and solutions)
└── Banking_Microservices_API.postman_collection.json
```

---

## Technologies Used

- **Spring Boot 3.x** - Application framework
- **Spring Cloud** - Microservices patterns
- **Spring Cloud Config** - Centralized configuration
- **Netflix Eureka** - Service discovery
- **Spring Cloud Gateway** - API Gateway
- **Docker** - Containerization
- **Docker Compose** - Orchestration
- **H2 Database** - In-memory database
- **Maven** - Build tool

---

## Next Steps

1. ✅ Start services with Docker Compose
2. ✅ Verify all services register in Eureka
3. ✅ Test API endpoints using Postman
4. ✅ Monitor logs and health checks
5. 📈 Scale services as needed
6. 🔐 Add authentication and security
7. 📊 Add monitoring and metrics (Prometheus, Grafana)

---

## Support

For detailed information, refer to:
- **DEPLOYMENT_GUIDE.md** - Step-by-step deployment instructions
- **FIX_SUMMARY.md** - Technical details about fixes applied
- **Docker Compose** - Service orchestration configuration

---

**Last Updated**: March 2026  
**Status**: ✅ Ready for Production Deployment

