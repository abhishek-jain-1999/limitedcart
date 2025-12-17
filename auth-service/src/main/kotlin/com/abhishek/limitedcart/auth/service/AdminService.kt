package com.abhishek.limitedcart.auth.service

import com.abhishek.limitedcart.auth.dto.AdminMetrics
import com.abhishek.limitedcart.auth.dto.UserListView
import com.abhishek.limitedcart.auth.repository.UserRepository
import com.abhishek.limitedcart.common.exception.ResourceNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class AdminService(
    private val userRepository: UserRepository
) {

    fun getAllUsers(): List<UserListView> {
        return userRepository.findAll().map { user ->
            UserListView(
                id = requireNotNull(user.id).toString(),
                email = user.email,
                roles = user.roles.toSet()
            )
        }
    }

    @Transactional
    fun updateUserRoles(userId: UUID, roles: Set<String>): UserListView {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        // Validate roles
        val validRoles = setOf("ROLE_USER", "ROLE_WAREHOUSE", "ROLE_ADMIN")
        if (!roles.all { it in validRoles }) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role specified")
        }
        
        user.roles.clear()
        user.roles.addAll(roles)
        userRepository.save(user)

        return UserListView(
            id = requireNotNull(user.id).toString(),
            email = user.email,
            roles = user.roles.toSet()
        )
    }

    fun getMetrics(): AdminMetrics {
        val allUsers = userRepository.findAll()
        return AdminMetrics(
            totalUsers = allUsers.size.toLong(),
            totalAdmins = allUsers.count { it.roles.contains("ROLE_ADMIN") }.toLong(),
            totalWarehouseStaff = allUsers.count { it.roles.contains("ROLE_WAREHOUSE") }.toLong()
        )
    }
}
