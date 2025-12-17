package com.abhishek.limitedcart.auth.dto

data class UserListView(
    val id: String,
    val email: String,
    val roles: Set<String>
)

data class UpdateRolesRequest(
    val roles: Set<String>
)

data class AdminMetrics(
    val totalUsers: Long,
    val totalAdmins: Long,
    val totalWarehouseStaff: Long
)
