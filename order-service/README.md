# 🛒 Order Service (`order-service`)

**Role**: Flash Sale Ingestion & Saga Initiator.

The **Order Service** is the high-velocity entry point for the platform. It is engineered to handle "Flash Sales" where thousands of users hit the "Buy" button simultaneously. Instead of processing the entire transaction synchronously (which would block threads and crash the DB), it uses an **Async Placement (202 Accepted)** pattern to accept the order "intent" and offload the heavy lifting.

---

## 1. API Reference

### 🛍️ Customer Operations
*Requires `ROLE_USER`.*

| Method | Endpoint                       | Response       | Description                                                       |
| :----- | :----------------------------- | :------------- | :---------------------------------------------------------------- |
| `POST` | `/orders`                      | `202 Accepted` | **Async**. Validates price, saves intent, returns ID immediately. |
| `GET`  | `/orders/{id}`                 | `200 OK`       | Poll status (`PENDING` -> `PAYMENT_PENDING` -> `CONFIRMED`).      |
| `POST` | `/orders/{id}/cancel`          | `200 OK`       | Request cancellation (see Logic below).                           |
| `POST` | `/orders/{id}/payment-success` | `200 OK`       | Webhook from Payment Service.                                     |

**Create Order Response:**
```json
{
  "orderId": "770e8400-a55b-...",
  "status": "PENDING",
  "message": "Order reserved successfully. Processing payment..."
}
```

---

## 2. Business Logic: The "Flash Sale" Flow (Option 1)

**Goal**: Respond in < 20ms to prevent connection pile-up.

1.  **Fail Fast**: Check price cache (Redis). If missing, error.
2.  **Persist Intent**: Save a minimal `OrderEntity` with status `PENDING` to Postgres. This ensures the user can immediately "Get" the order by ID, preventing 404s.
3.  **Publish Event**: Send `OrderReservationEvent` to Kafka (`orders.reservations`).
4.  **Return 202**: Reply to the user.
5.  **Process (Worker)**: The `temporal-worker` picks up the event and starts the distributed transaction.

### Order Cancellation Logic
Users can cancel orders *if* they haven't been shipped yet.

1.  **Check Status**: Allowed only if status is `PENDING`, `INVENTORY_RESERVED`, or `PAYMENT_PENDING`.
2.  **Stop Workflow**: Calls Temporal `workflow.cancel()`.
3.  **Compensate**: Temporal triggers `OrderWorkflow` cancellation handler -> Releases Inventory -> Refunds Payment (if any).
4.  **Update DB**: Sets status to `CANCELLED`.

---

## 3. State Machine

The order moves through specific states driven by the Saga:

| State                | transitions To       | Trigger                               |
| :------------------- | :------------------- | :------------------------------------ |
| `PENDING`            | `INVENTORY_RESERVED` | Worker successfully locks stock.      |
| `INVENTORY_RESERVED` | `PAYMENT_PENDING`    | System requests payment from user.    |
| `PAYMENT_PENDING`    | `CONFIRMED`          | Payment Webhook received.             |
| `PAYMENT_PENDING`    | `FAILED`             | Payment timeout (15 mins) or decline. |
| `*`                  | `CANCELLED`          | User requested cancellation.          |

---

## 4. Configuration

| Variable                         | Description   | Topic/Queue Name                 |
| :------------------------------- | :------------ | :------------------------------- |
| `KAFKA_BOOTSTRAP`                | Kafka Broker  | `localhost:9092`                 |
| `KAFKA_TOPIC_ORDER_RESERVATIONS` | Async Trigger | `orders.reservations`            |
| `KAFKA_TOPIC_ORDER_CREATED`      | Event Log     | `orders.created`                 |
| `KAFKA_TOPIC_ORDER_PROGRESS`     | Event Log     | `order.progress` (SSE uses this) |

---

## 5. Local Development

**Run Service**:
```bash
./mvnw -pl order-service spring-boot:run
```
