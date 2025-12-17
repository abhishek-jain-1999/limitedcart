import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'admin_provider.dart';
import 'admin_layout.dart';

class AdminProducts extends StatefulWidget {
  const AdminProducts({super.key});

  @override
  State<AdminProducts> createState() => _AdminProductsState();
}

class _AdminProductsState extends State<AdminProducts> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<AdminProvider>().fetchProducts();
    });
  }

  void _showProductDialog({required AdminProvider provider, String? productId}) {
    final isEdit = productId != null;
    final product = isEdit ? provider.products.firstWhere((p) => p.id == productId) : null;
    
    final nameController = TextEditingController(text: product?.name ?? '');
    final descController = TextEditingController(text: product?.description ?? '');
    final priceController = TextEditingController(text: product?.price.toString() ?? '0');
    final maxQtyController = TextEditingController(text: (product?.maxQuantityPerSale ?? 1).toString());
    bool isActive = product?.active ?? true;

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(isEdit ? 'Edit Product' : 'Create Product'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(controller: nameController, decoration: const InputDecoration(labelText: 'Name')),
              TextField(controller: descController, decoration: const InputDecoration(labelText: 'Description')),
              TextField(controller: priceController, decoration: const InputDecoration(labelText: 'Price'), keyboardType: TextInputType.number),
              TextField(
                controller: maxQtyController,
                decoration: const InputDecoration(labelText: 'Max Quantity Per Sale'),
                keyboardType: TextInputType.number,
              ),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: const Text('Active'),
                value: isActive,
                onChanged: (value) => setState(() {
                  isActive = value;
                }),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          ElevatedButton(
            onPressed: () async {
              final data = {
                'name': nameController.text,
                'description': descController.text,
                'price': double.parse(priceController.text),
                'maxQuantityPerSale': int.tryParse(maxQtyController.text) ?? 1,
                'active': isActive,
              };
              try {
                if (isEdit) {
                  await provider.updateProduct(productId, data);
                } else {
                  await provider.createProduct(data);
                }
                if (ctx.mounted) Navigator.pop(ctx);
              } catch (e) {
                ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Error: $e')));
              }
            },
            child: const Text('Save'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return AdminLayout(
      child: Consumer<AdminProvider>(
        builder: (context, provider, _) {
          if (provider.isLoading) return const Center(child: CircularProgressIndicator());
          
          return Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text('Product Management', style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
                  ElevatedButton.icon(
                    onPressed: () => _showProductDialog(provider: provider),
                    icon: const Icon(Icons.add),
                    label: const Text('Add Product'),
                    style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF4F46E5), foregroundColor: Colors.white),
                  ),
                ],
              ),
              const SizedBox(height: 24),
              Expanded(
                child: Container(
                  decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(8)),
                  child: SingleChildScrollView(
                    child: DataTable(
                      columns: const [
                        DataColumn(label: Text('Name')),
                        DataColumn(label: Text('Description')),
                        DataColumn(label: Text('Price')),
                        DataColumn(label: Text('Actions')),
                      ],
                      rows: provider.products.map((product) {
                        return DataRow(cells: [
                          DataCell(Text(product.name)),
                          DataCell(Text(product.description)),
                          DataCell(Text('\$${product.price.toStringAsFixed(2)}')),
                          DataCell(Row(
                            children: [
                              TextButton(
                                onPressed: () => _showProductDialog(provider: provider, productId: product.id),
                                child: const Text('Edit'),
                              ),
                              TextButton(
                                onPressed: () async {
                                  final confirm = await showDialog<bool>(
                                    context: context,
                                    builder: (ctx) => AlertDialog(
                                      title: const Text('Confirm'),
                                      content: const Text('Delete this product?'),
                                      actions: [
                                        TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
                                        ElevatedButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Delete')),
                                      ],
                                    ),
                                  );
                                  if (confirm == true) {
                                    await provider.deleteProduct(product.id);
                                  }
                                },
                                child: const Text('Delete', style: TextStyle(color: Colors.red)),
                              ),
                            ],
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
