package com.abhishek.limitedcart.common.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date

@Component
class JwtUtil(
    private val properties: JwtProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val signingKey = Keys.hmacShaKeyFor(properties.secret.toByteArray(StandardCharsets.UTF_8))

    fun generateToken(email: String, userId: String, roles: Collection<String>): String {
        val now = Date()
        val expiry = Date(now.time + properties.expirationSeconds * 1000)

        return Jwts.builder()
            .setSubject(email)
            .setIssuer(properties.issuer)
            .claim("roles", roles)
            .claim("userId", userId)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact()
    }

    fun validateToken(token: String): Boolean =
        runCatching { parseClaims(token); true }.getOrDefault(false)

    fun extractEmail(token: String): String? =
        runCatching { parseClaims(token).subject }.onFailure { log.warn("Error in extractEmail ", it) }.getOrNull()

    fun extractUserId(token: String): String? =
        runCatching { parseClaims(token)["userId"] as? String }.getOrNull()

    fun extractRoles(token: String): List<String> =
        runCatching {
            val claim = parseClaims(token)["roles"]
            when (claim) {
                is Collection<*> -> claim.filterIsInstance<String>()
                else -> emptyList()
            }
        }.getOrDefault(emptyList())

    private fun parseClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(signingKey)
            .requireIssuer(properties.issuer)
            .build()
            .parseSignedClaims(token)
            .payload
}
