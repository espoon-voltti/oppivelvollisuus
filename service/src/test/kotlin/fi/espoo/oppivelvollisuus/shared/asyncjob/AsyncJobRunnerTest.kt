// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.shared.asyncjob

import fi.espoo.oppivelvollisuus.PureJdbiTest
import fi.espoo.oppivelvollisuus.shared.logging.MdcKey
import fi.espoo.oppivelvollisuus.shared.time.HelsinkiDateTime
import fi.espoo.oppivelvollisuus.shared.time.RealAppClock
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AsyncJobRunnerTest : PureJdbiTest(resetDbBeforeEach = true) {
    private data class TestJob(
        val data: UUID = UUID.randomUUID()
    )

    private class LetsRollbackException : RuntimeException()

    private lateinit var asyncJobRunner: AsyncJobRunner<TestJob>

    private val currentCallback: AtomicReference<(msg: TestJob) -> Unit> = AtomicReference()

    @AfterEach
    fun afterEach() {
        asyncJobRunner.close()
        currentCallback.set(null)
    }

    @BeforeEach
    fun clean() {
        asyncJobRunner =
            AsyncJobRunner(
                TestJob::class,
                listOf(
                    AsyncJobRunner.Pool(
                        AsyncJobPool.Id(TestJob::class, "default"),
                        AsyncJobPool.Config(),
                        setOf(TestJob::class),
                    )
                ),
                jdbi,
                noopTracer,
            )
        asyncJobRunner.registerHandler { _, _, msg: TestJob -> currentCallback.get()(msg) }
    }

    @Test
    fun `planned jobs are not saved if the transaction gets rolled back`() {
        assertThrows<LetsRollbackException> {
            db.transaction { tx ->
                asyncJobRunner.plan(
                    tx,
                    listOf(TestJob(UUID.randomUUID())),
                    runAt = HelsinkiDateTime.now(),
                )
                throw LetsRollbackException()
            }
        }
        assertEquals(
            0,
            db.read { it.createQuery { sql("SELECT count(*) FROM async_job") }.exactlyOne<Int>() },
        )
    }

    @Test
    fun `logging MDC keys are set during job execution`() {
        val job = TestJob()
        val future =
            this.setAsyncJobCallback {
                assertEquals(job, it)
                val traceId = MdcKey.TRACE_ID.get()
                val spanId = MdcKey.SPAN_ID.get()
                assertNotNull(traceId)
                assertNotNull(spanId)
            }
        db.transaction { asyncJobRunner.plan(it, listOf(job), runAt = HelsinkiDateTime.now()) }
        asyncJobRunner.runPendingJobsSync(RealAppClock(), 1)
        future.get(0, TimeUnit.SECONDS)
    }

    @Test
    fun `pending jobs can be executed manually`() {
        val job = TestJob()
        val future = this.setAsyncJobCallback { assertEquals(job, it) }
        db.transaction { asyncJobRunner.plan(it, listOf(job), runAt = HelsinkiDateTime.now()) }
        asyncJobRunner.runPendingJobsSync(RealAppClock())
        future.get(0, TimeUnit.SECONDS)
    }

    @Test
    fun `pending jobs are executed in the background if AsyncJobRunner has been started`() {
        val job = TestJob()
        val future = this.setAsyncJobCallback { assertEquals(job, it) }
        db.transaction { asyncJobRunner.plan(it, listOf(job), runAt = HelsinkiDateTime.now()) }
        asyncJobRunner.startBackgroundPolling(RealAppClock(), Duration.ofSeconds(1))
        future.get(10, TimeUnit.SECONDS)
    }

    @Test
    fun `AsyncJobRunner wakes up automatically after a transaction plans jobs`() {
        val job = TestJob()
        val future = this.setAsyncJobCallback { assertEquals(job, it) }
        asyncJobRunner.enableAfterCommitHooks()
        db.transaction { asyncJobRunner.plan(it, listOf(job), runAt = HelsinkiDateTime.now()) }
        future.get(10, TimeUnit.SECONDS)
    }

    @Test
    fun `failed jobs get retried`() {
        val job = TestJob()
        val failingFuture = this.setAsyncJobCallback { throw LetsRollbackException() }
        db.transaction {
            asyncJobRunner.plan(it, listOf(job), 20, Duration.ZERO, runAt = HelsinkiDateTime.now())
        }
        assertEquals(1, asyncJobRunner.runPendingJobsSync(RealAppClock(), 1))

        val exception = assertThrows<ExecutionException> { failingFuture.get(10, TimeUnit.SECONDS) }
        assertTrue(exception.cause is LetsRollbackException)

        val future = this.setAsyncJobCallback { assertEquals(job, it) }
        assertEquals(1, asyncJobRunner.runPendingJobsSync(RealAppClock(), 1))
        future.get(10, TimeUnit.SECONDS)

        assertEquals(0, asyncJobRunner.runPendingJobsSync(RealAppClock(), 1))
    }

    private fun <R> setAsyncJobCallback(f: (msg: TestJob) -> R): Future<R> {
        val future = CompletableFuture<R>()
        currentCallback.set { msg: TestJob ->
            try {
                val completed = future.complete(f(msg))
                assertTrue(completed)
            } catch (t: Throwable) {
                future.completeExceptionally(t)
                throw t
            }
        }
        return future
    }
}
