# 📊 Inventory Service (`inventory-service`)

**Role**: Stock Keeper & Concurrency Manager.

The **Inventory Service** maintains the physical reality of the warehouse. Its most critical job is to prevent **Overselling** (selling items you don't have) without destroying database performance. It achieves this using **Optimistic Locking** rather than pessimistic row locks.

---

## 1. API Reference

### ⚙️ Saga Operations (Internal)
*These endpoints are used by the Temporal Worker to execute the "Reserve -> Confirm" protocol.*

| Method | Endpoint             | Behavior                                                                       |
| :----- | :------------------- | :----------------------------------------------------------------------------- |
| `POST` | `/inventory/reserve` | **Transactional**. Checks stock, decrements quantity, creates reservation row. |
| `POST` | `/inventory/confirm` | Finalizes reservation (status change).                                         |
| `POST` | `/inventory/release` | **Compensation**. Increments stock back, marks reservation cancelled.          |

### 📦 Warehouse Operations
*Requires `ROLE_ADMIN` or `ROLE_WAREHOUSE`.*

| Method | Endpoint             | Description                          |
| :----- | :------------------- | :----------------------------------- |
| `POST` | `/inventory/restock` | Add stock (e.g., received shipment). |
| `GET`  | `/inventory/stock`   | List current stock levels.           |
| `GET`  | `/inventory/summary` | Dashboard stats (Low stock alerts).  |

---

## 2. Deep Dive: Optimistic Locking

We use the `@Version` annotation on the `Stock` entity. High concurrency is handled at the database level using version checks.

**The "Race Condition" Scenario**:
Imagine 2 users buy the last item (Qty: 1) at the exact same millisecond.

1.  **User A** reads DB: `Qty=1, Version=5`.
2.  **User B** reads DB: `Qty=1, Version=5`.
3.  **User A** updates:
    ```sql
    UPDATE stock SET quantity=0, version=6 
    WHERE id=... AND version=5
    ```
    **Result**: 1 row updated. (Success)
4.  **User B** updates:
    ```sql
    UPDATE stock SET quantity=0, version=6 
    WHERE id=... AND version=5
    ```
    **Result**: 0 rows updated. (Fail - Version is now 6, not 5).
5.  **Handling**: Application throws `OptimisticLockingFailureException`.
6.  **Outcome**: User B's request fails cleanly. No negative stock.

---

## 3. Data Model

**Database**: `inventory_db`

**Table: `stock`**
| Column       | Type   | Notes                                    |
| :----------- | :----- | :--------------------------------------- |
| `product_id` | UUID   | PK                                       |
| `quantity`   | INT    | Check constraints ensure >= 0            |
| `version`    | BIGINT | **The Lock**. Increments on every write. |

**Table: `reservations`**
| Column     | Type | Notes                                    |
| :--------- | :--- | :--------------------------------------- |
| `order_id` | UUID | Unique Constraint (Enforces Idempotency) |
| `status`   | ENUM | `RESERVED`, `CONFIRMED`, `CANCELLED`     |

---

## 4. Local Development

**Run Service**:
```bash
./mvnw -pl inventory-service spring-boot:run
```
