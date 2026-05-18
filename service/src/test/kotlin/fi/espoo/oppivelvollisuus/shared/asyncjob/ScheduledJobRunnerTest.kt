// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.shared.asyncjob

import fi.espoo.oppivelvollisuus.PureJdbiTest
import fi.espoo.oppivelvollisuus.shared.time.RealAppClock
import fi.espoo.oppivelvollisuus.shared.time.europeHelsinki
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.singleOrNull
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.use
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ScheduledJobRunnerTest : PureJdbiTest(resetDbBeforeEach = true) {
    enum class TestScheduledJob {
        TestJob
    }

    private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    private val testTime = LocalTime.of(1, 0)
    private val testSchedule =
        object : JobSchedule {
            override val jobs: List<ScheduledJobDefinition> =
                listOf(
                    ScheduledJobDefinition(
                        TestScheduledJob.TestJob,
                        ScheduledJobSettings(enabled = true, schedule = JobSchedule.daily(testTime)),
                    ) { _, _ ->
                        val previous = jobExecuted.getAndSet(true)
                        assertFalse(previous)
                    }
                )
        }
    private val jobExecuted = AtomicBoolean(false)

    @BeforeEach
    fun beforeEach() {
        asyncJobRunner = AsyncJobRunner(AsyncJob::class, listOf(AsyncJob.main), jdbi, noopTracer)
        jobExecuted.set(false)
    }

    @Test
    fun `a job specified by DailySchedule is scheduled and executed correctly`() {
        ScheduledJobRunner(jdbi, noopTracer, asyncJobRunner, listOf(testSchedule), dataSource)
            .use { runner ->
                runner.scheduler.start()
                val exec =
                    runner.getScheduledExecutionsForTask(TestScheduledJob.TestJob).singleOrNull()!!
                assertEquals(exec.executionTime.atZone(europeHelsinki).toLocalTime(), testTime)

                runner.scheduler.reschedule(exec.taskInstance, Instant.EPOCH)
                runner.scheduler.triggerCheckForDueExecutions()

                val start = Instant.now()
                while (asyncJobRunner.runPendingJobsSync(RealAppClock()) == 0) {
                    Thread.sleep(100)

                    assert(Duration.between(start, Instant.now()) < Duration.ofSeconds(10))
                }
            }

        assertTrue(jobExecuted.get())
    }
}
