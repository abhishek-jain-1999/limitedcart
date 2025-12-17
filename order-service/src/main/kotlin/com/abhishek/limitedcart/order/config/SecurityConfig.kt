package com.abhishek.limitedcart.order.config

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
                it.requestMatchers(HttpMethod.POST, "/orders").hasAuthority(UserRole.USER.authority)
                it.requestMatchers(HttpMethod.GET, "/orders/**").authenticated()
                // Internal endpoints
                it.requestMatchers("/orders/*/confirm", "/orders/*/fail", "/orders/*/progress",
                    "/orders/*/payment-success", "/orders/*/payment-failure").permitAll()
                it.requestMatchers("/actuator/health").permitAll()
                it.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
