# Frontend Payment Flow Documentation

## Overview

This document describes the complete frontend integration flow for the link-based payment system in LimitedCart. The system uses a redirect-based payment model where users are directed to a dedicated payment page to complete their transaction.

## Architecture

```
┌─────────────┐      ┌──────────────┐      ┌─────────────────┐      ┌─────────────────┐
│   Frontend  │─────▶│ Order Service│─────▶│ Temporal Worker │─────▶│ Payment Service │
│   (Client)  │      │              │      │    (Saga)       │      │                 │
└─────────────┘      └──────────────┘      └─────────────────┘      └─────────────────┘
       │                                                                      │
       │                                                                      ▼
       │              ┌──────────────────────────────────────┐      ┌─────────────────┐
       └─────────────▶│   Notification Service (SSE)         │      │  Payment Page   │
                      │   Real-time Order Progress Updates   │      │  (Frontend UI)  │
                      └──────────────────────────────────────┘      └─────────────────┘
```

## Complete Payment Flow

### Step 1: User Places Order

**Frontend Action:**
```javascript
POST /orders
Headers:
  Authorization: Bearer <JWT_TOKEN>
  Content-Type: application/json

Body:
{
  "productId": "uuid-of-product",
  "quantity": 2
}
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "amount": 39.98
}
```

**What Happens:**
- Order is created with status `PENDING`
- Temporal Saga is triggered automatically
- User receives `orderId` to track the order
- Frontend should store this `orderId` for tracking

---

### Step 2: Frontend Opens SSE Connection (Optional but Recommended)

**Frontend Action:**
```javascript
const eventSource = new EventSource(
  '/notifications/stream',
  {
    headers: {
      'Authorization': `Bearer ${jwtToken}`
    }
  }
);

eventSource.addEventListener('order-progress', (event) => {
  const data = JSON.parse(event.data);
  console.log('Order Progress:', data);
  // Update UI based on order status
  handleOrderProgress(data);
});

eventSource.addEventListener('connected', (event) => {
  console.log('Connected to notification stream');
});
```

**Order Progress Events You'll Receive:**
```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user-uuid",
  "status": "PLACED",
  "message": "Order placed successfully",
  "occurredAt": "2024-12-05T06:53:46Z"
}
```

**Status Progression:**
1. `PLACED` - Order created
2. `INVENTORY_RESERVED` - Inventory reserved for this order
3. `PAYMENT_PENDING` - Waiting for customer to complete payment
4. `PAYMENT_CONFIRMED` - Payment completed successfully
5. `PAID` - Payment successful
6. `CONFIRMED` - Order confirmed
7. `FAILED` - Order failed (with reason in message)

---

### Step 3: Frontend Initiates Payment

**When to Call:**
- After receiving `PAYMENT_PENDING` status via SSE, OR
- Immediately after order creation if not using SSE

**Frontend Action:**
```javascript
POST /payments/initiate
Headers:
  Authorization: Bearer <JWT_TOKEN>
  Content-Type: application/json

Body:
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "amount": 39.98,
  "currency": "USD"
}
```

**Response:**
```json
{
  "paymentId": "660e8400-e29b-41d4-a716-446655440001",
  "paymentLink": "http://localhost:3000/payment?token=abc123-payment-token-xyz789"
}
```

**Important Notes:**
- This endpoint requires authentication (user must be logged in)
- The `amount` should match the order amount
- If a payment already exists for the order and is still `PENDING`, the existing payment link will be returned

---

### Step 4: Redirect User to Payment Page

**Frontend Action:**
```javascript
// Option 1: Full page redirect
window.location.href = paymentLink;

// Option 2: Open in new tab/window
window.open(paymentLink, '_blank');

// Option 3: Display in iframe (if payment page supports it)
<iframe src={paymentLink} />
```

**Payment Page URL Structure:**
```
http://localhost:3000/payment?token=abc123-payment-token-xyz789
```

**What the Frontend Payment Page Should Do:**
1. Extract the `token` from URL query parameters
2. Display a payment form to collect card details
3. Submit card details to the payment processing endpoint

---

### Step 5: Payment Page Collects Card Details

**Payment Page UI Elements:**
```html
<form id="payment-form">
  <input name="cardNumber" placeholder="Card Number" required />
  <input name="expiryMonth" placeholder="MM" required />
  <input name="expiryYear" placeholder="YYYY" required />
  <input name="cvc" placeholder="CVC" required />
  <input name="cardHolderName" placeholder="Cardholder Name" required />
  <button type="submit">Pay Now</button>
</form>
```

**Important:**
- This page does NOT require user authentication
- Access is granted via the payment token in the URL
- The page should validate card details client-side before submission

---

### Step 6: Payment Page Processes Payment

**Frontend Action:**
```javascript
POST /payments/process
Headers:
  Content-Type: application/json

Body:
{
  "token": "abc123-payment-token-xyz789",
  "cardDetails": {
    "cardNumber": "4242424242424242",
    "expiryMonth": "12",
    "expiryYear": "2025",
    "cvc": "123",
    "cardHolderName": "John Doe"
  }
}
```

**Success Response:**
```json
{
  "success": true,
  "paymentId": "660e8400-e29b-41d4-a716-446655440001",
  "message": "Payment successful"
}
```

**Failure Response:**
```json
{
  "success": false,
  "paymentId": "660e8400-e29b-41d4-a716-446655440001",
  "message": "Payment declined by processor"
}
```

**Important:**
- This endpoint does NOT require authentication (uses token-based access)
- Idempotent: Multiple submissions with the same token won't create duplicate charges
- The backend automatically notifies the order service of success/failure

---

### Step 7: Payment Page Shows Result & Redirects

**On Success:**
```javascript
// Show success message
showSuccessMessage("Payment successful! Redirecting...");

// Redirect back to order confirmation page
setTimeout(() => {
  window.location.href = `/orders/${orderId}/confirmation`;
}, 2000);
```

**On Failure:**
```javascript
// Show error message
showErrorMessage(response.message);

// Allow user to retry or contact support
displayRetryButton();
```

---

### Step 8: Main App Receives Real-time Updates

**SSE Events on Original Page (if connection is still open):**

```javascript
// When payment is confirmed
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user-uuid",
  "status": "PAYMENT_CONFIRMED",
  "message": "Payment confirmed, finalizing order",
  "occurredAt": "2024-12-05T06:55:00Z"
}

// When payment is successful
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user-uuid",
  "status": "PAID",
  "message": "Payment successful",
  "occurredAt": "2024-12-05T06:55:01Z"
}

// When order is confirmed
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user-uuid",
  "status": "CONFIRMED",
  "message": "Order confirmed",
  "occurredAt": "2024-12-05T06:55:02Z"
}
```

**UI Updates Based on Status:**
```javascript
function handleOrderProgress(event) {
  switch(event.status) {
    case 'PLACED':
      showOrderStatus('Order placed, reserving inventory...');
      break;
    case 'INVENTORY_RESERVED':
      showOrderStatus('Inventory reserved, waiting for payment...');
      break;
    case 'PAYMENT_PENDING':
      showPaymentButton(event.orderId); // Trigger payment initiation
      break;
    case 'PAYMENT_CONFIRMED':
      showOrderStatus('Payment confirmed, finalizing...');
      break;
    case 'PAID':
      showOrderStatus('Payment successful!');
      break;
    case 'CONFIRMED':
      showOrderStatus('Order confirmed! 🎉');
      redirectToOrderDetails(event.orderId);
      break;
    case 'FAILED':
      showError(`Order failed: ${event.message}`);
      break;
  }
}
```

---

## Testing with Mock Payment Processor

The current implementation uses a `MockPaymentProcessor` with deterministic behavior:

**Card Numbers:**
- Cards ending in `0` will always **fail**
- Cards ending in any other digit will **succeed**

**Test Card Numbers:**
```
Success: 4242424242424242
Success: 5555555555554444
Failure: 4242424242424240
Failure: 1111111111111110
```

---

## Error Handling

### Payment Initiation Errors

```javascript
try {
  const response = await fetch('/payments/initiate', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ orderId, amount, currency })
  });
  
  if (!response.ok) {
    if (response.status === 409) {
      // Payment already succeeded
      showError('This order has already been paid');
    } else if (response.status === 401) {
      // Not authenticated
      redirectToLogin();
    } else {
      showError('Failed to initiate payment');
    }
    return;
  }
  
  const data = await response.json();
  window.location.href = data.paymentLink;
} catch (error) {
  showError('Network error. Please try again.');
}
```

### Payment Processing Errors

```javascript
try {
  const response = await fetch('/payments/process', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token, cardDetails })
  });
  
  const data = await response.json();
  
  if (data.success) {
    showSuccess('Payment successful!');
    setTimeout(() => redirectToOrder(orderId), 2000);
  } else {
    showError(data.message || 'Payment failed');
    enableRetry();
  }
} catch (error) {
  showError('Payment processing failed. Please try again.');
}
```

---

## Polling Alternative (if SSE not available)

If your frontend cannot use SSE, you can poll the payment status:

```javascript
async function pollPaymentStatus(orderId) {
  const maxAttempts = 60; // Poll for up to 5 minutes
  let attempts = 0;
  
  const interval = setInterval(async () => {
    attempts++;
    
    try {
      const response = await fetch(`/payments/order/${orderId}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      
      const payment = await response.json();
      
      if (payment.status === 'SUCCEEDED') {
        clearInterval(interval);
        showSuccess('Payment confirmed!');
        redirectToOrderConfirmation(orderId);
      } else if (payment.status === 'FAILED') {
        clearInterval(interval);
        showError('Payment failed');
      } else if (attempts >= maxAttempts) {
        clearInterval(interval);
        showWarning('Payment status unknown. Please check your orders.');
      }
    } catch (error) {
      console.error('Error polling payment status:', error);
    }
  }, 5000); // Poll every 5 seconds
}
```

---

## Security Considerations

1. **Authentication:**
   - `/payments/initiate` requires JWT authentication
   - User must own the order being paid for (backend validates this)

2. **Payment Link Security:**
   - Payment links use one-time tokens
   - Tokens are unique per payment attempt
   - No authentication required on payment page (token is sufficient)

3. **Idempotency:**
   - Multiple payment attempts with same token won't create duplicate charges
   - Backend tracks payment status and prevents double-charging

4. **PCI Compliance:**
   - Card details are never logged or stored
   - Current mock processor simulates real payment flow
   - When switching to real PSP, ensure PCI compliance

---

## Complete Example Flow (React/TypeScript)

```typescript
import { useState, useEffect } from 'react';

function OrderCheckout() {
  const [orderId, setOrderId] = useState<string | null>(null);
  const [orderStatus, setOrderStatus] = useState<string>('');
  const [paymentLink, setPaymentLink] = useState<string | null>(null);

  // Step 1: Create Order
  async function placeOrder(productId: string, quantity: number) {
    const response = await fetch('/orders', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${getToken()}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ productId, quantity })
    });
    
    const order = await response.json();
    setOrderId(order.id);
    setOrderStatus('PENDING');
  }

  // Step 2: Listen to SSE
  useEffect(() => {
    if (!orderId) return;

    const eventSource = new EventSource('/notifications/stream', {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    });

    eventSource.addEventListener('order-progress', (event) => {
      const data = JSON.parse(event.data);
      if (data.orderId === orderId) {
        setOrderStatus(data.status);
        
        if (data.status === 'PAYMENT_PENDING') {
          initiatePayment();
        }
      }
    });

    return () => eventSource.close();
  }, [orderId]);

  // Step 3: Initiate Payment
  async function initiatePayment() {
    const response = await fetch('/payments/initiate', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${getToken()}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ orderId, amount: 39.98, currency: 'USD' })
    });
    
    const payment = await response.json();
    setPaymentLink(payment.paymentLink);
  }

  // Step 4: Redirect to Payment
  function redirectToPayment() {
    if (paymentLink) {
      window.location.href = paymentLink;
    }
  }

  return (
    <div>
      <h1>Order Status: {orderStatus}</h1>
      {orderStatus === 'PAYMENT_PENDING' && (
        <button onClick={redirectToPayment}>
          Complete Payment
        </button>
      )}
      {orderStatus === 'CONFIRMED' && (
        <p>✅ Order confirmed! Thank you for your purchase.</p>
      )}
    </div>
  );
}
```

---

## API Endpoints Summary

| Endpoint                    | Method | Auth Required    | Purpose                           |
| --------------------------- | ------ | ---------------- | --------------------------------- |
| `/orders`                   | POST   | Yes (JWT)        | Create new order                  |
| `/notifications/stream`     | GET    | Yes (JWT)        | SSE stream for real-time updates  |
| `/payments/initiate`        | POST   | Yes (JWT)        | Generate payment link             |
| `/payments/process`         | POST   | No (token-based) | Process payment with card details |
| `/payments/order/{orderId}` | GET    | No               | Get payment status for polling    |

---

## Next Steps

1. **Build Payment UI Page:**
   - Create a dedicated `/payment` route in your frontend
   - Extract token from URL query params
   - Design payment form UI
   - Handle success/failure states

2. **Integrate SSE:**
   - Implement EventSource connection
   - Handle order-progress events
   - Update UI based on status changes

3. **Handle Edge Cases:**
   - Network failures during payment
   - User closing payment page mid-transaction
   - Session timeout scenarios
   - Browser back button after payment

4. **Testing:**
   - Test with various card numbers (ending in 0 vs others)
   - Test timeout scenarios
   - Test multiple tabs/windows
   - Test SSE reconnection

---

## Future Enhancements

When switching from `MockPaymentProcessor` to a real PSP (Stripe, Razorpay, etc.):

1. Replace `MockPaymentProcessor` with actual PSP SDK
2. Update `PaymentProcessor` interface implementation
3. Add webhook handling for async payment confirmations
4. Implement proper PCI compliance measures
5. Add 3D Secure / SCA support if required
6. Handle partial payments and refunds

The frontend flow remains the same - only backend payment processing changes!
