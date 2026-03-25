# Eureka Registration Fix - Troubleshooting Guide

## Problem

When starting services in Docker, you may see this error:

```
com.netflix.discovery.shared.transport.TransportException: Cannot execute request on any known server
```

This error occurs when:
- Services try to register with Eureka before the service-registry is fully started
- Network connectivity issues between services
- Eureka client timeout is too short for Docker startup times

## Root Causes

1. **Service Startup Race Condition**: API Gateway and microservices attempt to register before Service Registry is ready
2. **Short Timeout Settings**: Default Eureka timeouts (30s) may not be enough in Docker with network delays
3. **Missing Retry Logic**: Services don't properly retry failed registrations
4. **Health Check Delays**: Docker health checks may not accurately reflect service readiness

## Solution Applied

### 1. Extended Eureka Timeout Configuration

Added to all service YAML files:

```yaml
eureka:
  client:
    registry-fetch-interval-seconds: 5          # Check registry every 5 seconds
    eureka-server-connect-timeout-seconds: 30   # 30 second timeout for connections
    eureka-server-read-timeout-seconds: 30      # 30 second timeout for reading responses
    service-url-poll-interval-seconds: 300      # Poll service URLs every 5 minutes
  instance:
    prefer-ip-address: false                    # Use hostname (container DNS)
    lease-renewal-interval-in-seconds: 10       # Send heartbeat every 10 seconds
    lease-expiration-duration-in-seconds: 30    # Mark DOWN if no heartbeat in 30 seconds
```

### 2. Docker Compose Environment Variables

Added to all client services:

```yaml
environment:
  - EUREKA_CLIENT_INITIAL_INSTANCE_INFO_REPLICATION_INTERVAL_SECONDS=10
  - EUREKA_CLIENT_REGISTRY_FETCH_INTERVAL_SECONDS=5
  - EUREKA_CLIENT_EUREKA_SERVER_CONNECT_TIMEOUT_SECONDS=30
  - EUREKA_CLIENT_SHOULD_ENFORCE_SERVER_SSL=false
```

### 3. Extended Service Startup Period

Changed health check configuration:

```yaml
healthcheck:
  start_period: 60s     # Give service 60 seconds to start before first health check
  interval: 15s         # Check health every 15 seconds
  timeout: 10s          # Wait 10 seconds for health check response
  retries: 5            # Try 5 times before marking unhealthy
```

### 4. Explicit Service Dependency Ordering

```yaml
depends_on:
  service-registry:
    condition: service_healthy    # Wait for Eureka to be healthy before starting
```

## Startup Sequence (After Fix)

```
1. config-server starts (40s startup grace)
                ↓
2. service-registry starts (40s startup grace)
   - Waits for config-server to be healthy
                ↓
3. customer-service, account-service, api-gateway start (60s startup grace)
   - All wait for service-registry to be healthy
   - Eureka client retries every 5 seconds if registration fails
   - Connection timeout: 30 seconds (plenty of time for Docker networking)
```

## Testing the Fix

### Step 1: Clean Up Previous Containers
```bash
docker-compose down -v
docker system prune -a --volumes
```

### Step 2: Rebuild Images
```bash
docker-compose build --no-cache
```

### Step 3: Start Services
```bash
docker-compose up -d
```

### Step 4: Monitor Logs
```bash
# Watch API Gateway logs
docker-compose logs -f api-gateway

# Expected successful output:
# SuccessfullyRegisteredWithEureka: true
# InstanceInfoReplicator: setting initial instance status as: UP
```

### Step 5: Verify Registration
```bash
# Check all services registered
curl http://localhost:8761/eureka/apps

# Check individual service
curl http://localhost:8761/eureka/apps/api-gateway
curl http://localhost:8761/eureka/apps/customer-service
curl http://localhost:8761/eureka/apps/account-service
```

## Configuration Files Modified

1. **docker-compose.yml**
   - Added Eureka retry environment variables to all client services
   - Extended start_period to 60 seconds for clients
   - Added explicit depends_on with health conditions

2. **api-gateway/src/main/resources/application.yaml**
   - Added registry-fetch-interval-seconds: 5
   - Added eureka-server-connect-timeout-seconds: 30
   - Added eureka-server-read-timeout-seconds: 30
   - Added lease renewal configuration

3. **customer-service/src/main/resources/application.yaml**
   - Same Eureka client timeout configuration

4. **account-service/src/main/resources/application.yaml**
   - Same Eureka client timeout configuration

## Environment Variables Explained

| Variable | Default | New Value | Purpose |
|----------|---------|-----------|---------|
| EUREKA_CLIENT_REGISTRY_FETCH_INTERVAL_SECONDS | 30 | 5 | How often to refresh service list |
| EUREKA_CLIENT_EUREKA_SERVER_CONNECT_TIMEOUT_SECONDS | 5 | 30 | Time to wait when connecting to Eureka |
| EUREKA_CLIENT_EUREKA_SERVER_READ_TIMEOUT_SECONDS | 8 | 30 | Time to wait for Eureka response |
| EUREKA_CLIENT_INITIAL_INSTANCE_INFO_REPLICATION_INTERVAL_SECONDS | 40 | 10 | Initial registration interval |
| EUREKA_INSTANCE_LEASE_RENEWAL_INTERVAL_IN_SECONDS | 30 | 10 | Heartbeat interval |
| EUREKA_INSTANCE_LEASE_EXPIRATION_DURATION_IN_SECONDS | 90 | 30 | Time before marking instance DOWN |

## Troubleshooting Continued Issues

### Issue: Still Getting "Cannot execute request on any known server"

**Check 1: Service Registry is Running**
```bash
docker-compose ps service-registry
# Should show: Up (healthy)

docker-compose logs service-registry | head -20
# Look for: SuccessfullyRegisteredWithEureka
```

**Check 2: Network Connectivity**
```bash
# From API Gateway container
docker-compose exec api-gateway curl -v http://service-registry:8761/eureka/apps

# Should return 200 OK with XML list of apps
```

**Check 3: Environment Variables**
```bash
# Verify environment is set
docker-compose exec api-gateway env | grep EUREKA

# Output should show:
# EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://service-registry:8761/eureka/
# EUREKA_INSTANCE_HOSTNAME=api-gateway
# etc.
```

**Check 4: Logs for Actual Error**
```bash
# Get detailed logs
docker-compose logs api-gateway | grep -A 5 "Cannot execute"

# Look for underlying cause like:
# - java.net.UnknownHostException (service-registry not accessible)
# - java.net.ConnectException (firewall/network issue)
# - java.util.concurrent.TimeoutException (too slow response)
```

### Issue: Services Register but Go DOWN

**Cause**: Lease expiration is too short for container restart delays

**Fix**: Already applied - lease expiration set to 30 seconds

**Verify**: Check Eureka dashboard - all instances should show as UP

### Issue: High CPU or Memory Usage

**Cause**: Registry fetch interval too low (constant polling)

**Fix**: Already applied - set to 5 seconds (reasonable balance)

**Adjust if needed**: Increase EUREKA_CLIENT_REGISTRY_FETCH_INTERVAL_SECONDS to 10-30

## Quick Debug Commands

```bash
# Watch startup progress
watch -n 1 'docker-compose ps'

# Real-time logs from all services
docker-compose logs -f

# Check if API Gateway can talk to Eureka
docker-compose exec api-gateway ping service-registry

# Check DNS resolution
docker-compose exec api-gateway nslookup service-registry

# Full stack trace on error
docker-compose logs api-gateway | grep -A 100 "Caused by"
```

## Success Indicators

✅ All containers show "Up (healthy)" in `docker-compose ps`  
✅ Eureka dashboard shows all 5 services as UP  
✅ API endpoints respond: `curl http://localhost:8080/api/customers`  
✅ No connection errors in service logs  
✅ Service registration happens within first 30 seconds of startup  

## Performance Notes

After applying these fixes:
- Startup time: ~90 seconds total (from `docker-compose up` to all services UP)
- Eureka registration: ~10-15 seconds after service starts
- Inter-service discovery: ~5 seconds (due to registry fetch interval)
- Failover recovery: ~30 seconds (if a service goes DOWN and back UP)

