// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.config

import com.zaxxer.hikari.HikariDataSource
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.core.mapper.ColumnMappers
import org.jdbi.v3.jackson3.Jackson3Config
import org.jdbi.v3.jackson3.Jackson3Plugin
import org.jdbi.v3.postgres.PostgresPlugin
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.json.JsonMapper

@Configuration
class DbConfig {
    @Bean
    fun jdbi(
        dataSource: HikariDataSource,
        jsonMapper: JsonMapper
    ) = configureJdbi(Jdbi.create(dataSource), jsonMapper)
}

private fun configureJdbi(
    jdbi: Jdbi,
    jsonMapper: JsonMapper
): Jdbi {
    jdbi
        .installPlugin(KotlinPlugin())
        .installPlugin(PostgresPlugin())
        .installPlugin(Jackson3Plugin())
    jdbi.getConfig(ColumnMappers::class.java).coalesceNullPrimitivesToDefaults = false
    jdbi.getConfig(Jackson3Config::class.java).mapper = jsonMapper
    return jdbi
}
