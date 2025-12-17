import 'package:flutter/material.dart';
import '../../core/api_client.dart';
import 'admin_models.dart';
import '../products/product_model.dart';

class AdminProvider extends ChangeNotifier {
  final ApiClient _apiClient;
  
  AdminMetrics? _metrics;
  List<UserListView> _users = [];
  List<Product> _products = [];
  
  bool _isLoading = false;
  String? _error;

  AdminMetrics? get metrics => _metrics;
  List<UserListView> get users => _users;
  List<Product> get products => _products;
  bool get isLoading => _isLoading;
  String? get error => _error;

  AdminProvider(this._apiClient);

  Future<void> fetchMetrics() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final response = await _apiClient.client.get('/admin/metrics');
      _metrics = AdminMetrics.fromJson(response.data);
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> fetchUsers() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final response = await _apiClient.client.get('/admin/users');
      final List<dynamic> data = response.data;
      _users = data.map((json) => UserListView.fromJson(json)).toList();
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> updateUserRoles(String userId, List<String> roles) async {
    try {
      await _apiClient.client.patch('/admin/users/$userId/roles', data: {
        'roles': roles,
      });
      await fetchUsers(); // Refresh
    } catch (e) {
      _error = e.toString();
      notifyListeners();
      rethrow;
    }
  }

  Future<void> fetchProducts() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final response = await _apiClient.client.get('/products');
      final body = response.data;
      final List<dynamic> items;
      if (body is List) {
        items = body;
      } else if (body is Map<String, dynamic> && body['items'] is List) {
        items = body['items'];
      } else {
        items = const [];
      }
      _products = items.map((json) => Product.fromJson(Map<String, dynamic>.from(json))).toList();
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> createProduct(Map<String, dynamic> productData) async {
    try {
      await _apiClient.client.post('/products', data: productData);
      await fetchProducts(); // Refresh
    } catch (e) {
      _error = e.toString();
      notifyListeners();
      rethrow;
    }
  }

  Future<void> updateProduct(String id, Map<String, dynamic> productData) async {
    try {
      await _apiClient.client.put('/products/$id', data: productData);
      await fetchProducts(); // Refresh
    } catch (e) {
      _error = e.toString();
      notifyListeners();
      rethrow;
    }
  }

  Future<void> deleteProduct(String id) async {
    try {
      await _apiClient.client.delete('/products/$id');
      await fetchProducts(); // Refresh
    } catch (e) {
      _error = e.toString();
      notifyListeners();
      rethrow;
    }
  }
}
