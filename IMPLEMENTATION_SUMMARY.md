# Technical Implementation Summary
## LimitedCart Microservices Platform

**Project Status:** Phase 1 (Infrastructure & Setup) and Phase 2 (Core Services) Complete  
**Technology Stack:** Spring Boot 3.2.5, Kotlin 1.9.24, PostgreSQL 15, Kafka 3.6 (KRaft), Redis 7, Temporal 1.22.4  
**Architecture:** Microservices with Database-per-Service Pattern  
**Date Generated:** 2025-12-01

---

## **1. PROJECT FOLDER ARCHITECTURE**

```
limitedcart/
│
├── build.gradle.kts                      # Root Gradle build configuration
├── settings.gradle.kts                   # Multi-module project settings
├── pom.xml                               # Maven configuration (if needed)
├── docker-compose.yml                    # Infrastructure orchestration
├── .gitignore                            # Git ignore patterns
│
├── docker/
│   └── init/
│       └── postgres/
│           └── init-multiple-databases.sh  # Creates auth_db, inventory_db, products_db, temporal_db
│
├── common/                               # Shared library module
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/abhishek/limitedcart/common/
│       ├── config/
│       │   └── JpaConfig.kt              # Enables JPA auditing
│       ├── entity/
│       │   └── BaseEntity.kt             # Abstract entity with id, createdAt, updatedAt
│       ├── error/
│       │   └── ErrorResponse.kt          # Standardized error response DTO
│       ├── exception/
│       │   ├── GlobalExceptionHandler.kt # @RestControllerAdvice for centralized exception handling
│       │   ├── OutOfStockException.kt    # Custom exception for inventory conflicts
│       │   └── ResourceNotFoundException.kt  # Custom 404 exception
│       ├── model/                        # (Empty - for future shared models)
│       └── util/
│           └── SharedUtils.kt            # Correlation ID generation, common utilities
│
├── auth-service/                         # Authentication & Authorization Service
│   ├── build.gradle.kts
│   └── src/main/
│       ├── kotlin/com/abhishek/limitedcart/auth/
│       │   ├── config/
│       │   │   ├── JwtProperties.kt      # JWT configuration properties binding
│       │   │   └── SecurityConfig.kt     # Spring Security configuration
│       │   ├── controller/
│       │   │   └── AuthController.kt     # REST endpoints: /auth/register, /auth/login
│       │   ├── dto/
│       │   │   └── AuthDtos.kt           # LoginRequest, RegisterRequest, AuthTokenResponse, UserView
│       │   ├── entity/
│       │   │   └── User.kt               # JPA entity with email, passwordHash, roles
│       │   ├── repository/
│       │   │   └── UserRepository.kt     # Spring Data JPA repository
│       │   ├── security/
│       │   │   ├── CustomUserDetailsService.kt  # Loads user for authentication
│       │   │   ├── JwtAuthFilter.kt      # OncePerRequestFilter for token validation
│       │   │   └── JwtUtil.kt            # Token generation, parsing, validation
│       │   └── service/
│       │       └── AuthService.kt        # Business logic: register, login, password hashing
│       └── resources/
│           ├── application.yml           # Database: auth_db, Flyway, JWT config
│           └── db/migration/
│               └── V1__init_auth.sql     # Creates users, user_roles tables
│
├── product-service/                      # Product Catalog Service
│   ├── build.gradle.kts
│   └── src/main/
│       ├── kotlin/com/abhishek/limitedcart/product/
│       │   ├── controller/
│       │   │   └── ProductController.kt  # REST endpoints: POST /products, GET /products/{id}, GET /products
│       │   ├── entity/
│       │   │   └── ProductEntity.kt      # JPA entity: name, description, price, maxQuantityPerSale, active
│       │   ├── repository/
│       │   │   └── ProductRepository.kt  # Spring Data JPA repository
│       │   └── service/
│       │       ├── ProductService.kt     # Business logic: create, get, list products
│       │       └── dto/
│       │           └── ProductDtos.kt    # CreateProductRequest, ProductResponse, ProductListResponse
│       └── resources/
│           ├── application.yml           # Database: products_db, Flyway config
│           └── db/migration/
│               └── V1__init_products.sql # Creates products table
│
├── inventory-service/                    # Stock Management Service
│   ├── build.gradle.kts
│   └── src/main/
│       ├── kotlin/com/abhishek/limitedcart/inventory/
│       │   ├── config/
│       │   │   └── InventoryConfig.kt    # Spring Retry configuration
│       │   ├── controller/
│       │   │   └── InventoryController.kt  # REST: /inventory/reserve, /confirm, /release
│       │   ├── dto/
│       │   │   └── InventoryDtos.kt      # ReserveRequest, ConfirmReservationRequest, ReleaseReservationRequest, ReserveResponse
│       │   ├── entity/
│       │   │   ├── Stock.kt              # productId, availableQuantity, @Version (optimistic locking)
│       │   │   └── Reservation.kt        # orderId (unique), productId, quantity, status, expiresAt
│       │   ├── exception/                # (Empty - uses common module exceptions)
│       │   ├── repository/
│       │   │   ├── StockRepository.kt    # Spring Data JPA repository
│       │   │   └── ReservationRepository.kt  # Includes findByOrderId for idempotency
│       │   └── service/
│       │       └── InventoryService.kt   # @Transactional + @Retryable logic: reserve, confirm, release
│       └── resources/
│           ├── application.yml           # Database: inventory_db, Flyway config
│           └── db/migration/
│               └── V1__init_inventory.sql  # Creates stock, reservations tables with version column
│
├── order-service/                        # (Placeholder for Phase 3)
│   └── src/
│
├── payment-service/                      # (Placeholder for Phase 3)
│   └── src/
│
└── temporal-worker/                      # (Placeholder for Phase 3)
    └── src/
```

---

## **2. MODULE & FILE RESPONSIBILITIES**

### **Module: `common`**
*Purpose: Shared utilities, base entities, and global exception handling used across all microservices.*

#### Files:
- **`config/JpaConfig.kt`**: Annotated with `@EnableJpaAuditing` to automatically populate `createdAt` and `updatedAt` fields in `BaseEntity`.
- **`entity/BaseEntity.kt`**: Abstract `@MappedSuperclass` with UUID `id`, `@CreatedDate createdAt`, and `@LastModifiedDate updatedAt` fields. All service entities extend this.
- **`error/ErrorResponse.kt`**: Data class for standardized error responses containing `status`, `error`, `message`, `correlationId`, and `path`.
- **`exception/GlobalExceptionHandler.kt`**: `@RestControllerAdvice` that maps exceptions to HTTP responses:
  - `OutOfStockException` → 409 Conflict
  - `ResourceNotFoundException` → 404 Not Found
  - `MethodArgumentNotValidException` → 400 Bad Request (validation errors)
  - `ResponseStatusException` → Custom status codes
  - Generic `Exception` → 500 Internal Server Error
- **`exception/OutOfStockException.kt`**: Custom exception thrown when inventory quantity is insufficient during reservation.
- **`exception/ResourceNotFoundException.kt`**: Custom exception thrown when a requested resource (product, stock, reservation) doesn't exist.
- **`util/SharedUtils.kt`**: Utility functions including correlation ID generation for request tracing.

---

### **Module: `auth-service`**
*Purpose: User authentication, JWT token generation/validation, and role-based access control.*

#### Files:
- **`controller/AuthController.kt`**: Exposes REST endpoints:
  - `POST /auth/register`: Creates new user account
  - `POST /auth/login`: Authenticates user and returns JWT token
- **`service/AuthService.kt`**: Business logic for:
  - User registration with BCrypt password hashing
  - Login validation and JWT token generation
  - Password verification against stored hash
- **`repository/UserRepository.kt`**: Spring Data JPA repository with custom query `findByEmail(email: String)`.
- **`entity/User.kt`**: JPA entity extending `BaseEntity` with fields:
  - `email`: Unique identifier
  - `passwordHash`: BCrypt-hashed password
  - `roles`: Set of role names (e.g., "ADMIN", "USER")
- **`security/JwtUtil.kt`**: Utility class for:
  - Generating signed JWT tokens with claims (userId, email, roles)
  - Parsing and validating tokens
  - Extracting user information from token claims
- **`security/JwtAuthFilter.kt`**: `OncePerRequestFilter` that:
  - Intercepts HTTP requests
  - Extracts Bearer token from `Authorization` header
  - Validates token using `JwtUtil`
  - Sets `SecurityContext` with authenticated user details
- **`security/SecurityConfig.kt`**: Spring Security configuration:
  - Configures BCrypt password encoder
  - Sets up stateless sessions (no cookies)
  - Defines endpoint security rules (public `/auth/**`, authenticated for others)
  - Integrates `JwtAuthFilter` into filter chain
- **`security/CustomUserDetailsService.kt`**: Implements `UserDetailsService` to load user from database for Spring Security.
- **`config/JwtProperties.kt`**: `@ConfigurationProperties` binding for JWT settings from `application.yml`:
  - `secret`: Signing key (default: "local-dev-secret")
  - `issuer`: Token issuer ("limitedcart-auth")
  - `expirationSeconds`: Token lifetime (3600 seconds)
- **`dto/AuthDtos.kt`**: Contains:
  - `LoginRequest`: email, password
  - `RegisterRequest`: email, password, roles
  - `AuthTokenResponse`: accessToken, tokenType, expiresIn
  - `UserView`: id, email, roles (sanitized user representation)
- **`resources/application.yml`**: Configuration for:
  - PostgreSQL connection to `auth_db` on localhost:5432
  - Flyway migration enabled
  - JPA with `ddl-auto: validate` (schema managed by Flyway)
  - JWT configuration under `app.security.jwt`
- **`resources/db/migration/V1__init_auth.sql`**: Flyway migration creating:
  - `users` table: id (UUID PK), email (UNIQUE), password_hash, created_at, updated_at
  - `user_roles` table: user_id, role_name (composite PK), FK to users with CASCADE delete

---

### **Module: `product-service`**
*Purpose: Product catalog management with CRUD operations and pagination support.*

#### Files:
- **`controller/ProductController.kt`**: REST endpoints:
  - `POST /products`: Create product (requires `X-Admin-Role: ADMIN` header)
  - `GET /products/{id}`: Retrieve product by UUID
  - `GET /products`: List products with pagination (default page size: 20)
- **`service/ProductService.kt`**: Business logic:
  - `createProduct()`: Validates and persists new product
  - `getProduct(id)`: Retrieves product or throws `ResourceNotFoundException`
  - `listProducts(pageable)`: Returns paginated list of active products
- **`repository/ProductRepository.kt`**: Spring Data JPA repository with custom query methods.
- **`entity/ProductEntity.kt`**: JPA entity extending `BaseEntity` with fields:
  - `name`: Product name (String, max 255 chars)
  - `description`: Detailed description (TEXT)
  - `price`: Decimal price (NUMERIC 19,2)
  - `maxQuantityPerSale`: Purchase limit per order
  - `active`: Boolean flag for soft deletion
- **`service/dto/ProductDtos.kt`**: Contains:
  - `CreateProductRequest`: name, description, price, maxQuantityPerSale
  - `ProductResponse`: Full product representation with timestamps
  - `ProductListResponse`: Paginated response with items, page metadata
- **`resources/application.yml`**: Configuration for:
  - PostgreSQL connection to `products_db`
  - Flyway enabled for schema migrations
  - JPA auditing and SQL formatting
- **`resources/db/migration/V1__init_products.sql`**: Creates `products` table with:
  - id (UUID PK), name, description, price, max_quantity_per_sale, active (default TRUE), created_at, updated_at

---

### **Module: `inventory-service`**
*Purpose: Stock management with optimistic locking, idempotent reservations, and compensation logic for distributed sagas.*

#### Files:
- **`entity/Stock.kt`**: JPA entity representing available inventory:
  - `productId`: String primary key (links to product catalog)
  - `availableQuantity`: Current stock count
  - `@Version version`: Long field enabling optimistic locking to prevent race conditions during concurrent updates
- **`entity/Reservation.kt`**: JPA entity for temporary stock holds:
  - `reservationId`: UUID primary key (auto-generated)
  - `orderId`: String with UNIQUE constraint (enables idempotent reservations)
  - `productId`: Foreign key to stock table
  - `quantity`: Reserved amount
  - `status`: Enum (RESERVED, CONFIRMED, CANCELLED)
  - `expiresAt`: Timestamp for TTL-based cleanup (5 minutes)
  - `createdAt`: Audit timestamp
- **`service/InventoryService.kt`**: Core transactional service with three key methods:
  - **`reserve(request)`**: Annotated with `@Transactional` and `@Retryable(OptimisticLockingFailureException, maxAttempts=3)`:
    1. Checks for existing reservation by orderId (idempotency)
    2. Fetches stock with pessimistic or optimistic locking
    3. Validates sufficient quantity
    4. Decrements `availableQuantity`
    5. Creates `Reservation` with 5-minute expiration
    6. Returns `ReserveResponse`
  - **`confirm(request)`**: Marks reservation as CONFIRMED (inventory already deducted)
  - **`release(request)`**: Compensation logic:
    1. Validates reservation ownership
    2. Restores stock by adding quantity back
    3. Marks reservation as CANCELLED
- **`controller/InventoryController.kt`**: REST endpoints:
  - `POST /inventory/reserve`: Initiates stock reservation
  - `POST /inventory/confirm`: Confirms successful order placement
  - `POST /inventory/release`: Cancels reservation (saga rollback)
- **`repository/StockRepository.kt`**: Spring Data JPA repository for `Stock` entity.
- **`repository/ReservationRepository.kt`**: Spring Data JPA repository with custom method:
  - `findByOrderId(orderId: String): Optional<Reservation>` for idempotency checks
- **`dto/InventoryDtos.kt`**: Contains:
  - `ReserveRequest`: orderId, productId, quantity
  - `ConfirmReservationRequest`: reservationId, orderId
  - `ReleaseReservationRequest`: reservationId, orderId
  - `ReserveResponse`: reservationId, orderId, productId, quantity, status, expiresAt
- **`config/InventoryConfig.kt`**: Enables Spring Retry with `@EnableRetry` annotation.
- **`resources/application.yml`**: Configuration for:
  - PostgreSQL connection to `inventory_db`
  - Flyway migrations enabled
  - JPA with optimistic locking support
- **`resources/db/migration/V1__init_inventory.sql`**: Creates:
  - `stock` table: product_id (VARCHAR PK), available_quantity, version (BIGINT for optimistic locking)
  - `reservations` table: reservation_id (UUID PK), order_id (VARCHAR UNIQUE), product_id (FK), quantity, status, expires_at, created_at
  - Index on `product_id` for reservation lookups

---

### **Module: `order-service`** *(Placeholder - Phase 3)*
*Purpose: Will orchestrate order workflows, coordinate with inventory and payment services via Temporal.*

**Status:** Directory created, no implementation yet.

---

### **Module: `payment-service`** *(Placeholder - Phase 3)*
*Purpose: Will handle payment processing, refunds, and integration with payment gateways.*

**Status:** Directory created, no implementation yet.

---

### **Module: `temporal-worker`** *(Placeholder - Phase 3)*
*Purpose: Will host Temporal workflow workers for saga orchestration.*

**Status:** Directory created, no implementation yet.

---

## **3. KEY TECHNICAL DECISIONS**

### **Why Optimistic Locking?**
The `@Version` annotation on `Stock.version` prevents **lost updates** during high-concurrency scenarios like flash sales. When two transactions try to update the same stock row simultaneously, JPA compares the version number with the database. If they don't match, an `OptimisticLockingFailureException` is thrown, and the `@Retryable` annotation triggers automatic retry with exponential backoff (100ms delay, 2.0 multiplier, max 3 attempts). This approach offers better performance than pessimistic locks while ensuring data consistency.

### **Idempotency Strategy**
The `UNIQUE` constraint on `Reservation.orderId` ensures that duplicate reservation requests (e.g., from retries or network issues) don't double-deduct inventory. The `reserve()` method first checks `findByOrderId()` and returns the existing reservation if found, making the operation idempotent. This is critical for distributed systems where network failures can cause duplicate requests.

### **Database-per-Service Pattern**
Each microservice owns its own PostgreSQL database (`auth_db`, `products_db`, `inventory_db`). This enforces **bounded contexts** and prevents tight coupling between services. Cross-service data access must happen via REST APIs or events, ensuring scalability and independent deployments. The `init-multiple-databases.sh` script automates database creation during Docker Compose startup.

### **Common Module Pattern**
The `common` module reduces code duplication by providing:
- **BaseEntity**: Eliminates boilerplate audit fields across all entities
- **GlobalExceptionHandler**: Centralizes error mapping logic
- **Custom Exceptions**: Enforces consistent error semantics (e.g., `OutOfStockException` always returns 409)
- **ErrorResponse**: Standardizes API error format with correlation IDs for tracing

### **Flyway for Schema Management**
Using Flyway (instead of `ddl-auto: create` or `update`) ensures:
- **Version-controlled migrations** trackable in Git
- **Reproducible deployments** across environments
- **Safe schema evolution** with rollback support
- **Audit trail** of database changes via `flyway_schema_history` table

### **Spring Retry for Resilience**
The `@Retryable` annotation on `InventoryService.reserve()` handles transient failures from optimistic locking. With exponential backoff, the system can gracefully handle temporary contention without failing the entire request. This improves user experience during traffic spikes.

### **JWT Stateless Authentication**
By using JWT tokens instead of session cookies:
- **Horizontal scaling** becomes trivial (no session replication needed)
- **Microservices can validate tokens independently** without calling auth-service
- **Mobile/SPA clients** can easily authenticate via Bearer tokens
The secret key is externalized via `application.yml` and supports environment-specific overrides using `${JWT_SECRET}`.

### **Correlation ID for Distributed Tracing**
The `SharedUtils.getOrCreateCorrelationId()` generates/extracts correlation IDs from request headers. These IDs appear in all error responses, enabling end-to-end request tracing across multiple microservices (future integration with Zipkin/Jaeger).

---

## **4. INFRASTRUCTURE STATUS**

### **Docker Containers (via `docker-compose.yml`)**

| Service                  | Image                           | Purpose                                                                         | Ports       | Health Check            |
| ------------------------ | ------------------------------- | ------------------------------------------------------------------------------- | ----------- | ----------------------- |
| **postgres**             | `postgres:15-alpine`            | Multi-tenant PostgreSQL hosting auth_db, products_db, inventory_db, temporal_db | `5432:5432` | `pg_isready` every 10s  |
| **redis**                | `redis:7-alpine`                | Caching layer for session management, rate limiting                             | `6379:6379` | AOF persistence enabled |
| **kafka**                | `bitnami/kafka:3.6`             | Event streaming (KRaft mode - no Zookeeper)                                     | `9092:9092` | Kafka topics list check |
| **temporal**             | `temporalio/auto-setup:1.22.4`  | Workflow orchestration engine                                                   | `7233:7233` | Depends on postgres     |
| **temporal-admin-tools** | `temporalio/admin-tools:1.22.4` | CLI for Temporal management                                                     | -           | -                       |
| **temporal-ui**          | `temporalio/ui:2.19.3`          | Web UI for workflow monitoring                                                  | `8088:8080` | -                       |
| **zipkin**               | `openzipkin/zipkin:3.4`         | Distributed tracing collector                                                   | `9411:9411` | -                       |

### **Connection Details**
- **PostgreSQL**: `jdbc:postgresql://localhost:5432/{database_name}` (username: `postgres`, password: `postgres`)
- **Kafka**: `localhost:9092` (PLAINTEXT protocol)
- **Redis**: `localhost:6379`
- **Temporal gRPC**: `localhost:7233`
- **Temporal UI**: `http://localhost:8088`
- **Zipkin**: `http://localhost:9411`

### **Network Configuration**
All containers are on the `limitedcart-network` bridge network with service discovery aliases:
- `postgres-db`, `redis-cache`, `kafka-broker`, `temporal-server`, `temporal-ui`, `tracing`

### **Persistent Volumes**
- `postgres_data`: Database files for all PostgreSQL databases
- `kafka_data`: Kafka logs and metadata (KRaft controller state)
- `redis_data`: Redis AOF (Append-Only File) for durability

---

## **5. CURRENT API CAPABILITIES**

### **Authentication Service** (`auth-service`)

| Method | Endpoint         | Request Body                                                                    | Response                                                                 | Purpose                                         |
| ------ | ---------------- | ------------------------------------------------------------------------------- | ------------------------------------------------------------------------ | ----------------------------------------------- |
| POST   | `/auth/register` | `{"email": "user@example.com", "password": "securePass123", "roles": ["USER"]}` | `{"id": "uuid", "email": "...", "roles": [...]}`                         | Create user account with BCrypt hashed password |
| POST   | `/auth/login`    | `{"email": "user@example.com", "password": "securePass123"}`                    | `{"accessToken": "eyJhbG...", "tokenType": "Bearer", "expiresIn": 3600}` | Authenticate and receive JWT token              |

**Security:**
- Passwords hashed with BCrypt strength 10
- JWT tokens signed with HMAC-SHA256
- Token expiration: 1 hour (configurable)
- Stateless authentication (no server-side sessions)

---

### **Product Service** (`product-service`)

| Method | Endpoint                   | Headers               | Request Body                                                                                   | Response                                                        | Purpose                                  |
| ------ | -------------------------- | --------------------- | ---------------------------------------------------------------------------------------------- | --------------------------------------------------------------- | ---------------------------------------- |
| POST   | `/products`                | `X-Admin-Role: ADMIN` | `{"name": "Limited Sneakers", "description": "...", "price": 199.99, "maxQuantityPerSale": 2}` | `{"id": "uuid", "name": "...", "price": 199.99, ...}`           | Create new product (admin only)          |
| GET    | `/products/{id}`           | -                     | -                                                                                              | `{"id": "uuid", "name": "...", ...}`                            | Retrieve product details by UUID         |
| GET    | `/products?page=0&size=20` | -                     | -                                                                                              | `{"items": [...], "page": 0, "size": 20, "totalElements": 150}` | List all active products with pagination |

**Features:**
- Pagination via Spring Data (`Pageable` parameter)
- Admin authorization via header validation (temporary, will integrate with JWT in Phase 3)
- Soft deletion via `active` flag
- Price stored as `NUMERIC(19,2)` for precision

---

### **Inventory Service** (`inventory-service`)

| Method | Endpoint             | Request Body                                                       | Response                                                                                                | Purpose                                                  |
| ------ | -------------------- | ------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------- | -------------------------------------------------------- |
| POST   | `/inventory/reserve` | `{"orderId": "ORD-12345", "productId": "PROD-001", "quantity": 2}` | `{"reservationId": "uuid", "orderId": "...", "status": "RESERVED", "expiresAt": "2025-12-01T18:25:00"}` | Reserve stock atomically with optimistic locking         |
| POST   | `/inventory/confirm` | `{"reservationId": "uuid", "orderId": "ORD-12345"}`                | `{"reservationId": "...", "status": "CONFIRMED", ...}`                                                  | Mark reservation as confirmed (order successful)         |
| POST   | `/inventory/release` | `{"reservationId": "uuid", "orderId": "ORD-12345"}`                | `{"reservationId": "...", "status": "CANCELLED", ...}`                                                  | Cancel reservation and restore stock (saga compensation) |

**Guarantees:**
- **Idempotency**: Duplicate `reserve()` calls with same `orderId` return existing reservation
- **Consistency**: Optimistic locking prevents overselling during concurrent requests
- **Retry Logic**: Automatic retry on version conflict (max 3 attempts)
- **TTL**: Reservations expire after 5 minutes (manual cleanup required in Phase 3)

**Error Scenarios:**
- `409 Conflict`: `OutOfStockException` when quantity exceeds `availableQuantity`
- `404 Not Found`: `ResourceNotFoundException` when product/reservation doesn't exist
- `400 Bad Request`: Validation errors (negative quantity, missing fields)

---

## **6. NEXT STEPS (Phase 3 Preview)**

**Phase 3 Goal:** Integrate Temporal workflows to orchestrate distributed sagas across Order, Inventory, and Payment services, ensuring eventual consistency with automatic rollback on failures.

### **Key Deliverables:**
1. **Order Service Implementation:**
   - Create `OrderEntity` with states (PENDING, CONFIRMED, CANCELLED)
   - REST endpoint: `POST /orders` to initiate order placement
   - Trigger Temporal workflow on order creation

2. **Temporal Saga Workflow:**
   - **Activities**: `ReserveInventoryActivity`, `ProcessPaymentActivity`, `ConfirmOrderActivity`
   - **Compensation Logic**: Automatic `ReleaseInventoryActivity` and `RefundPaymentActivity` on failure
   - **Error Handling**: Retry policies, timeouts, and dead-letter queues

3. **Payment Service Stub:**
   - Mock payment gateway integration
   - Endpoints: `/payments/charge`, `/payments/refund`
   - Random failure simulation for saga testing

4. **Event-Driven Architecture:**
   - Publish `OrderCreated`, `InventoryReserved`, `PaymentProcessed` events to Kafka
   - Enable async notification services (email, SMS)

5. **Observability Enhancements:**
   - Integrate Zipkin for distributed tracing
   - Add correlation ID propagation across service calls
   - Temporal UI monitoring for workflow visualization

6. **Reservation TTL Cleanup:**
   - Implement scheduled job to release expired reservations
   - Auto-restore stock for RESERVED items past `expiresAt`

---

## **APPENDIX: Build & Run Instructions**

### **Prerequisites**
- JDK 21
- Docker & Docker Compose
- Gradle 8.5+

### **Starting Infrastructure**
```bash
# Start all infrastructure services
docker-compose up -d

# Verify containers are healthy
docker ps

# Check PostgreSQL databases
docker exec -it limitedcart-postgres psql -U postgres -c "\l"
```

### **Building Services**
```bash
# Build all modules
./gradlew clean build

# Build specific service
./gradlew :auth-service:build

# Run tests
./gradlew test
```

### **Running Services Locally**
```bash
# Run auth-service
./gradlew :auth-service:bootRun

# Run product-service (default port: 8081)
./gradlew :product-service:bootRun

# Run inventory-service (default port: 8082)
./gradlew :inventory-service:bootRun
```

### **Database Migrations**
Flyway runs automatically on application startup. To manually apply migrations:
```bash
./gradlew :auth-service:flywayMigrate
./gradlew :product-service:flywayMigrate
./gradlew :inventory-service:flywayMigrate
```

### **Stopping Infrastructure**
```bash
# Stop and remove containers
docker-compose down

# Stop and remove volumes (deletes all data)
docker-compose down -v
```

---

**End of Implementation Summary**
