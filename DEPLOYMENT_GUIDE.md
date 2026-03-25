# Banking Microservices Application - Docker Deployment Guide

## Overview

This is a microservices-based banking application with the following components:
- **Service Registry (Eureka)**: Service discovery
- **Config Server**: Centralized configuration management
- **API Gateway**: Single entry point for all client requests
- **Customer Service**: Manages customer information
- **Account Service**: Manages customer accounts

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                          Internet/Client                         │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │   API Gateway   │ (Port 8080)
                    │    (localhost)  │
                    └────────┬────────┘
                             │
                    ┌────────▼──────────────────┐
                    │   Docker Network          │
                    │ (banking-network bridge) │
                    │                           │
         ┌──────────┼──────────┬────────────┬──┴──────────┐
         │          │          │            │             │
         ▼          ▼          ▼            ▼             ▼
    ┌────────┐ ┌────────┐ ┌──────────┐ ┌──────────┐ ┌─────────────┐
    │Config  │ │Service │ │Customer  │ │ Account  │ │ Zipkin      │
    │Server  │ │Registry│ │Service   │ │ Service  │ │ (Optional)  │
    │:8888   │ │:8761   │ │:8081     │ │ :8082    │ │ :9411       │
    └────────┘ └────────┘ └──────────┘ └──────────┘ └─────────────┘
```

## Prerequisites

- Docker installed and running
- Docker Compose installed
- Git (to clone the repository)

## Running the Application

### Step 1: Navigate to Config Server Directory

```bash
cd C:\Users\sudhe\OneDrive\Documents\project\banking-application\config-server
```

### Step 2: Start All Services with Docker Compose

```bash
docker-compose up -d
```

This command will:
1. Build all service images (config-server, service-registry, customer-service, account-service, api-gateway)
2. Start all containers on the `banking-network` network
3. Services will automatically register with Eureka Service Registry
4. Run in detached mode (background)

### Step 3: Verify Services are Running

```bash
docker-compose ps
```

Expected output:
```
NAME                  STATUS
config-server         Up (healthy)
service-registry      Up (healthy)
customer-service      Up (healthy)
account-service       Up (healthy)
api-gateway           Up (healthy)
```

### Step 4: Verify Service Registration in Eureka

Open your browser and navigate to:
```
http://localhost:8761
```

You should see all 5 services registered:
- `api-gateway`
- `customer-service`
- `account-service`

## API Endpoints

### Customer Service

#### Create a Customer
```bash
curl -X POST http://localhost:8080/api/customers \
  --header 'Content-Type: application/json' \
  --body '{
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "9876543210",
    "address": "123 Main St"
  }'
```

#### Get All Customers
```bash
curl http://localhost:8080/api/customers
```

#### Get Customer by ID
```bash
curl http://localhost:8080/api/customers/1
```

#### Update Customer
```bash
curl -X PUT http://localhost:8080/api/customers/1 \
  --header 'Content-Type: application/json' \
  --body '{
    "name": "Jane Doe",
    "email": "jane@example.com",
    "phone": "9876543210",
    "address": "123 Main St"
  }'
```

#### Delete Customer
```bash
curl -X DELETE http://localhost:8080/api/customers/1
```

### Account Service

#### Create an Account
```bash
curl -X POST http://localhost:8080/api/accounts \
  --header 'Content-Type: application/json' \
  --body '{
    "customerId": 1,
    "balance": 1000.0,
    "accountType": "SAVINGS"
  }'
```

#### Get All Accounts
```bash
curl http://localhost:8080/api/accounts
```

#### Get Account by ID
```bash
curl http://localhost:8080/api/accounts/1
```

#### Get Accounts by Customer ID
```bash
curl http://localhost:8080/api/accounts/customer/1
```

#### Update Account
```bash
curl -X PUT http://localhost:8080/api/accounts/1 \
  --header 'Content-Type: application/json' \
  --body '{
    "balance": 1500.0,
    "accountType": "SAVINGS"
  }'
```

#### Delete Account
```bash
curl -X DELETE http://localhost:8080/api/accounts/1
```

## Stopping Services

To stop all services:
```bash
docker-compose down
```

To stop and remove all containers including volumes:
```bash
docker-compose down -v
```

## Docker Networking

The services communicate through the `banking-network` Docker bridge network. Inside the Docker network:
- Services use their **container names** (not localhost) for internal communication
- Example: `customer-service` connects to `http://service-registry:8761/eureka/`

## Environment Variables Used

| Variable | Value | Purpose |
|----------|-------|---------|
| `CONFIG_SERVER_URL` | `http://config-server:8888` | Config server URL for clients |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://service-registry:8761/eureka/` | Eureka registry URL |
| `EUREKA_INSTANCE_HOSTNAME` | Service container name | Hostname for service registration |

## Troubleshooting

### Services Not Registering with Eureka

**Issue**: Services show as DOWN in Eureka dashboard

**Solution**: 
- Ensure all services are using container hostnames (not localhost) for internal communication
- Check docker-compose environment variables are set correctly
- Verify network connectivity: `docker network inspect banking-network`

### API Gateway Not Accessible

**Issue**: Cannot access API at `http://localhost:8080`

**Solution**:
- Verify API Gateway container is running: `docker-compose ps api-gateway`
- Check logs: `docker-compose logs api-gateway`
- Ensure port 8080 is not already in use

### Connection Refused Errors

**Issue**: Services getting "Connection refused" errors

**Solution**:
- This usually occurs when services try to use `localhost` instead of container names
- Update docker-compose.yml to use container DNS names
- Restart services: `docker-compose restart`

## Health Checks

Each service has a built-in health check:
- URL: `http://service-name:port/actuator/health`
- Examples:
  - `http://api-gateway:8080/actuator/health`
  - `http://service-registry:8761/actuator/health`
  - `http://customer-service:8081/actuator/health`
  - `http://account-service:8082/actuator/health`

## Logs

View logs for a specific service:
```bash
docker-compose logs -f service-name
```

Example:
```bash
docker-compose logs -f api-gateway
docker-compose logs -f customer-service
docker-compose logs -f service-registry
```

## Port Mapping

| Service | Internal Port | External Port | URL |
|---------|---------------|---------------|-----|
| API Gateway | 8080 | 8080 | http://localhost:8080 |
| Service Registry | 8761 | 8761 | http://localhost:8761 |
| Config Server | 8888 | 8888 | http://localhost:8888 |
| Customer Service | 8081 | 8081 | http://localhost:8081 |
| Account Service | 8082 | 8082 | http://localhost:8082 |

---

## Key Fixes Applied for Ubuntu Docker Deployment

1. **Docker Network DNS**: Changed all service URLs from `localhost` to container names (e.g., `service-registry`, `config-server`)
2. **Environment Variables**: Added `EUREKA_INSTANCE_HOSTNAME` to ensure proper service registration
3. **API Gateway Configuration**: Added complete Eureka client configuration
4. **Service YAML Files**: Added explicit Eureka client registration properties

