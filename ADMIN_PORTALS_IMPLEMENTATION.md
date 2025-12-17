# Admin Portals Implementation Summary

## Backend Complete ✅

All necessary backend endpoints have been created:

### Auth Service - User Management
- `GET /admin/users` - List all users with roles
- `PATCH /admin/users/{userId}/roles` - Update user  roles
- `GET /admin/metrics` - Platform metrics

Files created:
- `auth-service/src/main/kotlin/com/abhishek/limitedcart/auth/dto/AdminDtos.kt`
- `auth-service/src/main/kotlin/com/abhishek/limitedcart/auth/controller/AdminController.kt`
- `auth-service/src/main/kotlin/com/abhishek/limitedcart/auth/service/AdminService.kt`

### Product Service - Product CRUD
- `POST /products` - Create product (ROLE_ADMIN)
- `PUT /products/{id}` - Update product (ROLE_ADMIN)
- `DELETE /products/{id}` - Delete product (ROLE_ADMIN)
- `GET /products` - List products (public)

Updated: `product-service/src/main/kotlin/com/abhishek/limitedcart/product/controller/ProductController.kt`

### Inventory Service - Stock Management
- `GET /inventory/stock` - List all stock (with product names)
- `GET /inventory/summary` - Inventory summary (ROLE_WAREHOUSE, ROLE_ADMIN)
- `POST /inventory/restock` - Restock (ROLE_WAREHOUSE, ROLE_ADMIN)

Files updated:
- `inventory-service/src/main/kotlin/com/abhishek/limitedcart/inventory/controller/InventoryController.kt`
- `inventory-service/src/main/kotlin/com/abhishek/limitedcart/inventory/service/InventoryService.kt`
- `inventory-service/src/main/kotlin/com/abhishek/limitedcart/inventory/entity/Stock.kt` (added productName)
- `inventory-service/src/main/resources/db/migration/V2__add_product_name.sql`

## Frontend - Admin API Service

To complete the frontend admin portals, extend `frontend/src/services/api.ts` with:

```typescript
// Admin - User Management
async getUsers(): Promise<UserListView[]> {
  const response = await fetch(`${API_BASE_URL}/admin/users`, {
    headers: this.getHeaders(true),
  });
  return this.handleResponse<UserListView[]>(response);
}

async updateUserRoles(userId: string, roles: string[]): Promise<UserListView> {
  const response = await fetch(`${API_BASE_URL}/admin/users/${userId}/roles`, {
    method: 'PATCH',
    headers: this.getHeaders(true),
    body: JSON.stringify({ roles }),
  });
  return this.handleResponse<UserListView>(response);
}

async getAdminMetrics(): Promise<AdminMetrics> {
  const response = await fetch(`${API_BASE_URL}/admin/metrics`, {
    headers: this.getHeaders(true),
  });
  return this.handleResponse<AdminMetrics>(response);
}

// Admin - Product Management
async deleteProduct(id: string): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/products/${id}`, {
    method: 'DELETE',
    headers: this.getHeaders(true),
  });
  if (!response.ok) throw new Error('Failed to delete product');
}

async updateProduct(id: string, data: Partial<Product>): Promise<Product> {
  const response = await fetch(`${API_BASE_URL}/products/${id}`, {
    method: 'PUT',
    headers: this.getHeaders(true),
    body: JSON.stringify(data),
  });
  return this.handleResponse<Product>(response);
}

async createProduct(data: Omit<Product, 'id'>): Promise<Product> {
  const response = await fetch(`${API_BASE_URL}/products`, {
    method: 'POST',
    headers: this.getHeaders(true),
    body: JSON.stringify(data),
  });
  return this.handleResponse<Product>(response);
}

// Warehouse - Inventory Management
async getInventoryStock(): Promise<StockView[]> {
  const response = await fetch(`${API_BASE_URL}/inventory/stock`, {
    headers: this.getHeaders(true),
  });
  return this.handleResponse<StockView[]>(response);
}

async getInventorySummary(): Promise<InventorySummary> {
  const response = await fetch(`${API_BASE_URL}/inventory/summary`, {
    headers: this.getHeaders(true),
  });
  return this.handleResponse<InventorySummary>(response);
}

async restockProduct(productId: string, quantity: number): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/inventory/restock`, {
    method: 'POST',
    headers: this.getHeaders(true),
    body: JSON.stringify({ productId, quantity }),
  });
  if (!response.ok) throw new Error('Failed to restock');
}
```

## Frontend Structure

### 1. Role Detection  & Routing

Add to `AuthContext.tsx`:

```typescript
import {jwtDecode} from 'jwt-decode';

interface DecodedToken {
  userId: string;
  roles: string[];
}

// In AuthProvider
const [roles, setRoles] = useState<string[]>([]);

const decodeToken = (token: string) => {
  const decoded = jwtDecode<DecodedToken>(token);
  return decoded.roles || [];
};

// After login
const login = async (email: string, password: string) => {
  const response = await apiService.login(email, password);
  setToken(response.token);
  setUser(response.user);
  const userRoles = decodeToken(response.token);
  setRoles(userRoles);
  localStorage.setItem('jwtToken', response.token);
  localStorage.setItem('user', JSON.stringify(response.user));
  localStorage.setItem('roles', JSON.stringify(userRoles));
};

// Helpers
const hasRole = (role: string) => roles.includes(role);
const is Admin = () => hasRole('ROLE_ADMIN');
const isWarehouse = () => hasRole('ROLE_WAREHOUSE') || isAdmin();
```

### 2. Protected Route Component

Create `frontend/src/components/ProtectedRoute.tsx`:

```typescript
import { Navigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

export const ProtectedRoute: React.FC<{
  children: React.ReactNode;
  requiredRole?: string;
}> = ({ children, requiredRole }) => {
  const { isAuthenticated, roles } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (requiredRole && !roles.includes(requiredRole)) {
    return <Navigate to="/access-denied" replace />;
  }

  return <>{children}</>;
};
```

### 3. Admin/Warehouse Routes

Update `App.tsx`:

```typescript
<Routes>
  {/* Public routes */}
  <Route path="/" element={<Navigate to="/products" replace />} />
  <Route path="/login" element={<LoginPage />} />
  <Route path="/signup" element={<SignupPage />} />
  <Route path="/products" element={<ProductsPage />} />
  <Route path="/products/:id" element={<ProductDetailPage />} />
  <Route path="/orders/:orderId/updates" element={<OrderUpdatesPage />} />
  <Route path="/payment" element={<PaymentPage />} />

  {/* Warehouse routes */}
  <Route path="/warehouse/*" element={
    <ProtectedRoute requiredRole="ROLE_WAREHOUSE">
      <WarehouseLayout />
    </ProtectedRoute>
  }>
    <Route path="dashboard" element={<WarehouseDashboard />} />
    <Route path="inventory" element={<InventoryManagement />} />
    <Route path="products" element={<WarehouseProducts />} />
  </Route>

  {/* Admin routes */}
  <Route path="/admin/*" element={
    <ProtectedRoute requiredRole="ROLE_ADMIN">
      <AdminLayout />
    </ProtectedRoute>
  }>
    <Route path="dashboard" element={<AdminDashboard />} />
    <Route path="products" element={<AdminProducts />} />
    <Route path="users" element={<UserManagement />} />
  </Route>

  <Route path="/access-denied" element={<AccessDenied />} />
</Routes>
```

### 4. Key Components to Create

#### Warehouse Portal

**WarehouseLayout** (`frontend/src/pages/warehouse/WarehouseLayout.tsx`):
- Sidebar with navigation
- Logout button
- Outlet for nested routes

**WarehouseDashboard** (`frontend/src/pages/warehouse/WarehouseDashboard.tsx`):
- Summary cards (totalProducts, totalQuantity, lowStockCount)
- Fetch from `/inventory/summary`

**InventoryManagement** (`frontend/src/pages/warehouse/InventoryManagement.tsx`):
- Table showing stock with columns: Product Name, Available, Reserved, Total, Actions
- Restock button opens modal
- Modal: input quantity,submit to `/inventory/restock`

**WarehouseProducts** (`frontend/src/pages/warehouse/WarehouseProducts.tsx`):
- Read-only product list
- Reuse ProductsPage but disable actions

#### Admin Portal

**AdminLayout** (`frontend/src/pages/admin/AdminLayout.tsx`):
- Sidebar with navigation (Dashboard, Products, Users)
- Logout button
- Outlet for nested routes

**AdminDashboard** (`frontend/src/pages/admin/AdminDashboard.tsx`):
- Metrics cards (totalUsers, totalAdmins, totalWarehouseStaff)
- Fetch from `/admin/metrics`

**AdminProducts** (`frontend/src/pages/admin/AdminProducts.tsx`):
- Table with Edit/Delete buttons
- "Add Product" button opens modal
- Edit modal: pre-fill form, PUT to `/products/{id}`
- Delete: confirmation dialog, DELETE to `/products/{id}`

**UserManagement** (`frontend/src/pages/admin/UserManagement.tsx`):
- Table: Email, Roles, Actions
- Dropdown to change roles
- PATCH to `/admin/users/{userId}/roles`

### 5. Example: Warehouse Dashboard

```typescript
import React, { useEffect, useState } from 'react';
import { apiService } from '../../services/api';
import type { InventorySummary } from '../../types';

export const WarehouseDashboard: React.FC = () => {
  const [summary, setSummary] = useState<InventorySummary | null>(null);

  useEffect(() => {
    loadSummary();
  }, []);

  const loadSummary = async () => {
    const data = await apiService.getInventorySummary();
    setSummary(data);
  };

  if (!summary) return <div>Loading...</div>;

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-6">Warehouse Dashboard</h1>
      
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <StatCard label="Total Products" value={summary.totalProducts} />
        <StatCard label="Total Quantity" value={summary.totalQuantity} />
        <StatCard label="Reserved" value={summary.totalReserved} />
        <StatCard 
          label="Low Stock Alerts" 
          value={summary.lowStockCount}
          className="bg-red-50"
        />
      </div>
    </div>
  );
};

const StatCard: React.FC<{ label: string; value: number; className?: string }> = ({
  label,
  value,
  className = ''
}) => (
  <div className={`bg-white p-4 rounded shadow ${className}`}>
    <div className="text-gray-600 text-sm">{label}</div>
    <div className="text-3xl font-bold mt-2">{value}</div>
  </div>
);
```

## Testing

1. **Create Test Users**:
   - Admin user: roles = ["ROLE_ADMIN"]
   - Warehouse user: roles = ["ROLE_WAREHOUSE"]
   - Regular user: roles = ["ROLE_USER"]

2. **Test Flows**:
   - Login as warehouse → redirect to `/warehouse/dashboard`
   - View inventory summary
   - Restock a product
   - Login as admin → redirect to `/admin/dashboard`
   - Create/edit/delete products
   - Change user roles
   - Login as regular user → should only access customer views

## Docker Compose Updates

No changes needed! All services are already configured.  Just rebuild services:

```bash
docker-compose up --build
```

## Summary

**Backend ✅ Complete**:
- All admin endpoints created
- Role-based security configured
- Flyway migration for product_name added

**Frontend 🔨 To Complete**:
1. Add admin API methods to api.ts
2. Install jwt-decode: `npm install jwt-decode`
3. Update AuthContext with role detection
4. Create ProtectedRoute component
5. Create Warehouse portal pages (Layout, Dashboard, Inventory)
6. Create Admin portal pages (Layout, Dashboard, Products, Users)
7. Update App.tsx with new routes

All backend is ready. The frontend structure is designed to extend the existing customer portal with minimal code duplication by reusing components and styling.
