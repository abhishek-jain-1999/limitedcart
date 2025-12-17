package com.abhishek.limitedcart.common.security

data class UserContext(
    val userId: String,
    val email: String,
    val roles: List<String>
)
