import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../features/auth/auth_provider.dart';

class Navbar extends StatelessWidget implements PreferredSizeWidget {
  const Navbar({super.key});

  @override
  Size get preferredSize => const Size.fromHeight(64);

  @override
  Widget build(BuildContext context) {
    final authProvider = context.watch<AuthProvider>();

    return AppBar(
      backgroundColor: Colors.white,
      elevation: 1,
      title: Row(
        children: [
          InkWell(
            onTap: () => context.go('/products'),
            child: const Row(
              children: [
                Icon(Icons.shopping_bag, color: Color(0xFF4F46E5)),
                SizedBox(width: 8),
                Text(
                  'LimitedCart',
                  style: TextStyle(
                    color: Color(0xFF111827),
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => context.go('/products'),
          child: const Text('Products'),
        ),
        const SizedBox(width: 16),
        if (authProvider.isAuthenticated)
          TextButton(
            onPressed: () {
              authProvider.logout();
              context.go('/login');
            },
            child: const Text('Logout'),
          )
        else ...[
          TextButton(
            onPressed: () => context.go('/login'),
            child: const Text('Login'),
          ),
          const SizedBox(width: 8),
          ElevatedButton(
            onPressed: () => context.go('/signup'),
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFF4F46E5),
              foregroundColor: Colors.white,
            ),
            child: const Text('Sign up'),
          ),
        ],
        const SizedBox(width: 24),
      ],
    );
  }
}
