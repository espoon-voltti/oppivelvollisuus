package fi.espoo.oppivelvollisuus.config

import com.zaxxer.hikari.HikariDataSource
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.core.mapper.ColumnMappers
import org.jdbi.v3.postgres.PostgresPlugin
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DbConfig {
    @Bean
    fun jdbi(dataSource: HikariDataSource) = configureJdbi(Jdbi.create(dataSource))
}

private fun configureJdbi(jdbi: Jdbi): Jdbi {
    jdbi
        .installPlugin(KotlinPlugin())
        .installPlugin(PostgresPlugin())
    jdbi.getConfig(ColumnMappers::class.java).coalesceNullPrimitivesToDefaults = false
    return jdbi
}
