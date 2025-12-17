import 'package:flutter/material.dart';
import '../../core/api_client.dart';
import 'warehouse_models.dart';

class WarehouseProvider extends ChangeNotifier {
  final ApiClient _apiClient;
  
  InventorySummary? _summary;
  List<StockView> _stock = [];
  
  bool _isLoading = false;
  String? _error;

  InventorySummary? get summary => _summary;
  List<StockView> get stock => _stock;
  bool get isLoading => _isLoading;
  String? get error => _error;

  WarehouseProvider(this._apiClient);

  Future<void> fetchSummary() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final response = await _apiClient.client.get('/inventory/summary');
      _summary = InventorySummary.fromJson(response.data);
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> fetchStock() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final response = await _apiClient.client.get('/inventory/stock');
      final List<dynamic> data = response.data;
      _stock = data.map((json) => StockView.fromJson(json)).toList();
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> restockProduct(String productId, int quantity) async {
    try {
      await _apiClient.client.post('/inventory/restock', data: {
        'productId': productId,
        'quantity': quantity,
      });
      await fetchStock(); // Refresh
      await fetchSummary(); // Refresh summary too
    } catch (e) {
      _error = e.toString();
      notifyListeners();
      rethrow;
    }
  }
}
