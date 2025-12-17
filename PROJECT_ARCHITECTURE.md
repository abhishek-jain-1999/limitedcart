# PROJECT_ARCHITECTURE.md

## 1. High-Level Module Overview

| Module              | Service URL / Notes                       | Purpose                                                                |
| ------------------- | ----------------------------------------- | ---------------------------------------------------------------------- |
| `common`            | N/A                                       | Shared contracts (entities, events, Kafka config, Temporal contracts). |
| `auth-service`      | Internal: `http://auth-service:8080`      | JWT-based authentication and user management.                          |
| `product-service`   | Internal: `http://product-service:8080`   | Product catalog CRUD, Kafka product events, Elasticsearch indexing.    |
| `inventory-service` | Internal: `http://inventory-service:8080` | Inventory reservations with optimistic locking and retry semantics.    |
| `order-service`     | Internal: `http://order-service:8080`     | Order persistence + Temporal saga initiator + Kafka order events.      |
| `payment-service`   | Internal: `http://payment-service:8080`   | Mock payment gateway for charge/refund flows.                          |
| `temporal-worker`   | Internal: `http://temporal-worker:8080`   | Temporal workflow + activities executing the distributed saga steps.   |
| `nginx`             | External: `GATEWAY_PORT` → default `8080` | Nginx gateway routing all external traffic to internal services.       |

*(All services run on port 8080 inside Docker containers. Service URLs in `.env` use Docker internal networking. External access is via nginx on `GATEWAY_PORT`.)*

---

## 2. Complete Directory Tree
```
limitedcart/
├── pom.xml
├── .env
├── .env.example
├── .gitignore
├── docker-compose.yml
├── docker/
│ └── init/postgres/init-multiple-databases.sh
├── common/
│ ├── pom.xml
│ └── src/main/kotlin/com/abhishek/limitedcart/common/
│ ├── config/JpaConfig.kt
│ ├── entity/BaseEntity.kt
│ ├── error/ErrorResponse.kt
│ ├── event/Event.kt
│ ├── events/ProductEvents.kt
│ ├── events/OrderEvents.kt
│ ├── exception/GlobalExceptionHandler.kt
│ ├── exception/OutOfStockException.kt
│ ├── exception/ResourceNotFoundException.kt
│ ├── kafka/KafkaConfig.kt
│ ├── util/SharedUtils.kt
│ └── workflow/OrderSagaContracts.kt
├── auth-service/
│ ├── pom.xml
│ ├── src/main/kotlin/com/abhishek/limitedcart/auth/
│ │ ├── AuthServiceApplication.kt
│ │ ├── config/JwtProperties.kt
│ │ ├── config/SecurityConfig.kt
│ │ ├── controller/AuthController.kt
│ │ ├── dto/AuthDtos.kt
│ │ ├── entity/User.kt
│ │ ├── repository/UserRepository.kt
│ │ ├── security/CustomUserDetailsService.kt
│ │ ├── security/JwtAuthFilter.kt
│ │ ├── security/JwtUtil.kt
│ │ └── service/AuthService.kt
│ ├── src/main/resources/application.yml
│ └── src/main/resources/db/migration/V1__init_auth.sql
├── product-service/
│ ├── pom.xml
│ ├── src/main/kotlin/com/abhishek/limitedcart/product/
│ │ ├── ProductServiceApplication.kt
│ │ ├── config/ElasticsearchConfig.kt
│ │ ├── controller/ProductController.kt
│ │ ├── entity/ProductEntity.kt
│ │ ├── messaging/ProductEventPublisher.kt
│ │ ├── repository/ProductRepository.kt
│ │ ├── search/ProductDocument.kt
│ │ ├── search/ProductSearchRepository.kt
│ │ ├── search/ProductSearchService.kt
│ │ ├── service/ProductService.kt
│ │ └── service/dto/ProductDtos.kt
│ ├── src/main/resources/application.yml
│ └── src/main/resources/db/migration/V1__init_products.sql
├── inventory-service/
│ ├── pom.xml
│ ├── src/main/kotlin/com/abhishek/limitedcart/inventory/
│ │ ├── InventoryServiceApplication.kt
│ │ ├── config/InventoryConfig.kt
│ │ ├── controller/InventoryController.kt
│ │ ├── dto/InventoryDtos.kt
│ │ ├── entity/Reservation.kt
│ │ ├── entity/Stock.kt
│ │ ├── repository/ReservationRepository.kt
│ │ ├── repository/StockRepository.kt
│ │ └── service/InventoryService.kt
│ ├── src/main/resources/application.yml
│ └── src/main/resources/db/migration/V1__init_inventory.sql
├── order-service/
│ ├── pom.xml
│ ├── src/main/kotlin/com/abhishek/limitedcart/order/
│ │ ├── OrderServiceApplication.kt
│ │ ├── config/TemporalConfig.kt
│ │ ├── controller/OrderController.kt
│ │ ├── dto/OrderDtos.kt
│ │ ├── entity/OrderEntity.kt
│ │ ├── messaging/OrderEventPublisher.kt
│ │ ├── repository/OrderRepository.kt
│ │ └── service/OrderService.kt
│ ├── src/main/resources/application.yml
│ └── src/main/resources/db/migration/V1__init_orders.sql
├── payment-service/
│ ├── pom.xml
│ ├── src/main/kotlin/com/abhishek/limitedcart/payment/
│ │ ├── PaymentServiceApplication.kt
│ │ └── controller/PaymentController.kt
│ └── src/main/resources/application.yml
├── temporal-worker/
│ ├── pom.xml
│ ├── src/main/kotlin/com/abhishek/limitedcart/worker/
│ │ ├── TemporalWorkerApplication.kt
│ │ ├── activities/InventoryActivities.kt
│ │ ├── activities/OrderActivities.kt
│ │ ├── activities/PaymentActivities.kt
│ │ ├── activities/impl/InventoryActivitiesImpl.kt
│ │ ├── activities/impl/OrderActivitiesImpl.kt
│ │ ├── activities/impl/PaymentActivitiesImpl.kt
│ │ ├── config/TemporalWorkerConfig.kt
│ │ └── workflow/OrderWorkflowImpl.kt
│ └── src/main/resources/application.yml
├── api-gateway/
│ ├── pom.xml
│ ├── src/main/kotlin/com/abhishek/limitedcart/gateway/
│ │ ├── GatewayApplication.kt
│ │ └── config/{CorrelationFilter.kt, GatewayConfig.kt}
│ └── src/main/resources/application.yml
└── README / docs (IMPLEMENTATION_SUMMARY.md, PROJECT_ARCHITECTURE.md)
```
---

## 3. File-by-File Descriptions

### Module: `common`
**Purpose:** Shared Kotlin library with base entities, exception handling, Kafka/Temporal contracts.

- `pom.xml`: Declares shared dependencies (Spring Boot starter, validation, Kafka, Temporal SDK).
- `JpaConfig.kt`: Enables Spring Data JPA auditing globally.
- `BaseEntity.kt`: Abstract entity with UUID id + auditing timestamps.
- `ErrorResponse.kt`: Standardized error payload for APIs.
- `Event.kt`: Generic event wrapper used when publishing Kafka messages.
- `ProductEvents.kt`, `OrderEvents.kt`: Strongly typed payloads for Kafka topics.
- `GlobalExceptionHandler.kt`: Centralized REST exception handling (validation, out of stock, resource not found).
- `OutOfStockException.kt`, `ResourceNotFoundException.kt`: Shared runtime errors.
- `KafkaConfig.kt`: Configures JSON producer/consumer factories and listener container.
- `SharedUtils.kt`: Correlation ID helpers.
- `OrderSagaContracts.kt`: Shared Temporal workflow interface and saga request model.

### Module: `auth-service`
**Purpose:** Handles user registration/login, JWT issuance, and security filters.

- `AuthServiceApplication.kt`: Spring Boot entry point.
- `config/JwtProperties.kt`: Binds JWT settings from configuration.
- `config/SecurityConfig.kt`: Stateless Spring Security setup plus JWT filter.
- `controller/AuthController.kt`: REST endpoints `/auth/register` and `/auth/login`.
- `dto/AuthDtos.kt`: Request/response DTOs for authentication.
- `entity/User.kt`: JPA entity extending `BaseEntity` with email/password hash/roles.
- `repository/UserRepository.kt`: Spring Data repository for users.
- `security/CustomUserDetailsService.kt`: Loads user details for authentication.
- `security/JwtAuthFilter.kt`: Pulls tokens from `Authorization` header.
- `security/JwtUtil.kt`: Generates and validates JWTs.
- `service/AuthService.kt`: Business logic for register/login flows.
- `application.yml`: Datasource, Flyway, Kafka producer, JWT secret placeholders.
- `db/migration/V1__init_auth.sql`: Creates `users` and `user_roles` tables.

### Module: `product-service`
**Purpose:** Manages products, emits Kafka events, and indexes to Elasticsearch.

- `ProductServiceApplication.kt`: Spring Boot entry point.
- `config/ElasticsearchConfig.kt`: Configures Spring Data Elasticsearch client.
- `controller/ProductController.kt`: CRUD endpoints, admin guard, and `/products/search`.
- `entity/ProductEntity.kt`: Product JPA model extending `BaseEntity`.
- `messaging/ProductEventPublisher.kt`: Publishes `ProductCreated/UpdatedEvent`.
- `repository/ProductRepository.kt`: Spring Data repository.
- `search/ProductDocument.kt`: Elasticsearch document representation.
- `search/ProductSearchRepository.kt`: ES repository with name query.
- `search/ProductSearchService.kt`: Kafka listener + indexing/search logic.
- `service/ProductService.kt`: Business logic for create/update/list.
- `service/dto/ProductDtos.kt`: Create/update request DTOs + responses.
- `application.yml`: Datasource, Kafka, Elasticsearch, Flyway configuration.
- `db/migration/V1__init_products.sql`: Creates `products` table with audit columns.

### Module: `inventory-service`
**Purpose:** Guarantees stock reservations with optimistic locking and compensation hooks.

- `InventoryServiceApplication.kt`: Entry point.
- `config/InventoryConfig.kt`: Enables Spring Retry for optimistic locking.
- `controller/InventoryController.kt`: `/inventory/reserve`, `/inventory/confirm`, `/inventory/release`.
- `dto/InventoryDtos.kt`: Request/response payloads.
- `entity/Stock.kt`: JPA entity with `@Version` for optimistic locking.
- `entity/Reservation.kt`: Tracks reservation state and uniqueness on `orderId`.
- `repository/StockRepository.kt`, `ReservationRepository.kt`: Data access layers.
- `service/InventoryService.kt`: Reservation logic with retries, idempotency.
- `application.yml`: Datasource, Kafka bootstrap, Flyway settings.
- `db/migration/V1__init_inventory.sql`: Creates `stock` and `reservations` tables.

### Module: `order-service`
**Purpose:** Persists orders, starts Temporal saga workflows, and emits order lifecycle events.

- `OrderServiceApplication.kt`: Entry point.
- `config/TemporalConfig.kt`: Creates Temporal `WorkflowServiceStubs` using env address.
- `controller/OrderController.kt`: `POST /orders`, patch endpoints for confirm/fail (used by worker).
- `dto/OrderDtos.kt`: Create request and confirmations.
- `entity/OrderEntity.kt`: Order JPA model with status, payment info.
- `messaging/OrderEventPublisher.kt`: Sends order created/confirmed/failed events.
- `repository/OrderRepository.kt`: Data access.
- `service/OrderService.kt`: Creates orders, triggers workflows, handles updates.
- `application.yml`: Datasource, Kafka, Temporal address, Flyway.
- `db/migration/V1__init_orders.sql`: Creates `orders` table.

### Module: `payment-service`
**Purpose:** Simulated payment gateway with charge/refund endpoints.

- `PaymentServiceApplication.kt`: Entry point.
- `controller/PaymentController.kt`: `/payments/charge` and `/payments/refund` with latency + 20% failure simulation.
- `application.yml`: Port + actuator exposure (using `.env`).

### Module: `temporal-worker`
**Purpose:** Hosts Temporal workflow implementation and activities for the saga.

- `TemporalWorkerApplication.kt`: Boots worker, registers workflows/activities on `ORDER_SAGA_QUEUE`.
- `activities/InventoryActivities.kt`, `PaymentActivities.kt`, `OrderActivities.kt`: Activity contracts.
- `activities/impl/...`: RestClient-backed implementations calling inventory/payment/order services.
- `config/TemporalWorkerConfig.kt`: Configures Temporal clients and service RestClients using env URLs.
- `workflow/OrderWorkflowImpl.kt`: Saga definition with compensation logic.
- `application.yml`: Worker port, task queue, Temporal target, downstream service URLs.

### Module: `api-gateway`
**Purpose:** Spring Cloud Gateway front door for all services.

- `GatewayApplication.kt`: Entry point.
- `config/CorrelationFilter.kt`: Adds/propagates `X-Correlation-ID`.
- `config/GatewayConfig.kt`: Route definitions, injecting backend URLs from env.
- `application.yml`: Gateway port, exposed actuator endpoints, backend service URLs.

---

## 4. Maven Configuration Summary

- **Parent `pom.xml`**: Packaging `pom`. Declares modules (`common`, `auth-service`, `product-service`, `inventory-service`, `order-service`, `payment-service`, `temporal-worker`, `api-gateway`). Manages shared versions via properties: Spring Boot `3.2.5`, Kotlin `1.9.24`, Spring Cloud `2023.0.0`, Temporal SDK `1.22.2`. Includes `<dependencyManagement>` (Spring Cloud BOM + `common` version) and `<pluginManagement>` for Kotlin + Spring Boot plugins, configuring the Kotlin compiler flags and all-open annotations.
- **Module POMs**: Each module inherits the parent, adds module-specific dependencies, and configures the Spring Boot Maven plugin (`mainClass` set via `${start-class}` property). Library module `common` builds a plain JAR; service modules produce executable jars.

---

## 5. Infrastructure Files

| File                                              | Purpose                                                                                                                                                                                    |
| ------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `docker-compose.yml`                              | Spins up Postgres 15 (multi-DB init script), Redis, Kafka (Bitnami KRaft), Temporal server/UI, Zipkin—providing local infrastructure for all services.                                     |
| `docker/init/postgres/init-multiple-databases.sh` | Creates `auth_db`, `products_db`, `inventory_db`, `orders_db`, `temporal_db` inside the Postgres container.                                                                                |
| `.env` / `.env.example`                           | Centralized configuration for database credentials, Kafka bootstrap, Elasticsearch, Redis, Temporal, JWT secret, service ports & URLs. Example file contains safe defaults for onboarding. |
| `.gitignore`                                      | Excludes build artifacts (`target/`), IDE files, logs, OS trash; prevents accidental commits of generated files.                                                                           |
| `.mvn/wrapper/*`, `mvnw`, `mvnw.cmd`              | Maven wrapper allowing reproducible builds.                                                                                                                                                |

---

This document reflects the complete Kotlin + Maven codebase for the "limitedcart" microservices platform, detailing module responsibilities, file layout, and infrastructure configuration.
