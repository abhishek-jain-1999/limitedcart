class StockView {
  final String productId;
  final String? productName;
  final int quantity;
  final int reserved;
  final int available;

  StockView({
    required this.productId,
    required this.productName,
    required this.quantity,
    required this.reserved,
    required this.available,
  });

  factory StockView.fromJson(Map<String, dynamic> json) {
    return StockView(
      productId: json['productId'],
      productName: json['productName'],
      quantity: json['quantity'],
      reserved: json['reserved'],
      available: json['available'],
    );
  }
}

class InventorySummary {
  final int totalProducts;
  final int totalQuantity;
  final int totalReserved;
  final int lowStockCount;
  final int lowStockThreshold;

  InventorySummary({
    required this.totalProducts,
    required this.totalQuantity,
    required this.totalReserved,
    required this.lowStockCount,
    required this.lowStockThreshold,
  });

  factory InventorySummary.fromJson(Map<String, dynamic> json) {
    return InventorySummary(
      totalProducts: json['totalProducts'],
      totalQuantity: json['totalQuantity'],
      totalReserved: json['totalReserved'],
      lowStockCount: json['lowStockCount'],
      lowStockThreshold: json['lowStockThreshold'],
    );
  }
}
