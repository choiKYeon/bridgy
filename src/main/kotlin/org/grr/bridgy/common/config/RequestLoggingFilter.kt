package org.grr.bridgy.common.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class RequestLoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val startTime = System.currentTimeMillis()

        filterChain.doFilter(request, response)

        val elapsed = System.currentTimeMillis() - startTime
        val clientIp = request.getHeader("X-Forwarded-For") ?: request.remoteAddr
        val method = request.method
        val uri = request.requestURI
        val query = request.queryString?.let { "?$it" } ?: ""
        val status = response.status

        val statusMark = when {
            status < 300 -> "✅"
            status < 400 -> "↩️"
            status < 500 -> "⚠️"
            else         -> "❌"
        }

        log.info("$statusMark  $method $uri$query → $status  [${elapsed}ms]  from $clientIp")
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val uri = request.requestURI
        return uri.startsWith("/swagger-ui") ||
               uri.startsWith("/v3/api-docs") ||
               uri.startsWith("/actuator")
    }
}