// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.shared.config

import com.auth0.jwt.interfaces.JWTVerifier
import fi.espoo.oppivelvollisuus.shared.Tracing
import fi.espoo.oppivelvollisuus.shared.logging.MdcKey
import fi.espoo.oppivelvollisuus.shared.randomTracingId
import io.opentelemetry.api.trace.Span
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class HttpFilterConfig {
    @Bean
    fun basicMdcFilter() =
        FilterRegistrationBean(BasicMdcFilter()).apply {
            setName("basicMdcFilter")
            urlPatterns = listOf("/*")
            order = Int.MIN_VALUE
        }

    @Bean
    fun jwtTokenParser(jwtVerifier: JWTVerifier) =
        FilterRegistrationBean(JwtTokenDecoder(jwtVerifier)).apply {
            setName("jwtTokenParser")
            urlPatterns = listOf("/*")
            order = -10
        }

    @Bean
    fun jwtToAuthenticatedUser() =
        FilterRegistrationBean(RequestToAuthenticatedUser()).apply {
            setName("jwtToAuthenticatedUser")
            urlPatterns = listOf("/*")
            order = -9
        }

    @Bean
    fun httpAccessControl() =
        FilterRegistrationBean(HttpAccessControl()).apply {
            setName("httpAccessControl")
            urlPatterns = listOf("/*")
            order = -8
        }

    class BasicMdcFilter() : HttpFilter() {
        override fun doFilter(
            request: HttpServletRequest,
            response: HttpServletResponse,
            chain: FilterChain,
        ) {
            MdcKey.HTTP_METHOD.set(request.method)
            MdcKey.PATH.set(request.requestURI)
            MdcKey.QUERY_STRING.set(request.queryString ?: "")
            val (traceId, spanId) =
                request.getHeader("x-request-id")?.let { Pair(it, randomTracingId()) }
                    ?: randomTracingId().let { Pair(it, it) }
            MdcKey.TRACE_ID.set(traceId)
            Span.current().setAttribute(Tracing.oppivelvollisuusTraceId, traceId)
            MdcKey.SPAN_ID.set(spanId)
            MdcKey.REQ_IP.set(request.getHeader("x-real-ip") ?: request.remoteAddr)
            try {
                chain.doFilter(request, response)
            } finally {
                MdcKey.REQ_IP.unset()
                MdcKey.SPAN_ID.unset()
                MdcKey.TRACE_ID.unset()
                MdcKey.QUERY_STRING.unset()
                MdcKey.PATH.unset()
                MdcKey.HTTP_METHOD.unset()
            }
        }
    }
}
