package com.abhishek.limitedcart.auth.bootstrap

import com.abhishek.limitedcart.auth.entity.User
import com.abhishek.limitedcart.auth.repository.UserRepository
import com.abhishek.limitedcart.common.security.UserRole
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.security.crypto.password.PasswordEncoder

@Component
class AdminBootstrapper(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${app.bootstrap.admin-email:}") private val adminEmail: String?,
    @Value("\${app.bootstrap.admin-password:}") private val adminPassword: String?
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments?) {
        if (userRepository.count() > 0L) {
            return
        }

        val email = adminEmail?.trim()
        val password = adminPassword

        if (email.isNullOrBlank() || password.isNullOrBlank()) {
            log.warn("User table empty but ADMIN bootstrap credentials not provided; skipping seed.")
            return
        }

        if (userRepository.existsByEmail(email)) {
            log.info("Admin bootstrap skipped: user {} already exists.", email)
            return
        }

        val admin = User(
            email = email.lowercase(),
            passwordHash = passwordEncoder.encode(password),
            roles = mutableSetOf(UserRole.ADMIN.authority)
        )
        userRepository.save(admin)
        log.info("Seeded initial admin user {}", email)
    }
}
