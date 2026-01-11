# 🔔 Notification Service (`notification-service`)

**Role**: Real-Time Push Notifications (SSE).

The **Notification Service** bridges the gap between asynchronous backend processing and the user interface. Since orders are processed in the background (via Temporal), the frontend needs a way to know "What happened to my order?" without hitting Refresh 100 times. We use **Server-Sent Events (SSE)** for this.

---

## 1. API Reference

### 📡 Stream Endpoint

| Method | Endpoint                | Auth   | Description                                         |
| :----- | :---------------------- | :----- | :-------------------------------------------------- |
| `GET`  | `/notifications/stream` | ✅ User | Opens a persistent, one-way channel to the browser. |

**The Protocol**:
1.  Frontend calls `/notifications/stream`.
2.  Server keeps connection open (Timeout: Long).
3.  Server pushes data packets starting with `data:`.

---

## 2. Integration Flow

This service is a **Consumer** of Kafka events. It does not generate data itself; it forwards data.

**The Pipeline**:
1.  **Order Service** executes a status change (e.g., `PENDING` -> `PAYMENT_PENDING`).
2.  **Order Service** publishes event to Kafka topic `order.progress`.
3.  **Notification Service** consumes this event.
4.  **Notification Service** finds the `SseEmitter` associated with that `userId`.
5.  **Notification Service** pushes the payload to the open HTTP connection.
6.  **Browser** (`onMessage` handler) receives the JSON and updates the UI.

---

## 3. Configuration

| Variable                     | Description          |
| :--------------------------- | :------------------- |
| `KAFKA_BOOTSTRAP`            | Kafka Broker Address |
| `KAFKA_TOPIC_ORDER_PROGRESS` | Topic to listen to   |

---

## 4. Local Development

**Run Service**:
```bash
./mvnw -pl notification-service spring-boot:run
```

**Test with Curl**:
```bash
# Get a Token first
curl -N -H "Authorization: Bearer <TOKEN>" http://localhost:8080/notifications/stream
# You will see the connection hang (waiting for events). 
# Trigger an order in another terminal to see output.
```
