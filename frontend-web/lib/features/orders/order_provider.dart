import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:dio/dio.dart';
import '../../core/api_client.dart';
import '../../core/logger.dart';
import '../../core/sse_client_web.dart';
import 'order_model.dart';
import '../auth/auth_provider.dart';

class OrderProvider extends ChangeNotifier {
  final ApiClient _apiClient;
  AuthProvider _authProvider;

  Order? _currentOrder;
  final List<OrderUpdate> _updates = [];
  StreamSubscription? _sseSubscription;
  bool _isProcessingPayment = false;

  Order? get currentOrder => _currentOrder;

  List<OrderUpdate> get updates => List.unmodifiable(_updates);

  bool get isProcessingPayment => _isProcessingPayment;

  OrderProvider(this._apiClient, this._authProvider);

  /// Update auth reference when user logs in/out
  void updateAuth(AuthProvider authProvider) {
    _authProvider = authProvider;
  }

  /// Flash Sale Pattern: Create order with async 202 response handling
  /// Returns orderId immediately after stock reservation in Redis
  Future<String> createOrder(String productId, int quantity) async {
    final headers = <String, String>{};
    final userId = _authProvider.userId;
    if (userId != null && userId.isNotEmpty) {
      headers['X-User-Id'] = userId;
    }

    try {
      final response = await _apiClient.client.post(
        '/orders',
        data: {
          'productId': productId,
          'quantity': quantity,
        },
        options: Options(
          headers: headers,
          validateStatus: (status) => status != null && status < 500, // Accept 2xx, 4xx
        ),
      );

      // Handle async flash sale response (202 Accepted)
      if (response.statusCode == 202) {
        final reservation = OrderReservation.fromJson(Map<String, dynamic>.from(response.data ?? {}));

        // Clear previous state
        _updates.clear();
        _currentOrder = null;

        notifyListeners();
        return reservation.orderId;
      }

      // Handle error responses
      if (response.statusCode == 400) {
        final errorMessage = response.data?['message'] ?? 'Failed to create order';
        throw Exception(errorMessage); // Out of stock or price unavailable
      }

      throw Exception('Unexpected response: ${response.statusCode}');
    } on DioException catch (e) {
      if (e.response?.statusCode == 400) {
        final errorMessage = e.response?.data?['message'] ?? 'Out of stock or invalid request';
        throw Exception(errorMessage);
      }
      rethrow;
    }
  }

  Future<void> fetchOrder(String orderId) async {
    try {
      AppLogger.info('fetchOrder: $orderId');
      final response = await _apiClient.client.get('/orders/$orderId');
      _currentOrder = Order.fromJson(Map<String, dynamic>.from(response.data ?? {}));
      AppLogger.info('fetchOrdered: ${response.data}');
      notifyListeners();
      _connectToSse(orderId);
    } catch (e) {
      AppLogger.error('Error fetching order $orderId',error: e);
    }
  }

  SseClient? _sseClient;
  String? _activeOrderId; // Track which order we're connected to

  void _connectToSse(String orderId) async {
    if (orderId.isEmpty) return;
    
    // If already connected to this order, don't reconnect
    if (_activeOrderId == orderId && _sseClient != null && _sseSubscription != null) {
      AppLogger.info('SSE already connected for order: $orderId, skipping reconnection');
      return;
    }
    
    // Dispose previous connection if switching orders
    _sseClient?.dispose();
    _sseSubscription?.cancel();
    _activeOrderId = orderId;

    AppLogger.info('Connecting to SSE for order: $orderId');

    try {
      // Use XMLHttpRequest SSE client for web
      _sseClient = SseClient();
      final stream = _sseClient!.connect(orderId);
      
      final Set<String> processedUpdates = {};

      // Listen to the stream
      _sseSubscription = stream.listen(
        (jsonData) {
          try {
            AppLogger.log('Received SSE event: $jsonData');
            
            final update = OrderUpdate.fromJson(jsonData);
            
            AppLogger.info('Parsed update - orderId: ${update.orderId}, status: ${update.status}');
            
            if (update.orderId != orderId) {
              AppLogger.warn('Skipping update - orderId mismatch: ${update.orderId} != $orderId');
              return;
            }

            final updateKey = '${update.orderId}-${update.status}-${update.occurredAt.millisecondsSinceEpoch}';
            if (processedUpdates.contains(updateKey)) {
              AppLogger.warn('Duplicate update received: ${update.status}');
              return;
            }

            processedUpdates.add(updateKey);

            AppLogger.logStream('Order Progress', {
              'orderId': update.orderId,
              'status': update.status,
              'occurrence': update.occurredAt.toIso8601String(),
              'message': update.message,
            });

            final exists = _updates.any((u) => 
              u.status == update.status && 
              u.occurredAt.difference(update.occurredAt).inSeconds.abs() < 2
            );

            if (!exists) {
              _updates.insert(0, update);
              if (_currentOrder != null) {
                _currentOrder = _currentOrder!.copyWith(status: update.status);
              }
              AppLogger.success('Calling notifyListeners() with ${_updates.length} updates');
              notifyListeners();
              AppLogger.success('Order status updated: ${update.status}');
            } else {
              AppLogger.warn('Update already exists in list: ${update.status}');
            }
          } catch (e, stackTrace) {
            AppLogger.error('Error processing SSE event', error: e);
            AppLogger.log('Stack trace: $stackTrace');
          }
        },
        onError: (error) {
          AppLogger.error('SSE stream error', error: error);
        },
        onDone: () {
          AppLogger.warn('SSE stream closed');
        },
        cancelOnError: false,
      );
    } catch (e, stackTrace) {
      AppLogger.error('SSE connection error', error: e, force: true);
      AppLogger.log('Stack trace: $stackTrace');
    }
  }

  Future<String?> initiatePayment(String orderId) async {
    if (orderId.isEmpty) return null;
    if (_currentOrder == null || _currentOrder!.id != orderId) {
      await fetchOrder(orderId);
    }

    final amount = _currentOrder?.amount ?? 0;
    final response = await _apiClient.client.post('/payments/initiate', data: {
      'orderId': orderId,
      'amount': amount,
      'currency': 'USD',
    });

    final data = Map<String, dynamic>.from(response.data ?? {});
    final paymentLink = data['paymentLink']?.toString();
    return paymentLink;
  }

  Future<void> processPayment({
    required String token,
    required String cardNumber,
    required String expiryMonth,
    required String expiryYear,
    required String cvc,
    required String cardHolderName,
  }) async {
    _isProcessingPayment = true;
    notifyListeners();
    try {
      await _apiClient.client.post('/payments/process', data: {
        'token': token,
        'cardDetails': {
          'cardNumber': cardNumber,
          'expiryMonth': expiryMonth,
          'expiryYear': expiryYear,
          'cvc': cvc,
          'cardHolderName': cardHolderName,
        },
      });
    } finally {
      _isProcessingPayment = false;
      notifyListeners();
    }
  }

  /// Cancel an order
  Future<void> cancelOrder(String orderId) async {
    if (orderId.isEmpty) return;
    
    try {
      AppLogger.info('Cancelling order: $orderId');
      await _apiClient.client.post('/orders/$orderId/cancel');
      AppLogger.success('Order cancelled successfully');
      
      // Refresh order status
      await fetchOrder(orderId);
    } catch (e) {
      AppLogger.error('Failed to cancel order', error: e);
      rethrow;
    }
  }

  @override
  void dispose() {
    _sseSubscription?.cancel();
    _sseClient?.dispose();
    _activeOrderId = null;
    super.dispose();
  }
}
