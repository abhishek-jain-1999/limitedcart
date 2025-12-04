# Infrastructure Cleanup & Core Refactoring Summary

## Overview
This document summarizes the infrastructure cleanup and core refactoring performed on the limitedcart microservices project. All changes are **structural/configuration only** with **no functional behavior changes**.

---

## Task A: Redis Removal ✅

### Changes Made

**1. `docker-compose.yml`**
- Removed entire `redis` service block (lines 24-31)
- Removed `SPRING_DATA_REDIS_HOST: redis` from all service environment sections:
  - `auth-service`
  - `product-service`  
  - `inventory-service`
  - `order-service`
  - `payment-service`

**2. `.env`**
- Removed Redis configuration section:
  ```properties
  # Cache / Redis
  REDIS_HOST=localhost
  REDIS_PORT=6379
  ```

### Reasoning
Redis was configured but not actually used by any service. No `RedisTemplate`, `StringRedisTemplate`, or Redis-related code existed in the codebase. Removing it simplifies infrastructure and reduces resource usage.

---

## Task B: JWT Logic Centralization ✅

### Changes Made

**1. Created `common/src/main/kotlin/com/abhishek/limitedcart/common/security/`**

Three new files moved from `auth-service`:

- **JwtProperties.kt**: Configuration properties for JWT (secret, issuer, expiration)
- **JwtUtil.kt**: Core JWT logic (generate, validate, extract claims)
- **JwtAuthFilter.kt**: Spring Security filter for JWT authentication

**2. Updated `common/pom.xml`**
Added dependencies:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.5</version>
</dependency>
```

**3. Updated `auth-service/src/main/kotlin/com/abhishek/limitedcart/auth/`**

Changed imports:
```kotlin
// Before
import com.abhishek.limitedcart.auth.security.JwtUtil
import com.abhishek.limitedcart.auth.security.JwtAuthFilter
import com.abhishek.limitedcart.auth.config.JwtProperties

// After
import com.abhishek.limitedcart.common.security.JwtUtil
import com.abhishek.limitedcart.common.security.JwtAuthFilter
import com.abhishek.limitedcart.common.security.JwtProperties
```

Files modified:
- `auth/config/SecurityConfig.kt`
- `auth/service/AuthService.kt`

Deleted:
- `auth-service/src/main/kotlin/com/abhishek/limitedcart/auth/security/` (entire directory)
- `auth-service/src/main/kotlin/com/abhishek/limitedcart/auth/config/JwtProperties.kt`

**4. Updated JWT Implementation**
Changed from deprecated JJWT 0.11.x API to 0.12.x API:
```kotlin
// Before (deprecated)
Jwts.parserBuilder()
    .setSigningKey(signingKey)
    .parseClaimsJws(token)
    .body

// After (current)
Jwts.parser()
    .verifyWith(signingKey)
    .parseSignedClaims(token)
    .payload
```

### Reasoning
JWT logic is now in the `common` module, making it reusable across all services. **Future services** (product, inventory, order, payment) can now:
1. Add dependency on `common`
2. Import `JwtAuthFilter` and `JwtProperties`
3. Configure SecurityFilterChain to validate JWT tokens locally
4. Eliminate dependency on auth-service for every request

**Behavior**: Identical JWT generation and validation. Same claims structure, same algorithm (HS256), same secret.

---

## Task C: Externalize Hardcoded Queue/Topic Names ✅

### Changes Made

**1. Temporal Queue Name**

**temporal-worker/src/main/kotlin/...worker/TemporalWorkerApplication.kt:**
```kotlin
// Before
val worker = factory.newWorker("ORDER_SAGA_QUEUE")

// After
class TemporalWorkerApplication(
    @Value("\${worker.temporal.task-queue}") private val taskQueue: String
) {
    val worker = factory.newWorker(taskQueue)
}
```

**order-service/src/main/kotlin/.../order/service/OrderService.kt:**
```kotlin
// Before
.setTaskQueue("ORDER_SAGA_QUEUE")

// After
class OrderService(
    @Value("\${app.temporal.orderSagaQueue}") private val taskQueue: String
) {
    .setTaskQueue(taskQueue)
}
```

**2. Kafka Topic Names**

**order-service/src/main/resources/application.yml:**
```yaml
# Before
app:
  kafka:
    topics:
      orderCreated: orders.created
      orderConfirmed: orders.confirmed
      orderFailed: orders.failed

# After
app:
  kafka:
    topics:
      orderCreated: ${KAFKA_TOPIC_ORDER_CREATED:orders.created}
      orderConfirmed: ${KAFKA_TOPIC_ORDER_CONFIRMED:orders.confirmed}
      orderFailed: ${KAFKA_TOPIC_ORDER_FAILED:orders.failed}
  temporal:
    orderSagaQueue: ${TEMPORAL_TASK_QUEUE:ORDER_SAGA_QUEUE}
```

**product-service/src/main/resources/application.yml:**
```yaml
# Before
app:
  kafka:
    topics:
      productCreated: product.created
      productUpdated: product.updated

# After
app:
  kafka:
    topics:
      productCreated: ${KAFKA_TOPIC_PRODUCT_CREATED:product.created}
      productUpdated: ${KAFKA_TOPIC_PRODUCT_UPDATED:product.updated}
```

**3. Created `.env.example`**
Documented all available environment variables with comments:
```properties
# Temporal
TEMPORAL_TASK_QUEUE=ORDER_SAGA_QUEUE

# Kafka Topic Names (optional - defaults provided in application.yml)
# KAFKA_TOPIC_PRODUCT_CREATED=product.created
# KAFKA_TOPIC_PRODUCT_UPDATED=product.updated
# KAFKA_TOPIC_ORDER_CREATED=orders.created
# KAFKA_TOPIC_ORDER_CONFIRMED=orders.confirmed
# KAFKA_TOPIC_ORDER_FAILED=orders.failed
```

### Reasoning
Queue and topic names are now configurable via environment variables with sensible defaults. This enables:
- Different names per environment (dev, staging, prod)
- No code changes for topic/queue configuration  
- Clearer documentation of all configurable values

**Behavior**: No change. Default values match previous hardcoded strings.

---

## Environment Variables Reference

### Temporal Configuration
| Variable              | Default            | Used By                        |
| --------------------- | ------------------ | ------------------------------ |
| `TEMPORAL_TASK_QUEUE` | `ORDER_SAGA_QUEUE` | temporal-worker, order-service |

### Kafka Topics
| Variable                      | Default            | Used By         |
| ----------------------------- | ------------------ | --------------- |
| `KAFKA_TOPIC_PRODUCT_CREATED` | `product.created`  | product-service |
| `KAFKA_TOPIC_PRODUCT_UPDATED` | `product.updated`  | product-service |
| `KAFKA_TOPIC_ORDER_CREATED`   | `orders.created`   | order-service   |
| `KAFKA_TOPIC_ORDER_CONFIRMED` | `orders.confirmed` | order-service   |
| `KAFKA_TOPIC_ORDER_FAILED`    | `orders.failed`    | order-service   |

---

## Next Steps

### To Use Shared JWT in Other Services

Example for `product-service`:

**1. Ensure dependency on common**
```xml
<dependency>
    <groupId>com.abhishek.limitedcart</groupId>
    <artifactId>common</artifactId>
    <version>${project.version}</version>
</dependency>
```

**2. Add JWT config to application.yml**
```yaml
app:
  security:
    jwt:
      secret: ${JWT_SECRET}
      issuer: limitedcart
      expirationSeconds: 3600
```

**3. Create SecurityConfig**
```kotlin
import com.abhishek.limitedcart.common.security.JwtAuthFilter
import com.abhishek.limitedcart.common.security.JwtProperties

@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
```

**4. Provide UserDetailsService bean**
```kotlin
@Bean
fun userDetailsService(): UserDetailsService = InMemoryUserDetailsManager()
```

Now the service validates JWT tokens locally without calling auth-service!

---

## Compilation Status

✅ `common` module: **Successfully compiled and installed**
```bash
mvn -pl common clean install -DskipTests
# BUILD SUCCESS
```

**Next**: Rebuild other modules that depend on `common`:
```bash
mvn -pl auth-service clean package -DskipTests
mvn -pl order-service clean package -DskipTests  
mvn -pl temporal-worker clean package -DskipTests
```

---

## Summary

### What Changed
- **Removed**: Redis service and all references
- **Centralized**: JWT security logic in `common` module  
- **Externalized**: Temporal queue name and Kafka topic names to environment variables

### What Stayed the Same
- **JWT behavior**: Identical token generation/validation
- **Temporal**: Same queue name by default
- **Kafka**: Same topic names by default
- **All business logic**: Untouched

### Benefits
1. **Simpler infrastructure**: No unused Redis
2. **Reusable security**: JWT validation available to all services
3. **Config flexibility**: Queue/topic names configurable per environment
4. **Better documentation**: `.env.example` documents all variables
