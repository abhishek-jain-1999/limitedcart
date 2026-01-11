# ⚙️ Temporal Worker (`temporal-worker`)

**Role**: The Brain (Saga Orchestrator).

The **Temporal Worker** is a "headless" service. It does not expose a REST API. Instead, it connects to the **Temporal Server** and polls for tasks. It executes the **Distributed Saga** that coordinates Order, Inventory, and Payment services.

**Why Temporal?**
In a microservices architecture, transactions span multiple databases. We cannot use ACID database transactions. Temporal gives us "Durable Execution" - if the worker crashes in the middle of a workflow, it resumes exactly where it left off, guaranteeing that we never lose an order or leave inventory in a reserved state forever.

---

## 1. The `OrderWorkflow` Saga

The workflow represents the life of an order. It is defined as code (`OrderWorkflow.java`) but executes as a persistent state machine.

### Happy Path Steps
1.  **Activity**: `Inventory.reserve(orderId)`
    *   *Result*: Inventory Locked.
2.  **Activity**: `Order.updateStatus(PAYMENT_PENDING)`
3.  **Signal Wait**: `WaitForPayment` (Timeout: 15 mins)
    *   *Event*: User completes payment on frontend.
    *   *Signal Received*: "PAID".
4.  **Activity**: `Inventory.confirm(orderId)`
5.  **Activity**: `Order.finalize(CONFIRMED)`

### Compensation Path (Rollback)
If Payment fails (or timeout occurs):
1.  **Activity**: `Inventory.release(orderId)`
    *   *Result*: Stock returned to pool.
2.  **Activity**: `Order.fail(FAILED)`

---

## 2. Configuration

**Environment Variables**:

| Variable                | Description        | Default                         |
| :---------------------- | :----------------- | :------------------------------ |
| `TEMPORAL_TARGET`       | Temporal Server    | `localhost:7233`                |
| `TEMPORAL_TASK_QUEUE`   | Queue Name         | `ORDER_SAGA_QUEUE`              |
| `INVENTORY_SERVICE_URL` | REST Client Target | `http://inventory-service:8080` |
| `PAYMENT_SERVICE_URL`   | REST Client Target | `http://payment-service:8080`   |
| `ORDER_SERVICE_URL`     | REST Client Target | `http://order-service:8080`     |

---

## 3. Local Development

**Run Worker**:
```bash
./mvnw -pl temporal-worker spring-boot:run
```

**Temporal Cloud/UI**:
Access the Dashboard at [http://localhost:8088](http://localhost:8088) to visualize your workflows running in real-time.
