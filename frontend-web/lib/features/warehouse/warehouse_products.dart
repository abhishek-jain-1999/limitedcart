import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../products/product_provider.dart';
import 'warehouse_layout.dart';

class WarehouseProducts extends StatefulWidget {
  const WarehouseProducts({super.key});

  @override
  State<WarehouseProducts> createState() => _WarehouseProductsState();
}

class _WarehouseProductsState extends State<WarehouseProducts> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<ProductProvider>().fetchProducts();
    });
  }

  @override
  Widget build(BuildContext context) {
    return WarehouseLayout(
      child: Consumer<ProductProvider>(
        builder: (context, provider, _) {
          if (provider.isLoading) return const Center(child: CircularProgressIndicator());

          return Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Products (Read-Only)', style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
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
                        DataColumn(label: Text('Stock')),
                      ],
                      rows: provider.products.map((product) {
                        return DataRow(cells: [
                          DataCell(Text(product.name)),
                          DataCell(Text(product.description)),
                          DataCell(Text('\$${product.price.toStringAsFixed(2)}')),
                          DataCell(Text(product.maxQuantityPerSale.toString())),
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
