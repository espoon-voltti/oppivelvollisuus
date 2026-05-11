// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus

import fi.espoo.oppivelvollisuus.shared.asyncjob.AsyncJob
import fi.espoo.oppivelvollisuus.shared.asyncjob.AsyncJobRunner
import fi.espoo.oppivelvollisuus.shared.asyncjob.ScheduledJobs
import fi.espoo.oppivelvollisuus.shared.config.defaultJsonMapperBuilder
import fi.espoo.oppivelvollisuus.shared.db.Database
import fi.espoo.oppivelvollisuus.shared.dev.resetDatabase
import fi.espoo.oppivelvollisuus.shared.email.MockEmailClient
import io.opentelemetry.api.trace.Tracer
import java.net.URL
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.env.Environment
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [SharedIntegrationTestConfig::class],
)
abstract class FullApplicationTest(private val resetDbBeforeEach: Boolean) {
    @LocalServerPort protected var httpPort: Int = 0

    protected val jsonMapper: JsonMapper =
        defaultJsonMapperBuilder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()

    @Autowired private lateinit var jdbi: Jdbi

    @Autowired protected lateinit var env: Environment

    @Autowired protected lateinit var tracer: Tracer

    @Autowired protected lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    @Autowired protected lateinit var scheduledJobs: ScheduledJobs

    protected lateinit var db: Database.Connection

    protected fun dbInstance(): Database = Database(jdbi, tracer)

    protected val testAttachmentPngFile =
        this::class.java.getResource("/attachments-fixtures/test-attachment.png") as URL

    @BeforeAll
    fun beforeAll() {
        assert(httpPort > 0)
        db = Database(jdbi, tracer).connectWithManualLifecycle()
        if (!resetDbBeforeEach) {
            db.transaction { it.resetDatabase() }
        }
    }

    @BeforeEach
    fun resetBeforeTest() {
        if (resetDbBeforeEach) {
            db.transaction { it.resetDatabase() }
        }
        MockEmailClient.clear()
    }

    @AfterAll
    fun afterAll() {
        db.close()
    }
}
