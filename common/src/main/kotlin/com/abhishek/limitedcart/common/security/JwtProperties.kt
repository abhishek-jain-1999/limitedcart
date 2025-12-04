package com.abhishek.limitedcart.common.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "app.security.jwt")
data class JwtProperties @ConstructorBinding constructor(
    val secret: String,
    val issuer: String = "limitedcart",
    val expirationSeconds: Long = 3600
)
