package com.abhishek.limitedcart.auth.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

@Component
class LoggingAccessDeniedHandler : AccessDeniedHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication?.principal
        log.warn(
            "403 Access denied for method={} path={} principal={}",
            request.method,
            request.requestURI,
            principal
        )

        if (!response.isCommitted) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, accessDeniedException.message)
        }
    }
}
