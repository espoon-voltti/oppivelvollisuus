// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.cfg.EnumFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

fun defaultJsonMapperBuilder(): JsonMapper.Builder =
    JsonMapper
        .builder()
        .addModules(KotlinModule.Builder().build())
        // We never want to serialize timestamps as numbers but use ISO formats instead.
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)

fun defaultJsonMapper(): JsonMapper = defaultJsonMapperBuilder().build()

@Configuration
class JacksonConfig {
    // This replaces default JsonMapper provided by Spring Boot autoconfiguration
    @Bean
    fun jsonMapper(): JsonMapper =
        defaultJsonMapperBuilder()
            .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .build()
}
