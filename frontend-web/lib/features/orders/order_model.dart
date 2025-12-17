class Order {
  final String id;
  final String status;
  final double amount;

  const Order({
    required this.id,
    required this.status,
    required this.amount,
  });

  factory Order.fromJson(Map<String, dynamic> json) {
    return Order(
      id: json['id']?.toString() ?? '',
      status: json['status']?.toString() ?? 'UNKNOWN',
      amount: (json['amount'] as num?)?.toDouble() ?? 0,
    );
  }

  Order copyWith({String? status}) => Order(
        id: id,
        status: status ?? this.status,
        amount: amount,
      );
}

/// New model for async flash sale order reservations (HTTP 202)
class OrderReservation {
  final String orderId;
  final String status;
  final String message;

  const OrderReservation({
    required this.orderId,
    required this.status,
    required this.message,
  });

  factory OrderReservation.fromJson(Map<String, dynamic> json) {
    return OrderReservation(
      orderId: json['orderId']?.toString() ?? '',
      status: json['status']?.toString() ?? 'PENDING',
      message: json['message']?.toString() ?? 'Order processing',
    );
  }
}

class OrderUpdate {
  final String orderId;
  final String status;
  final DateTime occurredAt;
  final String? message;

  OrderUpdate({
    required this.orderId,
    required this.status,
    required this.occurredAt,
    this.message,
  });

  factory OrderUpdate.fromJson(Map<String, dynamic> json) {
    return OrderUpdate(
      orderId: json['orderId']?.toString() ?? '',
      status: json['status']?.toString() ?? 'UNKNOWN',
      occurredAt: DateTime.tryParse(json['occurredAt']?.toString() ?? '') ?? DateTime.now(),
      message: json['message'] as String?,
    );
  }
}
