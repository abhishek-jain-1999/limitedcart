package com.abhishek.limitedcart.auth.entity

import com.abhishek.limitedcart.common.entity.BaseEntity
import com.abhishek.limitedcart.common.security.UserRole
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Column(nullable = false, unique = true)
    var email: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = [JoinColumn(name = "user_id")])
    @Column(name = "role_name")
    var roles: MutableSet<String> = mutableSetOf(UserRole.USER.authority)
) : BaseEntity() {

    // JPA requires a no-arg constructor for proxy creation.
    constructor() : this(
        email = "",
        passwordHash = "",
        roles = mutableSetOf(UserRole.USER.authority)
    )

    fun asReadModel(): UserView =
        UserView(
            id = this.id,
            email = email,
            roles = roles,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}

data class UserView(
    val id: UUID?,
    val email: String,
    val roles: Set<String>,
    val createdAt: java.time.LocalDateTime?,
    val updatedAt: java.time.LocalDateTime?
)
