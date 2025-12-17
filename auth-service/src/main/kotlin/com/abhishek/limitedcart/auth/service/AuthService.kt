package com.abhishek.limitedcart.auth.service

import com.abhishek.limitedcart.auth.dto.AuthTokenResponse
import com.abhishek.limitedcart.auth.dto.LoginRequest
import com.abhishek.limitedcart.auth.dto.RegisterRequest
import com.abhishek.limitedcart.auth.entity.User
import com.abhishek.limitedcart.auth.entity.UserView
import com.abhishek.limitedcart.auth.repository.UserRepository
import com.abhishek.limitedcart.common.security.JwtUtil
import com.abhishek.limitedcart.common.security.UserRole
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManager: AuthenticationManager,
    private val jwtUtil: JwtUtil
) {

    @Transactional
    fun register(request: RegisterRequest): UserView {
        if (userRepository.existsByEmail(request.email)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Email already registered")
        }

        val user = User(
            email = request.email.lowercase(),
            passwordHash = passwordEncoder.encode(request.password),
            roles = mutableSetOf(UserRole.USER.authority)
        )
        return userRepository.save(user).asReadModel()
    }

    fun login(request: LoginRequest): AuthTokenResponse {
        val authenticationToken = UsernamePasswordAuthenticationToken(request.email.lowercase(), request.password)
        authenticationManager.authenticate(authenticationToken)
        val user = userRepository.findByEmail(request.email.lowercase())
            .orElseThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials") }
        val token = jwtUtil.generateToken(user.email, requireNotNull(user.id).toString(), user.roles)
        return AuthTokenResponse(token = token, user = user.asReadModel())
    }
}
