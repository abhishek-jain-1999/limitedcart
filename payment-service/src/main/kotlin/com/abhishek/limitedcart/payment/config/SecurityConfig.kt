package com.abhishek.limitedcart.payment.config

import com.abhishek.limitedcart.common.security.JwtAuthFilter
import com.abhishek.limitedcart.common.security.JwtProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties::class)
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                it.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                it.requestMatchers("/actuator/health").permitAll()
                // POST /payments/initiate requires authentication
                it.requestMatchers(HttpMethod.POST, "/payments/initiate").authenticated()
                // POST /payments/process allows token-based access (no JWT required for payment page)
                it.requestMatchers(HttpMethod.POST, "/payments/process").permitAll()
                // Internal polling endpoint used by saga to track payments
                it.requestMatchers(HttpMethod.GET, "/payments/order/*").permitAll()
                // Legacy endpoints for temporal-worker (internal)
                it.requestMatchers("/payments/charge", "/payments/refund").permitAll()
                it.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
