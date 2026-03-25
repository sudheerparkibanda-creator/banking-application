# Fix Summary: API Gateway and Service Registry Issues in Ubuntu Docker

## Problems Identified

### Problem 1: Docker Network DNS Resolution Issues

**Root Cause:**
When services run in Docker containers, `localhost` (127.0.0.1) refers to the container itself, not the Docker host. Services were trying to connect to:
- `http://localhost:8888` (Config Server)
- `http://localhost:8761/eureka/` (Service Registry)

Inside the container, these URLs point to the container itself, causing connection failures.

**Impact:**
- Services couldn't register with Eureka Service Registry
- API Gateway couldn't discover other services
- Cross-service communication failed

**Solution:**
Changed all localhost references to Docker container hostnames:
- `http://config-server:8888` instead of `http://localhost:8888`
- `http://service-registry:8761/eureka/` instead of `http://localhost:8761/eureka/`

---

### Problem 2: API Gateway Missing Service Discovery Configuration

**Root Cause:**
The API Gateway had `@EnableDiscoveryClient` annotation but lacked the required Eureka client configuration in `application.yaml`.

**Impact:**
- API Gateway didn't register with Eureka
- Failed to discover downstream services (customer-service, account-service)

**Solution:**
Added complete Eureka client configuration to API Gateway's `application.yaml`:
```yaml
eureka:
  client:
    service-url:
      default-zone: ${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:http://localhost:8761/eureka/}
    register-with-eureka: true
    fetch-registry: true
  instance:
    hostname: ${EUREKA_INSTANCE_HOSTNAME:localhost}
```

---

### Problem 3: Incomplete Eureka Configuration in Services

**Root Cause:**
Account and Customer services had Spring Cloud Config import but missing explicit Eureka client registration properties.

**Impact:**
- Unreliable service registration
- Services may timeout during startup waiting for Eureka discovery

**Solution:**
Added complete Eureka client configuration to both account-service and customer-service `application.yaml` files.

---

### Problem 4: Docker Compose Environment Variable Misalignment

**Root Cause:**
Docker-compose.yml had unused environment variables and incorrect hostname configuration:
- `EUREKA_SERVER_URL=http://localhost:8761/eureka/` (incorrect)
- `SPRING_PROFILES_ACTIVE=native` (not used by clients)

**Impact:**
- Services received conflicting configuration
- Startup delays due to health check retries

**Solution:**
Updated docker-compose.yml with correct environment variables:
```yaml
environment:
  - CONFIG_SERVER_URL=http://config-server:8888
  - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://service-registry:8761/eureka/
  - EUREKA_INSTANCE_HOSTNAME=service-name
```

---

## Files Modified

### 1. API Gateway Configuration
**File:** `api-gateway/src/main/resources/application.yaml`
- Added Eureka client configuration
- Added Eureka instance hostname configuration

### 2. Customer Service Configuration
**File:** `customer-service/src/main/resources/application.yaml`
- Added Eureka client service-url configuration
- Added Eureka instance hostname configuration

### 3. Account Service Configuration
**File:** `account-service/src/main/resources/application.yaml`
- Added Eureka client service-url configuration
- Added Eureka instance hostname configuration

### 4. Service Registry Configuration
**File:** `service-registry/src/main/resources/application.yaml`
- Already had correct configuration, verified consistency

### 5. Config Server Configuration
**File:** `config-server/src/main/resources/application.yaml`
- Added Eureka instance hostname configuration for consistency

### 6. Docker Compose Configuration
**File:** `config-server/docker-compose.yml`
- Updated all service environment variables to use container DNS names
- Added `EUREKA_INSTANCE_HOSTNAME` for all services
- Removed incorrect `EUREKA_SERVER_URL` and `SPRING_PROFILES_ACTIVE` variables
- Updated `CONFIG_SERVER_URL` to use container hostname

---

## Key Changes Summary

| Component | Old Configuration | New Configuration | Reason |
|-----------|-------------------|-------------------|--------|
| Config Server URL | `localhost:8888` | `config-server:8888` | Docker DNS resolution |
| Eureka URL | `localhost:8761` | `service-registry:8761` | Docker DNS resolution |
| API Gateway | No Eureka config | Full Eureka client config | Enable service discovery |
| All Services | Partial Eureka config | Complete Eureka config | Reliable registration |
| Hostname | localhost (default) | Container name (env var) | Proper Eureka registration |

---

## Testing the Fix

### 1. Verify Service Registration
```bash
curl http://localhost:8761/eureka/apps
```
Should return all registered instances.

### 2. Test Customer API
```bash
curl -X POST http://localhost:8080/api/customers \
  -H 'Content-Type: application/json' \
  -d '{"name":"Test","email":"test@example.com","phone":"1234","address":"Test St"}'
```

### 3. Test Account Creation
```bash
curl -X POST http://localhost:8080/api/accounts \
  -H 'Content-Type: application/json' \
  -d '{"customerId":1,"balance":1000.0,"accountType":"SAVINGS"}'
```

### 4. Verify Inter-Service Communication
Services should successfully communicate via API Gateway without connection errors.

---

## Deployment Checklist for Ubuntu Docker

- [x] Docker Compose file uses container hostnames (not localhost)
- [x] All services have EUREKA_INSTANCE_HOSTNAME environment variable
- [x] All services have EUREKA_CLIENT_SERVICEURL_DEFAULTZONE pointing to service-registry
- [x] API Gateway has Eureka client configuration
- [x] All services are on same Docker network (banking-network)
- [x] Health checks are properly configured
- [x] Service dependencies are correctly ordered (depends_on)

---

## Prevention Tips for Future Issues

1. **Use Docker DNS Names**: Always use container names for inter-container communication, never localhost
2. **Environment Variables**: Clearly document all required environment variables
3. **Health Checks**: Verify health endpoints work before depending on them
4. **Service Discovery**: Ensure all Eureka clients have proper configuration
5. **Testing**: Test service registration and discovery during deployment
6. **Logging**: Enable debug logging for Eureka client during troubleshooting

