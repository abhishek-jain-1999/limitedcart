package com.abhishek.limitedcart.common.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig(
    @Value("\${app.security.allowed-origins:*}") private val allowedOrigins: String
) {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOriginPatterns = buildOriginPatterns()
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.exposedHeaders = listOf("Authorization", "Link", "X-Total-Count")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    private fun buildOriginPatterns(): List<String> {
        val tokens = allowedOrigins.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "*" }

        if (allowedOrigins.contains("*")) {
            return listOf("*")
        }

//        if (tokens.isEmpty()) {
//            return listOf("http://localhost:*", "https://localhost:*")
//        }

        return tokens.flatMap { token ->
            if (token.contains("://")) {
                listOf(token)
            } else {
                listOf("http://$token:*", "https://$token:*")
            }
        }.distinct()
    }
}
