# Banking Microservices: Docker-Only Run Guide

This is the single document to run and test the project using Docker, with one architecture diagram and curl/Postman-ready examples.

## 1) Architecture (Single Diagram)

```text
             Client / Postman / curl
                        |
                        v
                 API Gateway :8080
          routes /api/customers/**, /api/accounts/**
                    |                    |
                    v                    v
           Customer Service :8081   Account Service :8082
                    |                    |
                    +---------+----------+
                              |
                              v
                  Service Registry (Eureka) :8761
                  (gateway + services register/discover)

          API Gateway + Customer Service + Account Service
                              |
                              v
                     Config Server :8888
                        (fetch config)
```

## 2) Start Using Docker Only

Run from project root (`banking-application`):

```bash
docker-compose up -d --build
```

Check containers:

```bash
docker-compose ps
```

Follow logs (optional):

```bash
docker-compose logs -f
```

Stop all services:

```bash
docker-compose down
```

## 3) Basic Health/Infra Checks

```bash
curl http://localhost:8888/actuator/health
curl http://localhost:8761/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8080/actuator/health
```

Optional Eureka check:

```bash
curl http://localhost:8761/eureka/apps
```

## 4) API Test Examples (Gateway)

### 4.1 Create Customer

```bash
curl -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Johnson",
    "email": "alice.johnson@example.com",
    "phone": "9999999999",
    "address": "Bangalore"
  }'
```

### 4.2 Get All Customers

```bash
curl http://localhost:8080/api/customers
```

### 4.3 Create Account (use valid `customerId`)

```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "balance": 10000.0,
    "accountType": "SAVINGS"
  }'
```

### 4.4 Deposit Money

```bash
curl -X POST "http://localhost:8080/api/accounts/1/add?amount=5000.0&customerId=1"
```

### 4.5 Withdraw Money

```bash
curl -X POST "http://localhost:8080/api/accounts/1/withdraw?amount=2000.0&customerId=1"
```

### 4.6 Get Account Details

```bash
curl http://localhost:8080/api/accounts/1
```

## 5) Postman Quick Setup

- Base URL: `http://localhost:8080`
- Use these paths:
  - `POST /api/customers`
  - `GET /api/customers`
  - `POST /api/accounts`
  - `POST /api/accounts/{accountId}/add?amount=...&customerId=...`
  - `POST /api/accounts/{accountId}/withdraw?amount=...&customerId=...`
  - `GET /api/accounts/{accountId}`

Sample JSON body for `POST /api/customers`:

```json
{
  "name": "Alice Johnson",
  "email": "alice.johnson@example.com",
  "phone": "9999999999",
  "address": "Bangalore"
}
```

Sample JSON body for `POST /api/accounts`:

```json
{
  "customerId": 1,
  "balance": 10000.0,
  "accountType": "SAVINGS"
}
```


