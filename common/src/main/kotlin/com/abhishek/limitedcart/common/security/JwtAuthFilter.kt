package com.abhishek.limitedcart.common.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtUtil: JwtUtil
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substringAfter("Bearer ").trim()
            val username = jwtUtil.extractEmail(token)
            
//            log.warn("token :${token}, username: $username")
            
            if (!username.isNullOrBlank() && SecurityContextHolder.getContext().authentication == null) {
                if (jwtUtil.validateToken(token)) {
                    val roles = jwtUtil.extractRoles(token)
                    val userId = jwtUtil.extractUserId(token) ?: ""
                    val authorities = roles.map { org.springframework.security.core.authority.SimpleGrantedAuthority(it) }
                    
                    val userContext = UserContext(userId, username, roles)
                    
                    val authentication = UsernamePasswordAuthenticationToken(
                        userContext,
                        null,
                        authorities
                    )
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authentication
                    log.debug("Authenticated {} {} as {}", request.method, request.requestURI, username)
                } else {
                    log.warn("Invalid JWT presented for {} {} (subject={})", request.method, request.requestURI, username)
                }
            } else if (username.isNullOrBlank()) {
                log.warn("JWT missing subject for {} {}", request.method, request.requestURI)
            }
        } else if (authHeader != null && authHeader.isNotBlank()) {
            log.debug("Unsupported Authorization header (no Bearer prefix) for {} {}", request.method, request.requestURI)
        }
        filterChain.doFilter(request, response)
    }
}
