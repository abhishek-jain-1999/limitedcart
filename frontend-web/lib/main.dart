import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:flutter_web_plugins/url_strategy.dart';
import 'package:flutter/foundation.dart';

import 'core/api_client.dart';
import 'core/logger.dart';
import 'features/auth/auth_provider.dart';
import 'features/auth/login_page.dart';
import 'features/auth/signup_page.dart';
import 'features/products/product_provider.dart';
import 'features/products/products_page.dart';
import 'features/products/product_detail_page.dart';
import 'features/orders/order_provider.dart';
import 'features/orders/order_tracking_page.dart';
import 'features/payment/payment_page.dart';
import 'features/admin/admin_provider.dart';
import 'features/admin/admin_dashboard.dart';
import 'features/admin/admin_products.dart';
import 'features/admin/user_management.dart';
import 'features/warehouse/warehouse_provider.dart';
import 'features/warehouse/warehouse_dashboard.dart';
import 'features/warehouse/inventory_management.dart';
import 'features/warehouse/warehouse_products.dart';
import 'shared/access_denied_page.dart';
import 'shared/loading_page.dart';

void main() {
  usePathUrlStrategy();
  
  // Enable debug logging in debug mode
  AppLogger.setDebugMode(kDebugMode);
  
  runApp(const LimitedCartApp());
}

class LimitedCartApp extends StatelessWidget {
  const LimitedCartApp({super.key});

  @override
  Widget build(BuildContext context) {
    final apiClient = ApiClient();

    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => AuthProvider(apiClient)),
        ChangeNotifierProvider(create: (_) => ProductProvider(apiClient)),
        ChangeNotifierProxyProvider<AuthProvider, OrderProvider>(
          create: (context) => OrderProvider(apiClient, context.read<AuthProvider>()),
          update: (context, auth, previous) {
            if (previous != null) {
              previous.updateAuth(auth); // ✅ Update auth reference
              return previous;           // ✅ Reuse instance
            }
            return OrderProvider(apiClient, auth);
          },
        ),
        ChangeNotifierProvider(create: (_) => AdminProvider(apiClient)),
        ChangeNotifierProvider(create: (_) => WarehouseProvider(apiClient)),
      ],
      child: const AppRouter(),
    );
  }
}

class AppRouter extends StatefulWidget {
  const AppRouter({super.key});

  @override
  State<AppRouter> createState() => _AppRouterState();
}

class _AppRouterState extends State<AppRouter> {
  late final GoRouter _router;
  String? _originalPath; // Store the deep link

  @override
  void initState() {
    super.initState();
    final authProvider = context.read<AuthProvider>();

    _router = GoRouter(
      initialLocation: '/_loading',
      refreshListenable: authProvider,
      redirect: (context, state) {
        final isLoading = authProvider.isLoading;
        final isLoggedIn = authProvider.isAuthenticated;
        final path = state.uri.path;
        final isAuthRoute = path == '/login' || path == '/signup';
        final isPublicRoute = path.startsWith('/payment');

        AppLogger.info('redirect: isLoading=$isLoading, auth=$isLoggedIn, path=$path');


        // 1. Public Routes (Always Allowed)
        if (isPublicRoute) return null;

        // 2. Handle Loading Phase & Deep Link Preservation
        if (isLoading) {
          if (path != '/_loading') _originalPath = path;
          return path == '/_loading' ? null : '/_loading';
        }

        // 3. Restore Deep Link after Loading
        if (path == '/_loading') {
          // If logged in, go to deep link or dashboard
          if (isLoggedIn) {
            final target = _originalPath ?? authProvider.getRedirectPath();
            _originalPath = null;
            return target != '/_loading' ? target : authProvider.getRedirectPath();
          }
          // If not logged in, go to login (we'll capture deep link later if needed, but for now just login)
          return '/login';
        }

        // 4. Role-Based Access Control
        if (path.startsWith('/admin') && !authProvider.isAdmin()) return '/access-denied';
        if (path.startsWith('/warehouse') && !authProvider.isWarehouse()) return '/access-denied';


        // 5. Protected Routes (Require Login)
        if (!isLoggedIn && !isAuthRoute) return '/login';

        // 6. Auth Page Redirect (Already Logged In)
        if (isLoggedIn && isAuthRoute) return authProvider.getRedirectPath();

        // 7. Root Redirect
        if (path == '/') return isLoggedIn ? authProvider.getRedirectPath() : '/login';

        return null;
      },
      routes: [
        GoRoute(path: '/_loading', builder: (context, state) => const LoadingPage()),
        GoRoute(path: '/login', builder: (context, state) => const LoginPage()),
        GoRoute(path: '/signup', builder: (context, state) => const SignupPage()),
        GoRoute(path: '/access-denied', builder: (context, state) => const AccessDeniedPage()),
        GoRoute(
          path: '/products',
          builder: (context, state) => const ProductsPage(),
          routes: [
            GoRoute(
              path: ':id',
              builder: (context, state) {
                final id = state.pathParameters['id'] ?? '';
                return ProductDetailPage(productId: id);
              },
            ),
          ],
        ),
        GoRoute(
          path: '/orders/:id',
          builder: (context, state) {
            final id = state.pathParameters['id'] ?? '';
            return OrderTrackingPage(orderId: id);
          },
        ),
        GoRoute(
          path: '/payment',
          builder: (context, state) {
            final orderId = state.uri.queryParameters['orderId'];
            final token = state.uri.queryParameters['token'];
            return PaymentPage(orderId: orderId, token: token);
          },
        ),
        // Admin Routes
        GoRoute(path: '/admin/dashboard', builder: (context, state) => const AdminDashboard()),
        GoRoute(path: '/admin/products', builder: (context, state) => const AdminProducts()),
        GoRoute(path: '/admin/users', builder: (context, state) => const UserManagement()),
        // Warehouse Routes
        GoRoute(path: '/warehouse/dashboard', builder: (context, state) => const WarehouseDashboard()),
        GoRoute(path: '/warehouse/inventory', builder: (context, state) => const InventoryManagement()),
        GoRoute(path: '/warehouse/products', builder: (context, state) => const WarehouseProducts()),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'LimitedCart',
      theme: ThemeData(
        primarySwatch: Colors.indigo,
        primaryColor: const Color(0xFF4F46E5),
        scaffoldBackgroundColor: Colors.grey[50],
        useMaterial3: true,
        fontFamily: 'Inter',
      ),
      routerConfig: _router,
      debugShowCheckedModeBanner: false,
    );
  }
}
