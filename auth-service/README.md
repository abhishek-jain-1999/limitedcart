# 🔐 Auth Service (`auth-service`)

**Role**: Identity Provider, JWT Issuer, and RBAC Enforcer.

The **Auth Service** is the foundation of security in LimitedCart. It acts as the centralized authority for user identity, managing registration, secure login, and the issuance of signed JWTs (JSON Web Tokens). It is designed to be purely **stateless**, meaning no sessions are stored on the server—validation happens via cryptographic signature checks in downstream services.

---

## 1. API Reference

All successful responses follow the standard JSON structure. Errors return a `4xx` or `5xx` with a message.

### 👤 User Authentication

| Method | Endpoint         | Auth Required | Request Body                            | Description                                           |
| :----- | :--------------- | :------------ | :-------------------------------------- | :---------------------------------------------------- |
| `POST` | `/auth/register` | ❌ No          | `{ "email": "...", "password": "..." }` | Creates a new user. Passwords are hashed with BCrypt. |
| `POST` | `/auth/login`    | ❌ No          | `{ "email": "...", "password": "..." }` | Validates credentials and returns a Bearer Token.     |

**Login Response Example:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "a1b2c3d4-e5f6-...",
    "email": "user@example.com",
    "roles": ["ROLE_USER"]
  }
}
```

### 👮 Admin Management
*Access Restriction: Requires `ROLE_ADMIN` in the JWT `roles` claim.*

| Method  | Endpoint                      | Description                                                                       |
| :------ | :---------------------------- | :-------------------------------------------------------------------------------- |
| `GET`   | `/admin/users`                | Returns a list of all registered users, their IDs, and current roles.             |
| `PATCH` | `/admin/users/{userId}/roles` | **Role Promotion**. Updates a user's role list (e.g., granting `ROLE_WAREHOUSE`). |
| `GET`   | `/admin/metrics`              | Returns simplified platform stats (Total Users, Admin Count, etc.).               |

---

## 2. Security Architecture

### JWT Design
We use **HMAC-SHA256 (HS256)** for signing tokens. The `common` module provides the `JwtUtil` class shared across services for validation.

**Token Payload (Claims):**
- `sub`: User ID (UUID)
- `email`: User Email
- `roles`: List of strings (e.g., `["ROLE_USER", "ROLE_ADMIN"]`)
- `iat`: Issued At timestamp
- `exp`: Expiration timestamp

### Role Logic (`UserRole.kt`)
- **`ROLE_USER`**: Default role. Can browse products and place orders.
- **`ROLE_WAREHOUSE`**: Can view inventory dashboards and execute restocks.
- **`ROLE_ADMIN`**: Can manage users, edit products, and view all orders.

---

## 3. Data Model

**Database**: `auth_db` (Postgres)

**Table: `users`**
| Column          | Type        | Constraints             | Description                         |
| :-------------- | :---------- | :---------------------- | :---------------------------------- |
| `id`            | UUID        | PK                      | Generated automatically.            |
| `email`         | VARCHAR     | UNIQUE, NOT NULL        | User's login handle.                |
| `password_hash` | VARCHAR     | NOT NULL                | BCrypt encoded string ($2a$10$...). |
| `roles`         | JSONB/ARRAY | DEFAULT '["ROLE_USER"]' | Stored as a list of strings.        |
| `created_at`    | TIMESTAMP   | DEFAULT NOW()           | Audit timestamp.                    |

---

## 4. Configuration

The service is configured via environment variables (in `.env` or K8s Secrets).

| Variable                | Description                                   | Default (Dev)                             |
| :---------------------- | :-------------------------------------------- | :---------------------------------------- |
| `POSTGRES_USER`         | Database Username                             | `postgres`                                |
| `POSTGRES_PASSWORD`     | Database Password                             | `postgres`                                |
| `POSTGRES_URL`          | JDBC Connection String                        | `jdbc:postgresql://postgres:5432/auth_db` |
| `JWT_SECRET`            | **Critical**. Base64 encoded key for signing. | `default-secret...`                       |
| `JWT_EXPIRATION`        | Token lifetime in milliseconds.               | `3600000` (1 Hour)                        |
| `ADMIN_BOOTSTRAP_EMAIL` | Email for the initial admin account.          | `admin@test.com`                          |

---

## 5. Local Development

**Prerequisites**: Docker (for Postgres).

1.  **Start Database**:
    ```bash
    docker-compose up -d postgres
    ```

2.  **Run Service**:
    ```bash
    ./mvnw -pl auth-service spring-boot:run
    ```

3.  **Verify**:
    Open Swagger UI at [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html).
