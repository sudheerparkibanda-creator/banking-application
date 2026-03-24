# Banking Application – Ubuntu Deployment Guide

## Prerequisites

Install Docker and Docker Compose on Ubuntu:

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg

# Add Docker's official GPG key
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# Add Docker repository
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Allow running Docker without sudo (log out and back in after this)
sudo usermod -aG docker $USER
```

## Project Structure

```
banking-application/
├── config-server/          # Spring Cloud Config Server  (port 8888)
│   └── docker-compose.yml  # Main compose file for all services
├── service-registry/       # Eureka Service Registry     (port 8761)
├── api-gateway/            # Spring Cloud API Gateway    (port 8080)
├── customer-service/       # Customer Management API     (port 8081)
├── account-service/        # Account Management API      (port 8082)
├── build.sh                # One-command Ubuntu launcher
└── .gitattributes          # Line-ending configuration
```

## Build & Run

> **No local Java or Maven required** – Docker handles everything via multi-stage builds.

```bash
# Clone / copy the project, then:
cd banking-application

# Make the script executable
chmod +x build.sh

# Build all images and start all services
./build.sh up

# Or manually:
docker compose -f config-server/docker-compose.yml up --build -d
```

## Service URLs

| Service          | URL                              |
|------------------|----------------------------------|
| Config Server    | http://localhost:8888            |
| Eureka Dashboard | http://localhost:8761            |
| API Gateway      | http://localhost:8080            |
| Customer Service | http://localhost:8081            |
| Account Service  | http://localhost:8082            |

## Startup Order

Docker Compose enforces this order using health checks:

```
config-server  →  service-registry  →  customer-service
                                    →  account-service
                                    →  api-gateway
```

## Useful Commands

```bash
# View logs for all services
./build.sh logs

# Stop and remove all containers
./build.sh down

# Rebuild a single service
docker compose -f config-server/docker-compose.yml build customer-service
docker compose -f config-server/docker-compose.yml up -d customer-service

# View running containers
docker compose -f config-server/docker-compose.yml ps
```

## API Endpoints (via API Gateway)

### Customer Service
```
POST   http://localhost:8080/api/customers
GET    http://localhost:8080/api/customers
GET    http://localhost:8080/api/customers/{id}
PUT    http://localhost:8080/api/customers/{id}
DELETE http://localhost:8080/api/customers/{id}
```

### Account Service
```
POST   http://localhost:8080/api/accounts
GET    http://localhost:8080/api/accounts
GET    http://localhost:8080/api/accounts/{id}
PUT    http://localhost:8080/api/accounts/{id}
DELETE http://localhost:8080/api/accounts/{id}
GET    http://localhost:8080/api/accounts/customer/{customerId}
```

