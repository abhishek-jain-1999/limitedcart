package com.abhishek.limitedcart.auth.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class NotFoundLoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        filterChain.doFilter(request, response)
        if (response.status == HttpServletResponse.SC_NOT_FOUND) {
            log.warn("404 Not Found for method={} path={}", request.method, request.requestURI)
        }
    }
}
