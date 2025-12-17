package com.abhishek.limitedcart.auth.controller

import com.abhishek.limitedcart.auth.dto.AdminMetrics
import com.abhishek.limitedcart.auth.dto.UpdateRolesRequest
import com.abhishek.limitedcart.auth.dto.UserListView
import com.abhishek.limitedcart.auth.service.AdminService
import com.abhishek.limitedcart.common.security.UserRole
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/admin")
@PreAuthorize(UserRole.HAS_ADMIN)
class AdminController(
    private val adminService: AdminService
) {

    @GetMapping("/users")
    fun getAllUsers(): ResponseEntity<List<UserListView>> {
        return ResponseEntity.ok(adminService.getAllUsers())
    }

    @PatchMapping("/users/{userId}/roles")
    fun updateUserRoles(
        @PathVariable userId: UUID,
        @RequestBody request: UpdateRolesRequest
    ): ResponseEntity<UserListView> {
        return ResponseEntity.ok(adminService.updateUserRoles(userId, request.roles))
    }

    @GetMapping("/metrics")
    fun getMetrics(): ResponseEntity<AdminMetrics> {
        return ResponseEntity.ok(adminService.getMetrics())
    }
}
