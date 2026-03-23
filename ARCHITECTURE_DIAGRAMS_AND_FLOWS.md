# BANKING MICROSERVICES - ARCHITECTURE & DIAGRAMS

**Comprehensive Visual Reference for Microservices Architecture**

---

## TABLE OF CONTENTS

1. High-Level System Architecture
2. Service Interaction Diagram
3. Data Flow Architecture
4. Deployment Architecture
5. Request Processing Flow
6. Service Dependencies
7. Technology Stack Diagram
8. Network Architecture
9. Sequence Diagrams
10. Configuration Flow

---

## 1. HIGH-LEVEL SYSTEM ARCHITECTURE

```
╔═══════════════════════════════════════════════════════════════════╗
║                     BANKING MICROSERVICES SYSTEM                  ║
╚═══════════════════════════════════════════════════════════════════╝

┌─────────────────────────────────────────────────────────────────┐
│                     CLIENT TIER                                  │
│                                                                   │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐   │
│  │   Web Browser    │  │   Mobile App     │  │  3rd Party   │   │
│  │    (React/Vue)   │  │   (iOS/Android)  │  │  Integrations│   │
│  └────────┬─────────┘  └────────┬─────────┘  └──────┬───────┘   │
│           │                     │                   │            │
└───────────┼─────────────────────┼───────────────────┼────────────┘
            │                     │                   │
            └─────────────┬───────┴───────┬───────────┘
                          │               │
                ┌─────────▼────────────────▼───────┐
                │                                   │
                │    API GATEWAY (Port 8080)       │
                │    Spring Cloud Gateway          │
                │                                   │
                │  ┌────────────────────────────┐  │
                │  │  Route /api/customers/** → │  │
                │  │  Customer Service (8081)   │  │
                │  └────────────────────────────┘  │
                │                                   │
                │  ┌────────────────────────────┐  │
                │  │  Route /api/accounts/** →  │  │
                │  │  Account Service (8082)    │  │
                │  └────────────────────────────┘  │
                │                                   │
                └─────────────┬───────┬────────────┘
                              │       │
                ┌─────────────┘       └────────────┐
                │                                  │
   ┌────────────▼───────────┐      ┌──────────────▼───────┐
   │                        │      │                      │
   │   CUSTOMER SERVICE     │      │   ACCOUNT SERVICE    │
   │   (Port 8081)          │      │   (Port 8082)        │
   │                        │      │                      │
   │  • Add Customer        │      │  • Create Account    │
   │  • Get Customer        │      │  • Get Account       │
   │  • Update Customer     │      │  • Deposit Money     │
   │  • Delete Customer     │      │  • Withdraw Money    │
   │  • List Customers      │      │  • Delete Account    │
   │                        │      │                      │
   └───────────┬────────────┘      └──────────────┬──────┘
               │                                  │
               │          ┌──────────────────────┘
               │          │
               └──────────┼──────────────────┐
                          │                  │
              ┌───────────▼────────┐  ┌──────▼────────────┐
              │                    │  │                   │
              │  SERVICE REGISTRY  │  │  CONFIG SERVER    │
              │   (Eureka)         │  │  (Port 8888)      │
              │  (Port 8761)       │  │                   │
              │                    │  │  • Stores configs │
              │  • Registers       │  │  • Serves to      │
              │    services        │  │    microservices  │
              │  • Maintains       │  │  • Centralizes    │
              │    service health  │  │    configuration  │
              │  • Provides        │  │  • Manages        │
              │    discovery       │  │    properties     │
              │                    │  │                   │
              └────────────────────┘  └───────────────────┘
                      ▲                        ▲
                      │                        │
                      └────────────┬───────────┘
                                   │
                      ┌────────────▼──────────┐
                      │                       │
                      │  PERSISTENT STORAGE   │
                      │  (H2/MySQL)           │
                      │                       │
                      │  • Customers Table    │
                      │  • Accounts Table     │
                      │                       │
                      └───────────────────────┘
```

---

## 2. SERVICE INTERACTION DIAGRAM

```
┌────────────────────────────────────────────────────────────────────┐
│                    SERVICE INTERACTION PATTERNS                     │
└────────────────────────────────────────────────────────────────────┘

                    API GATEWAY (8080)
                           │
                ┌──────────┴──────────┐
                │                     │
                ▼                     ▼
        ┌──────────────┐      ┌──────────────┐
        │  CUSTOMER    │      │   ACCOUNT    │
        │  SERVICE     │      │   SERVICE    │
        │  (8081)      │      │   (8082)     │
        └──────┬───────┘      └───────┬──────┘
               │                      │
               │                      │
               │  ┌──────────────────┘
               │  │
               │  │  Account Service calls Customer Service
               │  │  (Validating customer exists)
               │  │
               │  ▼  Request: GET /customers/{customerId}
               │     Response: {customer data}
               │
               │  with CIRCUIT BREAKER:
               │  ├─ If Customer Service UP → Forward request
               │  ├─ If Customer Service DOWN → Return cached data
               │  └─ If persistent → Return error after retries
               │
               ▼
        Both services connect to:
        
        ┌──────────────────┐
        │ SERVICE REGISTRY │
        │    (8761)        │
        └────────┬─────────┘
                 │
                 │  All services:
                 │  1. Register on startup
                 │  2. Send heartbeat (30s)
                 │  3. Health checks (10s)
                 │  4. De-register on shutdown
                 │
                 ▼
        ┌──────────────────┐
        │  CONFIG SERVER   │
        │     (8888)       │
        └──────────────────┘
                 │
                 │  All services:
                 │  1. Fetch config on startup
                 │  2. Refresh config on demand
                 │  3. Apply properties
                 │  4. Update runtime behavior
```

---

## 3. DATA FLOW ARCHITECTURE

```
┌────────────────────────────────────────────────────────────────────┐
│                  CREATE ACCOUNT - DATA FLOW                        │
└────────────────────────────────────────────────────────────────────┘

CLIENT REQUEST
│
│ POST /api/accounts
│ {
│   "customerId": 1,
│   "balance": 5000.0,
│   "accountType": "SAVINGS"
│ }
│
▼
┌──────────────────────────────────────────┐
│  API GATEWAY (8080)                      │
│                                          │
│  1. Receives request                     │
│  2. Validates format                     │
│  3. Adds tracing headers                 │
│  4. Routes to account-service:8082       │
└────────────┬─────────────────────────────┘
             │
             │ Forward request with tracing context
             │
             ▼
┌──────────────────────────────────────────┐
│  ACCOUNT SERVICE CONTROLLER (8082)       │
│                                          │
│  1. Receives request                     │
│  2. Validates JSON                       │
│  3. Extracts customerId                  │
└────────────┬─────────────────────────────┘
             │
             │ Call service layer
             │
             ▼
┌──────────────────────────────────────────┐
│  ACCOUNT SERVICE LAYER                   │
│                                          │
│  1. Business logic                       │
│  2. Validate customer exists             │
│  │  │                                    │
│  │  └─► CALL CUSTOMER SERVICE (8081)    │
│  │      GET /customers/1                │
│  │      with CIRCUIT BREAKER             │
│  │      ├─ Success: Continue             │
│  │      ├─ Failure: Check cache          │
│  │      └─ Error: Return 500             │
│  │                                        │
│  3. Create Account object                │
│  4. Call repository to save              │
└────────────┬─────────────────────────────┘
             │
             │ Save to database
             │
             ▼
┌──────────────────────────────────────────┐
│  JPA REPOSITORY                          │
│                                          │
│  1. Generate INSERT SQL                  │
│  2. Execute SQL                          │
│  3. Return saved entity with ID          │
└────────────┬─────────────────────────────┘
             │
             │ INSERT result
             │
             ▼
┌──────────────────────────────────────────┐
│  DATABASE (H2/MySQL)                     │
│                                          │
│  INSERT INTO accounts                    │
│  (customer_id, balance, account_type)    │
│  VALUES (1, 5000.0, 'SAVINGS')           │
│                                          │
│  Returns: ID = 42                        │
└────────────┬─────────────────────────────┘
             │
             │ Result back to repository
             │
             ▼
┌──────────────────────────────────────────┐
│  SERVICE LAYER                           │
│                                          │
│  1. Receive saved Account (id=42)        │
│  2. Return to controller                 │
└────────────┬─────────────────────────────┘
             │
             │ Convert to JSON
             │
             ▼
┌──────────────────────────────────────────┐
│  ACCOUNT SERVICE CONTROLLER              │
│                                          │
│  Return 201 CREATED                      │
│  {                                       │
│    "id": 42,                             │
│    "customerId": 1,                      │
│    "balance": 5000.0,                    │
│    "accountType": "SAVINGS"              │
│  }                                       │
└────────────┬─────────────────────────────┘
             │
             │ Return through gateway
             │
             ▼
┌──────────────────────────────────────────┐
│  API GATEWAY                             │
│                                          │
│  1. Add response headers                 │
│  2. Log response                         │
│  3. Send to client                       │
└────────────┬─────────────────────────────┘
             │
             ▼
        CLIENT RESPONSE
        HTTP 201 CREATED
        Account successfully created!
```

---

## 4. DEPLOYMENT ARCHITECTURE

```
┌────────────────────────────────────────────────────────────────────┐
│                  DOCKER CONTAINER DEPLOYMENT                       │
└────────────────────────────────────────────────────────────────────┘

HOST MACHINE (Port 8080-8888)
│
├─ Port 8080 ───┐
├─ Port 8081 ───┤
├─ Port 8082 ───┤   ┌──────────────────────────────────┐
├─ Port 8761 ───┤   │  Docker (Virtualized Container) │
└─ Port 8888 ───┤   │                                  │
                │   │  ┌────────────────────────────┐  │
                │   │  │  API GATEWAY Container    │  │
                │   │  │  Image: api-gateway:latest│  │
                │   │  │  Port: 8080               │  │
                │   │  │  Memory: 512Mi            │  │
                │   │  │  CPU: 100m                │  │
                │   │  └────────────────────────────┘  │
                │   │                                  │
                │   │  ┌────────────────────────────┐  │
                │   │  │ CUSTOMER SERVICE Container│  │
                │   │  │  Image: customer-service  │  │
                │   │  │  Port: 8081               │  │
                │   │  │  Memory: 512Mi            │  │
                │   │  │  CPU: 100m                │  │
                │   │  └────────────────────────────┘  │
                │   │                                  │
                │   │  ┌────────────────────────────┐  │
                │   │  │  ACCOUNT SERVICE Container│  │
                │   │  │  Image: account-service   │  │
                │   │  │  Port: 8082               │  │
                │   │  │  Memory: 512Mi            │  │
                │   │  │  CPU: 100m                │  │
                │   │  └────────────────────────────┘  │
                │   │                                  │
                │   │  ┌────────────────────────────┐  │
                │   │  │  SERVICE REGISTRY Container│  │
                │   │  │  Image: service-registry   │  │
                │   │  │  Port: 8761                │  │
                │   │  │  Memory: 512Mi             │  │
                │   │  │  CPU: 100m                 │  │
                │   │  └────────────────────────────┘  │
                │   │                                  │
                │   │  ┌────────────────────────────┐  │
                │   │  │  CONFIG SERVER Container  │  │
                │   │  │  Image: config-server     │  │
                │   │  │  Port: 8888               │  │
                │   │  │  Memory: 256Mi            │  │
                │   │  │  CPU: 50m                 │  │
                │   │  └────────────────────────────┘  │
                │   │                                  │
                │   │  ┌────────────────────────────┐  │
                │   │  │ Shared Network             │  │
                │   │  │ (banking-network)          │  │
                │   │  │                            │  │
                │   │  │ service-to-service DNS:   │  │
                │   │  │ • api-gateway:8080        │  │
                │   │  │ • customer-service:8081   │  │
                │   │  │ • account-service:8082    │  │
                │   │  │ • service-registry:8761   │  │
                │   │  │ • config-server:8888      │  │
                │   │  └────────────────────────────┘  │
                │   │                                  │
                │   │  ┌────────────────────────────┐  │
                │   │  │  Shared H2 Database        │  │
                │   │  │  (or MySQL volume mount)   │  │
                │   │  │                            │  │
                │   │  │  All services share data   │  │
                │   │  └────────────────────────────┘  │
                │   │                                  │
                └───┴──────────────────────────────────┘

Command to deploy:
  docker-compose up -d

Dependencies enforced:
  config-server (starts first)
    ↓ (waits for health check)
  service-registry
    ↓ (waits for health check)
  customer-service & account-service
    ↓ (waits for health check)
  api-gateway
    ↓ (all ready, system operational)
```

---

## 5. REQUEST PROCESSING FLOW

```
┌────────────────────────────────────────────────────────────────────┐
│              CUSTOMER CREATION - SEQUENCE DIAGRAM                  │
└────────────────────────────────────────────────────────────────────┘

Time
 │
 │  CLIENT              GATEWAY              CUSTOMER SERVICE
 │    │                    │                        │
 │    │  POST /api/customers
 │    ├────────────────────>│                        │
 │    │                     │ Add tracing ctx       │
 │    │                     │ Add request ID        │
 │    │                     │ Log request           │
 │    │                     ├─────────────────────>│
 │    │                     │ POST /customers      │
 │    │                     │                      │
 │    │                     │  Validate input      │
 │    │                     │  Check email unique  │
 │    │                     │  Create entity       │
 │    │                     │  Save to database    │
 │    │                     │                      │
 │    │                     │<─ 201 CREATED ──────┤
 │    │                     │ ID: 1                │
 │    │                     │ email: unique check  │
 │    │                     │                      │
 │    │<─ 201 CREATED ──────┤                      │
 │    │ Complete response   │                      │
 │    │                     │                      │
 ▼
```

---

## 6. SERVICE DEPENDENCIES

```
┌────────────────────────────────────────────────────────────────────┐
│                    SERVICE DEPENDENCY GRAPH                        │
└────────────────────────────────────────────────────────────────────┘

                    CONFIG SERVER (8888)
                            ▲
                            │ All services fetch config from here
                    ┌───────┼───────┬─────────┬─────────┐
                    │       │       │         │         │
                    ▼       ▼       ▼         ▼         ▼
            ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
            │ GATEWAY  │  │ REGISTRY │  │ CUSTOMER │  │ ACCOUNT  │
            │ (8080)   │  │ (8761)   │  │ (8081)   │  │ (8082)   │
            └────┬─────┘  └──────────┘  └────┬─────┘  └─────┬────┘
                 │                            │             │
        Depends on:                     Depends on:   Depends on:
        • SERVICE REGISTRY             • DATABASE    • DATABASE
        • CUSTOMER SERVICE                            • SERVICE REGISTRY
        • ACCOUNT SERVICE                            • CUSTOMER SERVICE


Startup Order (enforced by depends_on):
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│  1. CONFIG SERVER                                                │
│     ├─ Start immediately                                         │
│     └─ Make configurations available                             │
│         ↓                                                        │
│  2. SERVICE REGISTRY                                             │
│     ├─ Depends on: CONFIG SERVER healthy                         │
│     └─ Collect service registrations                             │
│         ↓                                                        │
│  3. CUSTOMER SERVICE & ACCOUNT SERVICE                           │
│     ├─ Depends on: SERVICE REGISTRY healthy                      │
│     └─ Register themselves                                       │
│         ↓                                                        │
│  4. API GATEWAY                                                  │
│     ├─ Depends on: SERVICE REGISTRY healthy                      │
│     └─ Discover and route to services                            │
│         ↓                                                        │
│  SYSTEM READY                                                    │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 7. TECHNOLOGY STACK DIAGRAM

```
┌────────────────────────────────────────────────────────────────────┐
│                    TECHNOLOGY LAYERED ARCHITECTURE                 │
└────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                       APPLICATION TIER                           │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  REST Controllers | Services | Repositories             │  │
│  │  (Spring Web, Spring Data JPA, Hibernate)               │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ▲
                              │ Uses
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    FRAMEWORK TIER                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Spring Boot 4.0.4 - Application Framework               │  │
│  │  Spring Cloud 2025.1.0 - Cloud-Native Framework          │  │
│  │  ┌──────────────────────────────────────────────────┐    │  │
│  │  │  Microservices Components                        │    │  │
│  │  ├─ Config Server (spring-cloud-config)            │    │  │
│  │  ├─ Service Registry (spring-cloud-eureka)         │    │  │
│  │  ├─ API Gateway (spring-cloud-gateway)             │    │  │
│  │  ├─ Circuit Breaker (resilience4j)                 │    │  │
│  │  ├─ Tracing (opentelemetry, micrometer)            │    │  │
│  │  └─ Monitoring (spring-boot-actuator)              │    │  │
│  │  └──────────────────────────────────────────────────┘    │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ▲
                              │ Built on
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      RUNTIME TIER                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Java 17 Runtime Environment (JDK)                      │  │
│  │  Tomcat (embedded in Spring Boot)                       │  │
│  │  JVM (Just-In-Time compiler, Garbage Collection)        │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ▲
                              │ Runs on
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    INFRASTRUCTURE TIER                           │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Docker Container (openjdk:17-jdk-slim)                 │  │
│  │  Docker Compose Orchestration                           │  │
│  │  Linux Operating System                                 │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ▲
                              │ Deployed on
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      STORAGE TIER                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  H2 Database (Development)                              │  │
│  │  MySQL Database (Production - Optional)                 │  │
│  │  JPA/Hibernate ORM Mapping                              │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 8. NETWORK ARCHITECTURE

```
┌────────────────────────────────────────────────────────────────────┐
│                      NETWORK TOPOLOGY                              │
└────────────────────────────────────────────────────────────────────┘

EXTERNAL NETWORK (Internet)
│
├─ Client requests arrive on
│  - http://localhost:8080/api/...
│  - Port 8080 exposed
│
│
DOCKER COMPOSE NETWORK
│
├─ network: banking-network
│  ├─ Bridge driver
│  ├─ Internal DNS (service-to-service)
│  │
│  ├─ api-gateway:8080
│  ├─ customer-service:8081
│  ├─ account-service:8082
│  ├─ service-registry:8761
│  └─ config-server:8888
│
│
CONTAINER-TO-CONTAINER COMMUNICATION
│
├─ account-service can reach customer-service via:
│  - http://customer-service:8081/customers/{id}
│
├─ All services discover each other via:
│  - Service Registry (Eureka)
│  - DNS in network: service-name:port
│
└─ All services config from:
    - http://config-server:8888
```

---

## 9. ACCOUNT CREATION WITH VALIDATION - FLOW

```
┌────────────────────────────────────────────────────────────────────┐
│         CREATE ACCOUNT - VALIDATION AND CIRCUIT BREAKER            │
└────────────────────────────────────────────────────────────────────┘

REQUEST:
POST /api/accounts
{
  "customerId": 1,
  "balance": 5000.0,
  "accountType": "SAVINGS"
}
│
▼
ACCOUNT SERVICE receives
│
├─ Extract customerId: 1
├─ Validate input
│  ├─ Balance > 0? ✓
│  ├─ AccountType valid? ✓
│  └─ CustomerId not null? ✓
│
▼
VALIDATE CUSTOMER EXISTS
│
├─ Call: Customer Service (8081)
│  │ GET /customers/1
│  │
│  │ Circuit Breaker State Machine:
│  │
│  ├─ CLOSED (normal)
│  │  └─ Forward request to customer-service
│  │     ├─ Success (200) ──► Continue
│  │     ├─ Failure (4xx/5xx) ──► Count failure
│  │     │   └─ After 5 failures ──► Switch to OPEN
│  │     └─ Timeout ──► Increment counter
│  │
│  ├─ OPEN (circuit broken)
│  │  ├─ Reject new requests immediately
│  │  └─ After 5 seconds timeout:
│  │     └─ Switch to HALF_OPEN
│  │
│  └─ HALF_OPEN (testing recovery)
│     ├─ Allow limited requests (1-2)
│     ├─ If success:
│     │  └─ Switch back to CLOSED
│     └─ If failure:
│        └─ Switch back to OPEN
│
▼
IF CUSTOMER EXISTS:
│
├─ Create Account object
├─ Set properties
├─ Call Repository.save()
│  ├─ Generate INSERT SQL
│  ├─ Execute on database
│  └─ Get ID back (42)
│
├─ Return Account (id=42)
└─ HTTP 201 CREATED

│
IF CUSTOMER NOT FOUND:
│
├─ Throw CustomerNotFoundException
├─ HTTP 400 Bad Request
└─ Error: "Customer 1 not found"

│
IF CIRCUIT BREAKER OPEN:
│
├─ Check fallback cache
├─ If in cache: Allow (assume customer valid)
└─ If not in cache: Throw ServiceUnavailable
    └─ HTTP 503 Service Unavailable
```

---

## 10. CONFIGURATION FLOW

```
┌────────────────────────────────────────────────────────────────────┐
│                  CONFIGURATION MANAGEMENT FLOW                     │
└────────────────────────────────────────────────────────────────────┘

SERVICE STARTUP SEQUENCE:

1. SERVICE STARTS
   │
   └─► Read bootstrap configuration
       • spring.application.name
       • CONFIG_SERVER_URL environment variable
       • EUREKA_SERVER_URL environment variable

2. CONTACT CONFIG SERVER
   │
   ├─► HTTP GET http://config-server:8888/{service-name}/default
   │
   └─► Receive JSON:
       {
         "name": "customer-service",
         "profiles": ["default"],
         "propertySources": [
           {
             "name": "customer-service.yaml",
             "source": {
               "server.port": "8081",
               "spring.jpa.hibernate.ddl-auto": "update",
               "management.tracing.sampling.probability": "1.0",
               "resilience4j.circuitbreaker...": {...}
             }
           }
         ]
       }

3. APPLY CONFIGURATION
   │
   ├─► Load all properties
   ├─► Override defaults
   ├─► Set environment-specific values
   └─► Configure Spring Beans

4. REGISTER WITH SERVICE REGISTRY
   │
   ├─► HTTP POST to Eureka
   ├─► Service Name: customer-service
   ├─► Instance URL: http://localhost:8081
   ├─► Health Check: /actuator/health
   └─► Status: UP

5. START ACCEPTING REQUESTS
   │
   └─► Ready on configured port

RUNTIME CONFIGURATION UPDATE:

Service receives request ──► Process with config ──► Use properties
                                   │
                                   ├─ If property changed in Config Server
                                   └─ Spring Cloud Config Client refresh:
                                      • Actuator endpoint /actuator/refresh
                                      • Or automatic refresh (if enabled)
                                      • Re-fetch config
                                      • Update beans

CONFIGURATION SOURCES (Priority Order):

1. Environment Variables (highest)
2. System Properties
3. Config Server
4. Application YAML
5. Default values (lowest)
```

---

## SUMMARY

This document provides visual representations of the Banking Microservices Architecture including:

✓ High-level system topology
✓ Service interaction patterns
✓ Data flow during operations
✓ Container deployment structure
✓ Technology stack layers
✓ Network topology
✓ Sequence flows
✓ Dependency relationships
✓ Configuration management
✓ Circuit breaker patterns

All diagrams are ASCII art for compatibility with:
- Text editors
- Markdown viewers
- Microsoft Word (paste as formatted text)
- GitHub/GitLab documentation
- Confluence pages

---

**Version**: 1.0  
**Date**: March 24, 2026  
**Status**: Complete & Production Ready

