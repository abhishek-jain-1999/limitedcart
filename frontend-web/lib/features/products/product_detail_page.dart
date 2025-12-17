import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../../shared/navbar.dart';
import '../orders/order_provider.dart';
import '../products/product_provider.dart';
import '../products/product_model.dart';
import '../auth/auth_provider.dart';

class ProductDetailPage extends StatefulWidget {
  final String productId;

  const ProductDetailPage({super.key, required this.productId});

  @override
  State<ProductDetailPage> createState() => _ProductDetailPageState();
}

class _ProductDetailPageState extends State<ProductDetailPage> {
  Product? _product;
  bool _isLoading = true;
  int _quantity = 1;
  bool _isCreatingOrder = false;

  @override
  void initState() {
    super.initState();
    _fetchProduct();
  }

  Future<void> _fetchProduct() async {
    final product = await context.read<ProductProvider>().getProduct(widget.productId);
    if (mounted) {
      setState(() {
        _product = product;
        _isLoading = false;
      });
    }
  }

  Future<void> _handleBuyNow() async {
    final authProvider = context.read<AuthProvider>();
    if (!authProvider.isAuthenticated) {
      context.go('/login');
      return;
    }

    setState(() => _isCreatingOrder = true);
    try {
      final orderId = await context.read<OrderProvider>().createOrder(
        widget.productId,
        _quantity,
      );
      if (mounted) {
        // Show success message for flash sale reservation
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('✅ Order reserved successfully! Redirecting to order tracking...'),
            backgroundColor: Colors.green,
            duration: Duration(seconds: 2),
          ),
        );
        // Navigate to order tracking page
        await Future.delayed(const Duration(milliseconds: 500));
        context.go('/orders/$orderId');
      }
    } catch (e) {
      if (mounted) {
        // Check if it's an out-of-stock error
        final errorMessage = e.toString().contains('stock') || e.toString().contains('Out of stock')
            ? '❌ Out of Stock! This item is currently unavailable.'
            : 'Failed to create order: $e';
        
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(errorMessage),
            backgroundColor: Colors.red,
            duration: const Duration(seconds: 4),
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _isCreatingOrder = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: const Navbar(),
      backgroundColor: Colors.grey[50],
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _product == null
              ? const Center(child: Text('Product not found'))
              : Center(
                  child: Container(
                    constraints: const BoxConstraints(maxWidth: 1200),
                    padding: const EdgeInsets.all(32),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        // Image Section
                        Expanded(
                          flex: 1,
                          child: Container(
                            height: 500,
                            decoration: BoxDecoration(
                              color: Colors.grey[200],
                              borderRadius: BorderRadius.circular(16),
                            ),
                            child: const Icon(
                              Icons.image,
                              size: 100,
                              color: Colors.grey,
                            ),
                          ),
                        ),
                        const SizedBox(width: 48),
                        // Details Section
                        Expanded(
                          flex: 1,
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                _product!.name,
                                style: const TextStyle(
                                  fontSize: 36,
                                  fontWeight: FontWeight.bold,
                                  color: Color(0xFF111827),
                                ),
                              ),
                              const SizedBox(height: 16),
                              Text(
                                '\$${_product!.price.toStringAsFixed(2)}',
                                style: const TextStyle(
                                  fontSize: 24,
                                  color: Color(0xFF4F46E5),
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                              const SizedBox(height: 24),
                              Text(
                                _product!.description,
                                style: const TextStyle(
                                  fontSize: 16,
                                  color: Color(0xFF4B5563),
                                  height: 1.5,
                                ),
                              ),
                              const SizedBox(height: 32),
                              // Quantity Selector
                              Row(
                                children: [
                                  const Text(
                                    'Quantity:',
                                    style: TextStyle(
                                      fontSize: 16,
                                      fontWeight: FontWeight.w500,
                                    ),
                                  ),
                                  const SizedBox(width: 16),
                                  Container(
                                    decoration: BoxDecoration(
                                      border: Border.all(color: Colors.grey[300]!),
                                      borderRadius: BorderRadius.circular(8),
                                    ),
                                    child: Row(
                                      children: [
                                        IconButton(
                                          onPressed: _quantity > 1
                                              ? () => setState(() => _quantity--)
                                              : null,
                                          icon: const Icon(Icons.remove),
                                        ),
                                        Text(
                                          '$_quantity',
                                          style: const TextStyle(
                                            fontSize: 16,
                                            fontWeight: FontWeight.bold,
                                          ),
                                        ),
                                        IconButton(
                                          onPressed: _quantity < _product!.maxQuantityPerSale
                                              ? () => setState(() => _quantity++)
                                              : null,
                                          icon: const Icon(Icons.add),
                                        ),
                                      ],
                                    ),
                                  ),
                                  const SizedBox(width: 16),
                                  Text(
                                    _product!.inStock
                                        ? '${_product!.maxQuantityPerSale} max per order'
                                        : 'Currently out of stock',
                                    style: TextStyle(
                                      color: _product!.inStock ? Colors.grey[600] : Colors.red[400],
                                      fontSize: 14,
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 48),
                              SizedBox(
                                width: double.infinity,
                                height: 56,
                                child: ElevatedButton(
                                  onPressed: _isCreatingOrder ? null : _handleBuyNow,
                                  style: ElevatedButton.styleFrom(
                                    backgroundColor: const Color(0xFF4F46E5),
                                    foregroundColor: Colors.white,
                                    shape: RoundedRectangleBorder(
                                      borderRadius: BorderRadius.circular(8),
                                    ),
                                  ),
                                  child: _isCreatingOrder
                                      ? const Row(
                                          mainAxisAlignment: MainAxisAlignment.center,
                                          children: [
                                            SizedBox(
                                              width: 20,
                                              height: 20,
                                              child: CircularProgressIndicator(
                                                color: Colors.white,
                                                strokeWidth: 2,
                                              ),
                                            ),
                                            SizedBox(width: 12),
                                            Text(
                                              'Reserving...',
                                              style: TextStyle(
                                                fontSize: 18,
                                                fontWeight: FontWeight.bold,
                                              ),
                                            ),
                                          ],
                                        )
                                      : const Text(
                                          'Buy Now',
                                          style: TextStyle(
                                            fontSize: 18,
                                            fontWeight: FontWeight.bold,
                                          ),
                                        ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
    );
  }
}
