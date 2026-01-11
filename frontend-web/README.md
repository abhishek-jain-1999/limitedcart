# 📱 Frontend Web (Flutter)

**Role**: The Unified Experience.

This single Flutter Web application serves all three user personas (**Customer, Admin, Warehouse**) dynamically based on their login roles. It replaces the traditional separation of "Storefront" vs "Admin Panel".

---

## 1. Features by Lifecycle

### 🛒 Customer Experience
- **Flash Sale Ordering**: "Buy Now" triggers the async order flow. The UI immediately shows a "Reserving..." state.
- **Real-Time Tracking**: The Order Status page connects to SSE. You can watch the timeline tick from `PENDING` -> `PAYMENT` -> `CONFIRMED` without refreshing.
- **Deep-Link Payments**: Redirection to `/payment?token=...` handles the checkout securely.

### 👮 Admin Portal (`ROLE_ADMIN`)
- **Dashboard**: High-level metrics.
- **User Management**: Promote users to Warehouse staff.
- **Product Catalog**: Create, Edit, Delete products.

### 📦 Warehouse Portal (`ROLE_WAREHOUSE`)
- **Inventory View**: See real-time stock levels.
- **Restock**: One-click restocking for low-inventory items.

---

## 2. Directory Structure

```text
lib/
├── core/               # Config, Constants, Dio Client
├── features/
│   ├── auth/           # Login UI, Guard Logic
│   ├── orders/         # Order Logic, SSE Listeners
│   ├── admin/          # Admin-only Screens
│   ├── warehouse/      # Warehouse-only Screens
│   └── products/       # Public Catalog
└── main.dart           # App Entry & Router
```

---

## 3. Configuration

Configuration is handled via `lib/core/config/api_config.dart`.

| Constant   | Description         | Default (Dev)                                |
| :--------- | :------------------ | :------------------------------------------- |
| `BASE_URL` | API Gateway         | `http://localhost:8080`                      |
| `SSE_URL`  | Notification Stream | `http://localhost:8080/notifications/stream` |

---

## 4. Running Locally

**Prerequisites**: [Flutter SDK](https://docs.flutter.dev/get-started/install).

1.  **Install Dependencies**:
    ```bash
    flutter pub get
    ```

2.  **Run in Chrome**:
    ```bash
    flutter run -d chrome
    ```

3.  **Build for Production**:
    ```bash
    flutter build web --release
    # Outputs to /build/web
    ```
