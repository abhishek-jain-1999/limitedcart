# Production Deployment Guide

## Pre-Deployment Checklist

### ✅ Infrastructure
- [x] Redis 7-alpine with AOF + RDB persistence
- [x] Kafka with proper retention policies
- [x] PostgreSQL for all services
- [x] Temporal server running
- [x] All environment variables configured

### ✅ Configuration
All settings externalized to `.env`:
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`
- `KAFKA_BOOTSTRAP`, topic names
- Database connection strings
- Temporal task queue names

### ✅ Data Synchronization
- [x] Product prices sync automatically via `product.events` topic
- [x] Inventory stock syncs on startup via `StartupStockSyncListener`
- [x] Manual sync available: `POST /inventory/admin/sync-redis`

### ✅ Error Handling
- [x] Idempotency checks in Kafka consumers
- [x] Manual Kafka ACK for reliability
- [x] Atomic Redis operations (Lua scripts)
- [x] Temporal compensation flows

## Deployment Steps

### 1. Infrastructure Setup
```bash
# Start all services
docker-compose up -d

# Verify Redis
docker exec redis redis-cli ping
# Expected: PONG

# Verify Kafka
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092
# Expected: product.events, orders.reservations, etc.
```

### 2. Initial Data Sync
```bash
# Sync prices to Redis (happens automatically via ProductEventConsumer)
# Sync stock to Redis
curl -X POST http://localhost:8080/inventory/admin/sync-redis \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

### 3. Verify Reconciliation
```bash
# Check Redis vs DB consistency
curl http://localhost:8080/admin/reconcile \
  -H "Authorization: Bearer <ADMIN_TOKEN>"

# Expected: All products show status: OK
```

### 4. Test Order Flow
```bash
# Place test order
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "X-User-Id: test-user" \
  -d '{
    "productId": "test-product-123",
    "quantity": 1
  }'

# Expected: HTTP 202 with orderId
```

### 5. Monitor Consumer Lag
```bash
# Check Kafka consumer groups
docker exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group order-processor-group
```

## Monitoring & Alerts

### Key Metrics to Monitor
1. **Order API Latency**: Target p99 < 50ms
2. **Redis Hit Rate**: Should be > 99%
3. **Kafka Consumer Lag**: Should be < 100 messages
4. **Stock Reconciliation**: Run daily, alert on CRITICAL status
5. **Redis Memory**: Monitor for OOM

### Health Checks
- Product Service: `http://product:8080/actuator/health`
- Inventory Service: `http://inventory:8080/actuator/health`
- Order Service: `http://order:8080/actuator/health`
- Redis: `redis-cli ping`

## Rollback Strategy

### If Issues Occur

**Option 1: Disable Gatekeeper**
1. Revert `OrderService.createOrder()` to original sync version
2. Redeploy Order Service
3. Kafka consumers continue processing buffered orders

**Option 2: Redis Failure**
1. Orders will fail-fast with "Price not available"
2. Fix Redis connection
3. Run `POST /admin/sync-redis` to restore data
4. Resume operations

**Option 3: Kafka Failure**
1. Orders will fail to publish
2. No stock is reserved (atomic operation fails together)
3. Fix Kafka
4. Resume operations (no data loss)

## Performance Tuning

### Redis
- Connection pool: `max-active: 8` (adjust based on load)
- Timeout: `2000ms`
- Persistence: AOF everysec (balance durability/performance)

### Kafka
- Consumer group: `order-processor-group`
- Manual ACK prevents data loss
- Partitioning by orderId ensures ordering

### Database
- Connection pools per service
- Read replicas for reporting queries
- Indexes on orderId, productId

## Security Checklist

- [x] Admin endpoints secured with `@PreAuthorize("hasRole('ADMIN')")`
- [x] JWT authentication on all APIs
- [x] Redis password protected (if configured)
- [ ] TLS for Kafka (production deployment)
- [ ] TLS for Redis (production deployment)
- [ ] Network isolation (services in private network)

## Disaster Recovery

### Redis Data Loss
1. Redis will auto-rebuild from AOF/RDB
2. If corruption, run: `POST /admin/sync-redis`
3. Verify with: `GET /admin/reconcile`

### Database Corruption
1. Restore from backup
2. Redis may have stale data
3. Run reconciliation to identify mismatches
4. Manual correction or resync

## Production Configuration Recommendations

### Redis
```yaml
# docker-compose.yml
redis:
  command: >
    redis-server
    --appendonly yes
    --appendfsync everysec
    --maxmemory 2gb
    --maxmemory-policy allkeys-lru
```

### Kafka
```yaml
# Increase retention for orders topic
KAFKA_CFG_LOG_RETENTION_HOURS: 168  # 7 days
KAFKA_CFG_LOG_RETENTION_BYTES: 1073741824  # 1GB
```

### Application
```yaml
# application.yml for all services
spring:
  redis:
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 16  # Increase for production
        max-idle: 8
        min-idle: 4
  kafka:
    consumer:
      max-poll-records: 100
      session-timeout-ms: 30000
```

## Next Steps

1. **Load Testing**: Run K6 tests to validate p99 < 50ms
2. **Monitoring Setup**: Integrate with Prometheus/Grafana
3. **Alert Configuration**: Set up PagerDuty/Slack alerts
4. **Frontend Integration**: Implement Phase 8 (see separate document)
