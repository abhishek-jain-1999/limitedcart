class AdminMetrics {
  final int totalUsers;
  final int totalAdmins;
  final int totalWarehouseStaff;

  AdminMetrics({
    required this.totalUsers,
    required this.totalAdmins,
    required this.totalWarehouseStaff,
  });

  factory AdminMetrics.fromJson(Map<String, dynamic> json) {
    return AdminMetrics(
      totalUsers: json['total_users'] ?? 0,
      totalAdmins: json['total_admins'] ?? 0,
      totalWarehouseStaff: json['total_warehouse_staff'] ?? 0,
    );
  }
}

class UserListView {
  final String id;
  final String email;
  final List<String> roles;

  UserListView({
    required this.id,
    required this.email,
    required this.roles,
  });

  factory UserListView.fromJson(Map<String, dynamic> json) {
    return UserListView(
      id: json['id'],
      email: json['email'],
      roles: List<String>.from(json['roles']),
    );
  }
}
