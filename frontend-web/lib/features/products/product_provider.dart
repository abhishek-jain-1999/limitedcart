import 'package:flutter/material.dart';
import '../../core/api_client.dart';
import 'product_model.dart';

class ProductProvider extends ChangeNotifier {
  final ApiClient _apiClient;
  List<Product> _products = [];
  bool _isLoading = false;
  String? _error;
  bool _hasLoaded = false;

  List<Product> get products => _products;
  bool get isLoading => _isLoading;
  String? get error => _error;
  bool get hasLoaded => _hasLoaded;
  bool get hasProducts => _products.isNotEmpty;

  ProductProvider(this._apiClient);

  Future<void> fetchProducts() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final response = await _apiClient.client.get('/products');
      final data = response.data;
      final List<dynamic> items;

      if (data is List) {
        items = data;
      } else if (data is Map<String, dynamic> && data['items'] is List) {
        items = data['items'];
      } else {
        items = const [];
      }

      _products = items.map((json) => Product.fromJson(Map<String, dynamic>.from(json))).toList();
    } catch (e) {
      _error = e.toString();
    } finally {
      _hasLoaded = true;
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<Product?> getProduct(String id) async {
    try {
      final response = await _apiClient.client.get('/products/$id');
      final data = response.data;
      final productJson = data is Map<String, dynamic> && data['product'] is Map<String, dynamic>
          ? Map<String, dynamic>.from(data['product'])
          : Map<String, dynamic>.from(data ?? <String, dynamic>{});
      return Product.fromJson(productJson);
    } catch (e) {
      _error = e.toString();
      notifyListeners();
      return null;
    }
  }
}
