import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'warehouse_provider.dart';
import 'warehouse_layout.dart';

class InventoryManagement extends StatefulWidget {
  const InventoryManagement({super.key});

  @override
  State<InventoryManagement> createState() => _InventoryManagementState();
}

class _InventoryManagementState extends State<InventoryManagement> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<WarehouseProvider>().fetchStock();
    });
  }

  void _showRestockDialog(String productId, String? productName) {
    final quantityController = TextEditingController(text: '50');
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Restock Product'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text('Product: ${productName ?? "Unknown"}', style: const TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 16),
            TextField(controller: quantityController, decoration: const InputDecoration(labelText: 'Quantity to Add'), keyboardType: TextInputType.number),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          ElevatedButton(
            onPressed: () async {
              try {
                await context.read<WarehouseProvider>().restockProduct(productId, int.parse(quantityController.text));
                if (mounted) Navigator.pop(ctx);
              } catch (e) {
                ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Error: $e')));
              }
            },
            child: const Text('Confirm'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return WarehouseLayout(
      child: Consumer<WarehouseProvider>(
        builder: (context, provider, _) {
          if (provider.isLoading) return const Center(child: CircularProgressIndicator());

          return Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Inventory Management', style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
              const SizedBox(height: 24),
              Expanded(
                child: Container(
                  decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(8)),
                  child: SingleChildScrollView(
                    child: DataTable(
                      columns: const [
                        DataColumn(label: Text('Product ID')),
                        DataColumn(label: Text('Product Name')),
                        DataColumn(label: Text('Available')),
                        DataColumn(label: Text('Reserved')),
                        DataColumn(label: Text('Total')),
                        DataColumn(label: Text('Actions')),
                      ],
                      rows: provider.stock.map((item) {
                        return DataRow(cells: [
                          DataCell(Text(item.productId.substring(0, 8) + '...')),
                          DataCell(Text(item.productName ?? 'Unknown')),
                          DataCell(Text(item.available.toString())),
                          DataCell(Text(item.reserved.toString())),
                          DataCell(Text(item.quantity.toString(), style: const TextStyle(fontWeight: FontWeight.bold))),
                          DataCell(TextButton(
                            onPressed: () => _showRestockDialog(item.productId, item.productName),
                            child: const Text('Restock'),
                          )),
                        ]);
                      }).toList(),
                    ),
                  ),
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}
