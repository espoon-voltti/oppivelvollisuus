// SPDX-FileCopyrightText: 2023-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.valpas

import fi.espoo.oppivelvollisuus.ValpasIntegrationEnv
import fi.espoo.oppivelvollisuus.domain.findNewValpasNotificationIds
import fi.espoo.oppivelvollisuus.shared.asyncjob.AsyncJob
import fi.espoo.oppivelvollisuus.shared.asyncjob.AsyncJobRunner
import fi.espoo.oppivelvollisuus.shared.db.Database
import fi.espoo.oppivelvollisuus.shared.time.AppClock
import fi.espoo.oppivelvollisuus.shared.time.HelsinkiDateTime
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

private val MAX_POLL_DURATION: Duration = Duration.ofHours(6)
private val MAX_DOWNLOAD_DURATION: Duration = Duration.ofHours(3)

@Service
class ValpasIntegrationService(
    private val env: ValpasIntegrationEnv,
    private val client: ValpasClient,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {
    init {
        asyncJobRunner.registerHandler<AsyncJob.StartValpasImport> { db, clock, _ ->
            runStartValpasImport(db, clock)
        }
        asyncJobRunner.registerHandler<AsyncJob.ImportValpasOppija> { db, clock, msg ->
            runImportValpasOppija(db, clock, msg)
        }
    }

    fun scheduleStartValpasImport(tx: Database.Transaction, clock: AppClock) {
        if (!env.enabled) {
            logger.warn { "Valpas integration disabled — scheduleStartValpasImport is a no-op" }
            return
        }
        val now = clock.now()
        val latest = tx.getMostRecentValpasQueryRun()
        if (
            latest != null &&
                (latest.state == ValpasQueryRunState.STARTED ||
                    latest.state == ValpasQueryRunState.FILES_READY)
        ) {
            logger.warn {
                "Failing stale in-flight valpas_query_run ${latest.id} (state=${latest.state})"
            }
            tx.markValpasQueryRunFailed(latest.id, now)
        }
        asyncJobRunner.plan(
            tx,
            listOf(AsyncJob.StartValpasImport),
            retryCount = 240,
            retryInterval = Duration.ofMinutes(1),
            runAt = now,
        )
    }

    fun advanceValpasImport(db: Database.Connection, clock: AppClock) {
        if (!env.enabled) {
            logger.warn { "Valpas integration disabled — advanceValpasImport is a no-op" }
            return
        }
        val latest = db.read { tx -> tx.getMostRecentValpasQueryRun() } ?: return
        when (latest.state) {
            ValpasQueryRunState.STARTED -> advanceFromStarted(db, clock, latest)

            ValpasQueryRunState.FILES_READY -> advanceFromFilesReady(db, clock, latest)

            ValpasQueryRunState.COMPLETED,
            ValpasQueryRunState.FAILED -> Unit
        }
    }

    private fun runStartValpasImport(db: Database.Connection, clock: AppClock) {
        if (!env.enabled) return
        val queryId = client.startQuery()
        db.transaction { tx -> tx.insertValpasQueryRun(queryId, clock.now()) }
    }

    private fun checkTimeout(
        db: Database.Connection,
        latest: ValpasQueryRun,
        startedAt: HelsinkiDateTime,
        max: Duration,
        label: String,
        now: HelsinkiDateTime,
    ): Boolean {
        if (Duration.between(startedAt.toInstant(), now.toInstant()) <= max) return false
        logger.error {
            "Valpas query ${latest.externalQueryId} $label timeout exceeded — marking FAILED"
        }
        db.transaction { tx -> tx.markValpasQueryRunFailed(latest.id, now) }
        return true
    }

    private fun advanceFromStarted(
        db: Database.Connection,
        clock: AppClock,
        latest: ValpasQueryRun,
    ) {
        val now = clock.now()
        if (checkTimeout(db, latest, latest.startedPollingAt, MAX_POLL_DURATION, "polling", now)) {
            return
        }
        val status =
            try {
                client.getQueryStatus(latest.externalQueryId)
            } catch (e: ValpasIntegrationException) {
                logger.warn(e) {
                    "Transient failure polling ${latest.externalQueryId}; will retry next tick"
                }
                return
            }
        when (status) {
            ValpasQueryStatus.Pending,
            ValpasQueryStatus.Running -> {}

            is ValpasQueryStatus.Complete -> {
                db.transaction { tx ->
                    tx.markValpasQueryRunFilesReady(latest.id, status.fileUrls, clock.now())
                }
                advanceFromFilesReady(
                    db,
                    clock,
                    latest.copy(
                        state = ValpasQueryRunState.FILES_READY,
                        fileUrls = status.fileUrls,
                        startedDownloadingAt = clock.now(),
                    ),
                )
            }

            ValpasQueryStatus.Failed -> {
                logger.error { "Valpas reported failed for ${latest.externalQueryId}" }
                db.transaction { tx -> tx.markValpasQueryRunFailed(latest.id, clock.now()) }
            }
        }
    }

    private fun advanceFromFilesReady(
        db: Database.Connection,
        clock: AppClock,
        latest: ValpasQueryRun,
    ) {
        val now = clock.now()
        val startedDownloadingAt =
            requireNotNull(latest.startedDownloadingAt) {
                "FILES_READY row missing startedDownloadingAt"
            }
        if (
            checkTimeout(db, latest, startedDownloadingAt, MAX_DOWNLOAD_DURATION, "download", now)
        ) {
            return
        }
        val files =
            try {
                requireNotNull(latest.fileUrls).map { client.downloadResultFile(it) }
            } catch (e: ValpasIntegrationException) {
                logger.warn(e) { "Transient download failure; will retry next tick" }
                return
            }
        val oppijat = files.flatMap { it.oppijat }
        db.transaction { tx ->
            val incomingIds = oppijat.mapNotNull { it.aktiivinenKuntailmoitus?.id }.toSet()
            val newIds = tx.findNewValpasNotificationIds(incomingIds)
            val toImport = oppijat.filter { o ->
                val id = o.aktiivinenKuntailmoitus?.id
                id != null && id in newIds
            }
            asyncJobRunner.plan(
                tx,
                toImport.map { AsyncJob.ImportValpasOppija(it) },
                runAt = clock.now(),
            )
            tx.markValpasQueryRunCompleted(latest.id, clock.now())
        }
    }

    private fun runImportValpasOppija(
        db: Database.Connection,
        clock: AppClock,
        msg: AsyncJob.ImportValpasOppija,
    ) {
        db.transaction { tx ->
            importValpasOppija(
                tx = tx,
                oppija = msg.oppija,
                opintopolkuBaseUrl = env.opintopolkuBaseUrl,
                now = clock.now(),
            )
        }
    }
}
