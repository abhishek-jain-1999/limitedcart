import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/roles.dart';
import 'admin_provider.dart';
import 'admin_layout.dart';

class UserManagement extends StatefulWidget {
  const UserManagement({super.key});

  @override
  State<UserManagement> createState() => _UserManagementState();
}

class _UserManagementState extends State<UserManagement> {
  String? _updatingUserId;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<AdminProvider>().fetchUsers();
    });
  }

  Future<void> _updateRole(String userId, List<String> roles) async {
    setState(() => _updatingUserId = userId);
    try {
      await context.read<AdminProvider>().updateUserRoles(userId, roles);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Error: $e')));
      }
    } finally {
      if (mounted) setState(() => _updatingUserId = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    return AdminLayout(
      child: Consumer<AdminProvider>(
        builder: (context, provider, _) {
          if (provider.isLoading) return const Center(child: CircularProgressIndicator());

          return Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('User Management', style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
              const SizedBox(height: 24),
              Expanded(
                child: Container(
                  decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(8)),
                  child: SingleChildScrollView(
                    child: DataTable(
                      columns: const [
                        DataColumn(label: Text('Email')),
                        DataColumn(label: Text('Current Roles')),
                        DataColumn(label: Text('Actions')),
                      ],
                      rows: provider.users.map((user) {
                        final isUpdating = _updatingUserId == user.id;
                        return DataRow(cells: [
                          DataCell(Text(user.email)),
                          DataCell(Text(user.roles.join(', '))),
                          DataCell(Row(
                            children: [
                              TextButton(
                                onPressed: isUpdating ? null : () => _updateRole(user.id, [UserRoles.user]),
                                child: const Text('User'),
                              ),
                              TextButton(
                                onPressed: isUpdating ? null : () => _updateRole(user.id, [UserRoles.warehouse]),
                                child: const Text('Warehouse', style: TextStyle(color: Colors.blue)),
                              ),
                              TextButton(
                                onPressed: isUpdating ? null : () => _updateRole(user.id, [UserRoles.admin]),
                                child: const Text('Admin', style: TextStyle(color: Colors.purple)),
                              ),
                            ],
                          )),
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
