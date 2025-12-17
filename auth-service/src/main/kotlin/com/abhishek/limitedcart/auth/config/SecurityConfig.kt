package com.abhishek.limitedcart.auth.config

import com.abhishek.limitedcart.auth.security.CustomUserDetailsService
import com.abhishek.limitedcart.auth.security.LoggingAccessDeniedHandler
import com.abhishek.limitedcart.common.security.JwtAuthFilter
import com.abhishek.limitedcart.common.security.JwtProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties::class)
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter,
    private val customUserDetailsService: CustomUserDetailsService,
    private val loggingAccessDeniedHandler: LoggingAccessDeniedHandler
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun daoAuthenticationProvider(): DaoAuthenticationProvider =
        DaoAuthenticationProvider().apply {
            setUserDetailsService(customUserDetailsService)
            setPasswordEncoder(passwordEncoder())
        }

    @Bean
    fun authenticationManager(): AuthenticationManager =
        org.springframework.security.authentication.ProviderManager(daoAuthenticationProvider())

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authenticationProvider(daoAuthenticationProvider())
            .exceptionHandling { it.accessDeniedHandler(loggingAccessDeniedHandler) }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                it.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                it.requestMatchers("/auth/**", "/actuator/**").permitAll()
                it.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
