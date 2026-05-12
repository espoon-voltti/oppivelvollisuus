// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus

import com.auth0.jwt.algorithms.Algorithm
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import fi.espoo.oppivelvollisuus.shared.config.JwtKeys
import fi.espoo.oppivelvollisuus.shared.config.loadPublicKeys
import fi.espoo.oppivelvollisuus.shared.db.Database
import fi.espoo.oppivelvollisuus.shared.db.configureJdbi
import fi.espoo.oppivelvollisuus.shared.dev.resetDatabase
import fi.espoo.oppivelvollisuus.shared.dev.runDevScript
import fi.espoo.oppivelvollisuus.shared.noopTracer
import org.flywaydb.core.Flyway
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension
import org.jdbi.v3.core.Jdbi
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import javax.sql.DataSource
import kotlin.also
import kotlin.apply
import kotlin.io.use
import kotlin.jvm.java
import kotlin.run

// Hides Closeable interface from Spring, which would close the shared instance otherwise
class TestDataSource(
    pool: HikariDataSource
) : DataSource by pool

private val globalLock = object {}
private var testDataSource: TestDataSource? = null

fun getTestDataSource(env: DatabaseEnv): TestDataSource =
    synchronized(globalLock) {
        testDataSource
            ?: TestDataSource(
                HikariDataSource(
                    HikariConfig().apply {
                        jdbcUrl = env.url
                        username = env.username
                        password = env.password.value
                        maximumPoolSize = env.maximumPoolSize
                        leakDetectionThreshold = env.leakDetectionThreshold
                    }
                ).also {
                    Flyway
                        .configure()
                        .apply {
                            pluginRegister
                                .getExact(PostgreSQLConfigurationExtension::class.java)
                                .isTransactionalLock = false
                        }.validateMigrationNaming(true)
                        .dataSource(
                            PGSimpleDataSource().apply {
                                setUrl(env.url)
                                user = env.username
                                password = env.password.value
                            }
                        ).load()
                        .run { migrate() }
                    Database(Jdbi.create(it), noopTracer()).connect { db ->
                        db.transaction { tx ->
                            tx.runDevScript("reset-database.sql")
                            tx.resetDatabase()
                        }
                    }
                }
            ).also { testDataSource = it }
    }

@TestConfiguration
class SharedIntegrationTestConfig(
    private val env: DatabaseEnv
) {
    @Bean fun jdbi(dataSource: DataSource) = configureJdbi(Jdbi.create(dataSource))

    @Bean fun dataSource(): DataSource = getTestDataSource(env)

    @Bean
    fun integrationTestJwtAlgorithm(): Algorithm {
        val publicKeys =
            this::class.java.getResourceAsStream("/jwks.json").use { loadPublicKeys(it) }
        return Algorithm.RSA256(JwtKeys(publicKeys))
    }
}
