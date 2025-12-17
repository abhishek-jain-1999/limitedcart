package com.abhishek.limitedcart.product.config

import com.abhishek.limitedcart.common.security.JwtAuthFilter
import com.abhishek.limitedcart.common.security.JwtProperties
import com.abhishek.limitedcart.common.security.UserRole
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
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                it.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                it.requestMatchers("/actuator/health").permitAll()
                it.requestMatchers("/actuator/env").permitAll()
                it.requestMatchers(HttpMethod.GET, "/products/**", "/api/products/**").permitAll()
                it.requestMatchers(HttpMethod.POST, "/products", "/api/products")
                    .hasAnyAuthority(UserRole.ADMIN.authority, UserRole.WAREHOUSE.authority)
                it.requestMatchers(HttpMethod.PUT, "/products/**", "/api/products/**")
                    .hasAnyAuthority(UserRole.ADMIN.authority, UserRole.WAREHOUSE.authority)
                it.requestMatchers(HttpMethod.DELETE, "/products/**", "/api/products/**")
                    .hasAnyAuthority(UserRole.ADMIN.authority, UserRole.WAREHOUSE.authority)
                it.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
