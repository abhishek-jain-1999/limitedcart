package com.abhishek.limitedcart.auth.controller

import com.abhishek.limitedcart.auth.dto.AuthTokenResponse
import com.abhishek.limitedcart.auth.dto.LoginRequest
import com.abhishek.limitedcart.auth.dto.RegisterRequest
import com.abhishek.limitedcart.auth.entity.UserView
import com.abhishek.limitedcart.auth.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<UserView> {
        val created = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthTokenResponse> =
        ResponseEntity.ok(authService.login(request))
}

