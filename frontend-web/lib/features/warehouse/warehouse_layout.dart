import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../auth/auth_provider.dart';

class WarehouseLayout extends StatelessWidget {
  final Widget child;

  const WarehouseLayout({super.key, required this.child});

  @override
  Widget build(BuildContext context) {
    final authProvider = context.watch<AuthProvider>();

    return Scaffold(
      backgroundColor: Colors.grey[100],
      body: Column(
        children: [
          Container(
            color: Colors.white,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              child: SizedBox(
                height: 64,
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text('Warehouse Portal', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
                    Row(
                      children: [
                        Text(authProvider.userEmail, style: const TextStyle(fontSize: 14, color: Color(0xFF4B5563))),
                        const SizedBox(width: 16),
                        TextButton(onPressed: () {authProvider.logout(); context.go('/login');}, child: const Text('Logout')),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ),
          Expanded(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  width: 256,
                  color: Colors.white,
                  child: Padding(
                    padding: const EdgeInsets.only(top: 20, left: 8, right: 8),
                    child: Column(
                      children: [
                        _SidebarItem(label: 'Dashboard', icon: Icons.dashboard, onTap: () => context.go('/warehouse/dashboard'), isActive: GoRouter.of(context).routerDelegate.currentConfiguration.uri.path == '/warehouse/dashboard'),
                        _SidebarItem(label: 'Inventory', icon: Icons.inventory_2, onTap: () => context.go('/warehouse/inventory'), isActive: GoRouter.of(context).routerDelegate.currentConfiguration.uri.path == '/warehouse/inventory'),
                        _SidebarItem(label: 'Products', icon: Icons.category, onTap: () => context.go('/warehouse/products'), isActive: GoRouter.of(context).routerDelegate.currentConfiguration.uri.path == '/warehouse/products'),
                      ],
                    ),
                  ),
                ),
                Expanded(child: Padding(padding: const EdgeInsets.all(24), child: child)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _SidebarItem extends StatelessWidget {
  final String label;
  final IconData icon;
  final VoidCallback onTap;
  final bool isActive;

  const _SidebarItem({required this.label, required this.icon, required this.onTap, this.isActive = false});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Material(
        color: isActive ? Colors.grey[50] : Colors.transparent,
        borderRadius: BorderRadius.circular(6),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(6),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
            child: Row(
              children: [
                Icon(icon, size: 20, color: isActive ? const Color(0xFF111827) : const Color(0xFF6B7280)),
                const SizedBox(width: 12),
                Text(label, style: TextStyle(fontSize: 16, fontWeight: isActive ? FontWeight.w600 : FontWeight.normal, color: isActive ? const Color(0xFF111827) : const Color(0xFF6B7280))),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
