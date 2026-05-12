// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.shared.config

import fi.espoo.oppivelvollisuus.AppEnv
import fi.espoo.oppivelvollisuus.DatabaseTable
import fi.espoo.oppivelvollisuus.Id
import fi.espoo.oppivelvollisuus.shared.Unauthorized
import fi.espoo.oppivelvollisuus.shared.auth.AuthenticatedUser
import fi.espoo.oppivelvollisuus.shared.db.Database
import fi.espoo.oppivelvollisuus.shared.time.AppClock
import fi.espoo.oppivelvollisuus.shared.time.HelsinkiDateTime
import fi.espoo.oppivelvollisuus.shared.time.MockAppClock
import fi.espoo.oppivelvollisuus.shared.time.RealAppClock
import io.opentelemetry.api.trace.Tracer
import jakarta.servlet.http.HttpServletRequest
import org.jdbi.v3.core.Jdbi
import org.springframework.context.annotation.Configuration
import org.springframework.core.MethodParameter
import org.springframework.core.convert.TypeDescriptor
import org.springframework.core.convert.converter.GenericConverter
import org.springframework.format.FormatterRegistry
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.ZonedDateTime
import java.util.UUID

@Configuration
class SpringMvcConfig(
    private val jdbi: Jdbi,
    private val tracer: Tracer,
    private val env: AppEnv
) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(asArgumentResolver<AuthenticatedUser.EspooUser?>(::resolveAuthenticatedUser))
        resolvers.add(
            asArgumentResolver<AuthenticatedUser.SystemInternalUser?>(::resolveAuthenticatedUser)
        )
        resolvers.add(asArgumentResolver<AuthenticatedUser?>(::resolveAuthenticatedUser))
        resolvers.add(asArgumentResolver { _, webRequest -> webRequest.getDatabaseInstance() })
        resolvers.add(asArgumentResolver { _, webRequest -> webRequest.getAppClock() })
    }

    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(convertFrom<String, Id<*>> { Id<DatabaseTable>(UUID.fromString(it)) })
    }

    override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON, MediaType.ALL)
    }

    @Deprecated("Deprecated in Spring Framework 7, but still used for compatibility")
    override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        // If the response body is a string, we want it to be converted as JSON, not directly
        // serialized as string
        converters.removeIf { it is StringHttpMessageConverter }
    }

    private fun WebRequest.getDatabaseInstance(): Database = getDatabase() ?: Database(jdbi, tracer).also(::setDatabase)

    private inline fun <reified T : AuthenticatedUser> resolveAuthenticatedUser(
        parameter: MethodParameter,
        webRequest: NativeWebRequest,
    ): T? {
        val user =
            webRequest.getNativeRequest(HttpServletRequest::class.java)?.getAuthenticatedUser()
                as? T
        if (user == null && !parameter.isOptional) {
            throw Unauthorized("Unauthorized request (${webRequest.getDescription(false)})")
        }
        return user
    }

    private fun WebRequest.getAppClock(): AppClock =
        if (!env.mockClock) {
            RealAppClock()
        } else {
            val mockTime =
                this.getHeader("OppivelvollisuusMockedTime")?.let {
                    HelsinkiDateTime.from(ZonedDateTime.parse(it))
                }
            MockAppClock(mockTime ?: HelsinkiDateTime.now())
        }
}

private inline fun <reified T> asArgumentResolver(
    crossinline f: (parameter: MethodParameter, webRequest: NativeWebRequest) -> T
): HandlerMethodArgumentResolver =
    object : HandlerMethodArgumentResolver {
        override fun supportsParameter(parameter: MethodParameter): Boolean = T::class.java.isAssignableFrom(parameter.parameterType)

        override fun resolveArgument(
            parameter: MethodParameter,
            mavContainer: ModelAndViewContainer?,
            webRequest: NativeWebRequest,
            binderFactory: WebDataBinderFactory?,
        ) = f(parameter, webRequest)
    }

private inline fun <reified I, reified O> convertFrom(crossinline f: (source: I) -> O): GenericConverter =
    object : GenericConverter {
        override fun getConvertibleTypes(): Set<GenericConverter.ConvertiblePair> =
            setOf(GenericConverter.ConvertiblePair(I::class.java, O::class.java))

        override fun convert(
            source: Any?,
            sourceType: TypeDescriptor,
            targetType: TypeDescriptor,
        ): Any? = (source as? I)?.let(f)
    }

private const val ATTR_DB = "oppivelvollisuus.database"

private fun WebRequest.getDatabase() = getAttribute(ATTR_DB, SCOPE_REQUEST) as Database?

private fun WebRequest.setDatabase(db: Database) = setAttribute(ATTR_DB, db, SCOPE_REQUEST)
