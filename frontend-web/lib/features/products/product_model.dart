class Product {
  final String id;
  final String name;
  final String description;
  final double price;
  final int maxQuantityPerSale;
  final bool active;
  final bool inStock;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  const Product({
    required this.id,
    required this.name,
    required this.description,
    required this.price,
    required this.maxQuantityPerSale,
    required this.active,
    required this.inStock,
    this.createdAt,
    this.updatedAt,
  });

  factory Product.fromJson(Map<String, dynamic> json) {
    return Product(
      id: json['id']?.toString() ?? '',
      name: json['name'] ?? '',
      description: json['description'] ?? '',
      price: (json['price'] as num?)?.toDouble() ?? 0,
      maxQuantityPerSale: json['maxQuantityPerSale'] ?? 1,
      active: json['active'] ?? false,
      inStock: json['inStock'] ?? false,
      createdAt: json['createdAt'] is int 
          ? DateTime.fromMillisecondsSinceEpoch(json['createdAt']) 
          : (json['createdAt'] != null ? DateTime.tryParse(json['createdAt'].toString()) : null),
      updatedAt: json['updatedAt'] is int 
          ? DateTime.fromMillisecondsSinceEpoch(json['updatedAt']) 
          : (json['updatedAt'] != null ? DateTime.tryParse(json['updatedAt'].toString()) : null),
    );
  }
}
