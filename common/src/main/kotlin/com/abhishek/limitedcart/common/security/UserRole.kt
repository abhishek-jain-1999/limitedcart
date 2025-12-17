package com.abhishek.limitedcart.common.security

/**
 * Centralised definition for all application roles.
 *
 * [authority] returns the full Spring Security authority string (e.g. ROLE_ADMIN)
 * while [name] (default enum name) is suitable for hasRole/hasAnyRole expressions.
 */
enum class UserRole {
    USER,
    WAREHOUSE,
    ADMIN;

    val authority: String = "ROLE_$name"

    companion object {
        const val HAS_ADMIN = "hasAuthority('ROLE_ADMIN')"
        const val HAS_WAREHOUSE = "hasAuthority('ROLE_WAREHOUSE')"
        const val HAS_USER = "hasAuthority('ROLE_USER')"

        const val ADMIN_OR_WAREHOUSE = "$HAS_ADMIN or $HAS_WAREHOUSE"
    }
}
