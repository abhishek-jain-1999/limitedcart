# 📦 Product Service (`product-service`)

**Role**: Catalog Authority, Search Engine, and Price Master.

The **Product Service** is the "Source of Truth" for all item details. It serves two critical functions:
1.  **Catalog Management**: Admin CRUD operations for products (Name, Description, Price).
2.  **Search & Discovery**: High-performance search capability (backed by Elasticsearch in production, currently simulated/hybrid).

Crucially, this service acts as the **Price Authority**. If a product's price changes here, an event is broadcast to ensuring all other services (like Order) eventually align with the new price.

---

## 1. API Reference

### 🔍 Public Catalog (Read-Only)

| Method | Endpoint           | Query Params        | Description                                          |
| :----- | :----------------- | :------------------ | :--------------------------------------------------- |
| `GET`  | `/products`        | `page=0`, `size=20` | standard paginated list of active products.          |
| `GET`  | `/products/{id}`   | -                   | Get full details of a specific product.              |
| `GET`  | `/products/search` | `q=laptop`          | **Search Endpoint**. Fuzzy matches name/description. |

### 🛠️ Inventory Management (Write)
*Access Restriction: `ROLE_ADMIN` or `ROLE_WAREHOUSE`.*

| Method   | Endpoint         | Request Body           | Description                                      |
| :------- | :--------------- | :--------------------- | :----------------------------------------------- |
| `POST`   | `/products`      | `{ name, price, ... }` | Create a new product listing.                    |
| `PUT`    | `/products/{id}` | `{ name, price, ... }` | Update details. Triggers **Price Update Event**. |
| `DELETE` | `/products/{id}` | -                      | **Soft Delete**. Sets `active = false`.          |

---

## 2. Architecture: Search & Events

### The Dual-Write Problem
When a product is created or updated, we need to update two places:
1.  **Postgres**: The canonical transactional record.
2.  **Search Index** (Elasticsearch/Lucene): For fast free-text search.

**Solution**: We use an Event-Driven consistency model.
1.  Admin updates Product in Postgres.
2.  Service publishes `ProductUpdatedEvent` to Kafka.
3.  **ProductSearchService** (internal listener) consumes this event.
4.  Listener updates the Search Index (implemented via `ProductSearchRepository`).

### Kafka Events
**Topic**: `product.events`

| Event Type        | Payload Data          | Purpose                                                     |
| :---------------- | :-------------------- | :---------------------------------------------------------- |
| `PRODUCT_CREATED` | `id`, `name`, `price` | Triggers indexing.                                          |
| `PRODUCT_UPDATED` | `id`, `price`         | Triggers re-indexing & cache invalidation in Order Service. |

---

## 3. Data Model

**Database**: `products_db`

**Table: `products`**
| Column         | Type      | Notes                       |
| :------------- | :-------- | :-------------------------- |
| `id`           | UUID      | PK                          |
| `name`         | VARCHAR   | Indexed for basic search    |
| `price`        | DECIMAL   | Precision 19,2              |
| `active`       | BOOLEAN   | Logic flag for soft deletes |
| `audit_fields` | TIMESTAMP | Created/Updated At          |

---

## 4. Configuration

| Variable                     | Description   | Default                         |
| :--------------------------- | :------------ | :------------------------------ |
| `POSTGRES_URL`               | DB Connection | `jdbc:postgresql://postgres...` |
| `KAFKA_BOOTSTRAP`            | Kafka Broker  | `localhost:9092`                |
| `KAFKA_TOPIC_PRODUCT_EVENTS` | Event Topic   | `product.events`                |

---

## 5. Local Development

**Run Service**:
```bash
./mvnw -pl product-service spring-boot:run
```

**Test Search**:
```bash
# Create Product
curl -X POST ... /products -d '{"name": "Gaming Laptop"}'

# Search
curl "http://localhost:8080/products/search?q=gaming"
```
