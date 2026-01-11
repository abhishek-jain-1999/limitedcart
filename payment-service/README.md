# 💳 Payment Service (`payment-service`)

**Role**: Payment Processor & Link Generator.

The **Payment Service** handles the financial transaction. In a modern e-commerce flow, payments are rarely synchronous (i.e., you don't send card details in the `POST /order` call). Instead, we generate a secure **Payment Link** that the user can visit. This handles 3D Secure, Strong Customer Authentication (SCA), and external gateway redirects (PayPal/Stripe) without blocking the backend.

---

## 1. API Reference

### 💸 Customer Operations

| Method | Endpoint             | Auth    | Description                                        |
| :----- | :------------------- | :------ | :------------------------------------------------- |
| `POST` | `/payments/initiate` | ✅ User  | Generates a payment link for a specific order.     |
| `POST` | `/payments/process`  | ⏳ Token | **Public**. User submits sensitive card data here. |

**Initiate Response:**
```json
{
  "paymentId": "pay-999...",
  "paymentLink": "http://frontend.com/payment?token=secure-token-123"
}
```

### 🔍 System Operations
| Method | Endpoint               | Description                                                      |
| :----- | :--------------------- | :--------------------------------------------------------------- |
| `GET`  | `/payments/order/{id}` | Poll payment status.                                             |
| `POST` | `/payments/refund`     | **Compensation**. Refunds a transaction if the Saga fails later. |

---

## 2. The Mock Processor (Dev Mode)

Since we cannot connect to real Stripe/PayPal in this demo, the service includes a **Deterministic Mock Processor**. This allows you to test both Success and Failure paths by using specific "Magic Numbers".

**Logic Matrix:**

| Card Number Ending             | Simulated Outcome | App Response                    |
| :----------------------------- | :---------------- | :------------------------------ |
| **...0** (e.g., `4242...4240`) | ❌ **Decline**     | `FAILED` - "Processor Declined" |
| **Any other digit**            | ✅ **Success**     | `SUCCEEDED`                     |

**Security Note**: In a real implementation, this service would effectively be a Proxy to the external gateway (Stripe API) and would mostly handle Webhooks.

---

## 3. Configuration

| Variable                    | Description                                   |
| :-------------------------- | :-------------------------------------------- |
| `PAYMENT_FRONTEND_BASE_URL` | Used to construct the `paymentLink` redirect. |
| `ORDER_SERVICE_URL`         | Used to send the "Payment Success" webhook.   |

---

## 4. Local Development

**Run Service**:
```bash
./mvnw -pl payment-service spring-boot:run
```
