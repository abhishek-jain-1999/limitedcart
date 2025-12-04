package com.abhishek.limitedcart.auth.dto

import com.abhishek.limitedcart.auth.entity.UserView
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:Email
    val email: String,
    @field:NotBlank
    @field:Size(min = 8, message = "Password must contain at least 8 characters")
    val password: String
)

data class LoginRequest(
    @field:Email
    val email: String,
    @field:NotBlank
    val password: String
)

data class AuthTokenResponse(
    val token: String,
    val user: UserView
)

