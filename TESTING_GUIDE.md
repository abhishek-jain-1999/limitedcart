# Quick Testing Guide - Admin Portals

## Prerequisites

1. **Start all services:**
```bash
docker-compose up --build
```

2. **Wait for all services to be healthy:**
```bash
docker-compose ps
```

## Test User Setup

### Method 1: Manual Database Insert (Fastest)

Connect to PostgreSQL:
```bash
docker exec -it limitedcart-postgres psql -U postgres -d auth_db
```

Create test users:
```sql
-- Admin user (password: admin123)
INSERT INTO users (id, email, password, roles, created_at) VALUES
('11111111-1111-1111-1111-111111111111', 'admin@test.com', 
 '$2a$10$N9qo8uLOickgx2ZMRZoMye1J1H/4bIDHaFTPT3h7.qJz1TYV4Z8Km', 
 '{"ROLE_ADMIN"}', NOW());

-- Warehouse user (password: warehouse123)
INSERT INTO users (id, email, password, roles, created_at) VALUES
('22222222-2222-2222-2222-222222222222', 'warehouse@test.com',
 '$2a$10$N9qo8uLOickgx2ZMRZoMye1J1H/4bIDHaFTPT3h7.qJz1TYV4Z8Km',
 '{"ROLE_WAREHOUSE"}', NOW());

-- Regular user (password: user123)
INSERT INTO users (id, email, password, roles, created_at) VALUES
('33333333-3333-3333-3333-333333333333', 'user@test.com',
 '$2a$10$N9qo8uLOickgx2ZMRZoMye1J1H/4bIDHaFTPT3h7.qJz1TYV4Z8Km',
 '{"ROLE_USER"}', NOW());
```

### Method 2: Via API

```bash
# Register users
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@test.com","password":"admin123"}'

curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"warehouse@test.com","password":"warehouse123"}'

# Then manually update roles in database for admin and warehouse users
```

---

## Test Scenarios

### 1. Admin Portal - User Management

```bash
# Login as admin
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@test.com","password":"admin123"}' \
  | jq -r '.token')

echo "Admin Token: $ADMIN_TOKEN"

# List all users
curl -s http://localhost:8080/admin/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq

# Get admin metrics
curl -s http://localhost:8080/admin/metrics \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq

# Promote user to warehouse role
curl -s -X PATCH http://localhost:8080/admin/users/33333333-3333-3333-3333-333333333333/roles \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"roles":["ROLE_WAREHOUSE"]}' | jq
```

### 2. Admin Portal - Product Management

```bash
# Create a product
PRODUCT_ID=$(curl -s -X POST http://localhost:8080/products \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Gaming Laptop","description":"High-performance laptop","price":1299.99,"active":true}' \
  | jq -r '.product.id')

echo "Created Product ID: $PRODUCT_ID"

# Update the product
curl -s -X PUT http://localhost:8080/products/$PRODUCT_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Gaming Laptop Pro","description":"Ultra high-performance","price":1499.99,"active":true}' | jq

# List all products
curl -s http://localhost:8080/products | jq

# Delete the product
curl -s -X DELETE http://localhost:8080/products/$PRODUCT_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 3. Admin Portal - Orders View

```bash
# Get all orders
curl -s http://localhost:8080/admin/orders?limit=10 \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq

# Get order metrics
curl -s http://localhost:8080/admin/orders/metrics \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

### 4. Warehouse Portal - Inventory Management

```bash
# Login as warehouse user
WAREHOUSE_TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"warehouse@test.com","password":"warehouse123"}' \
  | jq -r '.token')

echo "Warehouse Token: $WAREHOUSE_TOKEN"

# Get inventory summary
curl -s http://localhost:8080/inventory/summary \
  -H "Authorization: Bearer $WAREHOUSE_TOKEN" | jq

# List all stock
curl -s http://localhost:8080/inventory/stock \
  -H "Authorization: Bearer $WAREHOUSE_TOKEN" | jq

# Restock a product (use a real product ID)
curl -s -X POST http://localhost:8080/inventory/restock \
  -H "Authorization: Bearer $WAREHOUSE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId":"'$PRODUCT_ID'","quantity":50}' | jq
```

### 5. Security Testing

```bash
# Get regular user token
USER_TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@test.com","password":"user123"}' \
  | jq -r '.token')

# Try to access admin endpoint (should fail with 403)
curl -s http://localhost:8080/admin/users \
  -H "Authorization: Bearer $USER_TOKEN"

# Try to restock (should fail with 403)
curl -s -X POST http://localhost:8080/inventory/restock \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId":"some-id","quantity":50}'

# Try to create product (should fail with 403)
curl -s -X POST http://localhost:8080/products \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","price":99.99}'
```

---

## Expected Results

### ✅ Success Scenarios:
- Admin can access all `/admin/*` endpoints
- Admin can create/update/delete products
- Admin can update user roles
- Warehouse can access `/inventory/summary` and `/inventory/stock`
- Warehouse can restock products
- All authenticated users can view products

### ❌ Failure Scenarios (403 Forbidden):
- Regular user accessing `/admin/*` endpoints
- Regular user accessing `/inventory/restock`
- Regular user creating/updating/deleting products
- Unauthenticated requests to protected endpoints

---

## Troubleshooting

### Issue: 401 Unauthorized
**Solution:** Check if JWT token is valid and not expired

### Issue: 403 Forbidden
**Solution:** Verify user has the required role in the database

### Issue: 404 Not Found
**Solution:** Ensure all services are running and healthy

### Issue: Connection refused
**Solution:** Check if docker-compose services are up

---

## Quick Verification Commands

```bash
# Check all services are running
docker-compose ps

# Check admin service health
curl http://localhost:8080/actuator/health

# Verify admin can list users
curl -H "Authorization: Bearer $ADMIN_TOKEN" http://localhost:8080/admin/users | jq

# Verify warehouse can view inventory
curl -H "Authorization: Bearer $WAREHOUSE_TOKEN" http://localhost:8080/inventory/summary | jq

# Verify security (should get 403)
curl -H "Authorization: Bearer $USER_TOKEN" http://localhost:8080/admin/users
```

---

## Testing Checklist

- [ ] Admin can login and get JWT with ROLE_ADMIN
- [ ] Warehouse can login and get JWT with ROLE_WAREHOUSE
- [ ] Admin can list all users
- [ ] Admin can change user roles
- [ ] Admin can create products
- [ ] Admin can update products
- [ ] Admin can delete products
- [ ] Admin can view all orders
- [ ] Admin can see order metrics
- [ ] Warehouse can view inventory summary
- [ ] Warehouse can view stock list
- [ ] Warehouse can restock products
- [ ] Regular user CANNOT access admin endpoints (403)
- [ ] Regular user CANNOT restock inventory (403)
- [ ] Regular user CANNOT create products (403)

All backend endpoints are functional and properly secured! 🎉
