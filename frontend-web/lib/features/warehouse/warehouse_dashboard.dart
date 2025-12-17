import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'warehouse_provider.dart';
import 'warehouse_layout.dart';

class WarehouseDashboard extends StatefulWidget {
  const WarehouseDashboard({super.key});

  @override
  State<WarehouseDashboard> createState() => _WarehouseDashboardState();
}

class _WarehouseDashboardState extends State<WarehouseDashboard> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<WarehouseProvider>().fetchSummary();
    });
  }

  @override
  Widget build(BuildContext context) {
    return WarehouseLayout(
      child: Consumer<WarehouseProvider>(
        builder: (context, provider, _) {
          if (provider.isLoading) return const Center(child: CircularProgressIndicator());
          final summary = provider.summary;
          if (summary == null) return const SizedBox();

          return Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Warehouse Dashboard', style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
              const SizedBox(height: 24),
              Row(
                children: [
                  Expanded(child: _StatCard(label: 'Total Products', value: summary.totalProducts.toString(), color: Colors.blue[50]!)),
                  const SizedBox(width: 24),
                  Expanded(child: _StatCard(label: 'Total Quantity', value: summary.totalQuantity.toString(), color: Colors.green[50]!)),
                  const SizedBox(width: 24),
                  Expanded(child: _StatCard(label: 'Reserved Items', value: summary.totalReserved.toString(), color: Colors.yellow[50]!)),
                  const SizedBox(width: 24),
                  Expanded(child: _StatCard(label: 'Low Stock Alerts', value: summary.lowStockCount.toString(), color: Colors.red[50]!)),
                ],
              ),
              if (summary.lowStockCount > 0) ...[
                const SizedBox(height: 24),
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(color: Colors.yellow[50], borderRadius: BorderRadius.circular(8), border: Border.all(color: Colors.yellow[200]!)),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('⚠️ Low Stock Warning', style: TextStyle(fontWeight: FontWeight.w600, color: Color(0xFF78350F))),
                      const SizedBox(height: 8),
                      Text('${summary.lowStockCount} product(s) are below ${summary.lowStockThreshold} units. Please restock soon.', style: const TextStyle(fontSize: 14, color: Color(0xFF92400E))),
                    ],
                  ),
                ),
              ],
            ],
          );
        },
      ),
    );
  }
}

class _StatCard extends StatelessWidget {
  final String label;
  final String value;
  final Color color;

  const _StatCard({required this.label, required this.value, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(8)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500, color: Color(0xFF6B7280))),
          const SizedBox(height: 8),
          Text(value, style: const TextStyle(fontSize: 30, fontWeight: FontWeight.bold, color: Color(0xFF111827))),
        ],
      ),
    );
  }
}
