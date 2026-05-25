// SPDX-FileCopyrightText: 2023-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.valpas

import fi.espoo.oppivelvollisuus.ValpasQueryRunId
import fi.espoo.oppivelvollisuus.shared.db.Database
import fi.espoo.oppivelvollisuus.shared.db.DatabaseEnum
import fi.espoo.oppivelvollisuus.shared.time.HelsinkiDateTime

enum class ValpasQueryRunState : DatabaseEnum {
    STARTED,
    FILES_READY,
    COMPLETED,
    FAILED;

    override val sqlType: String = "valpas_query_run_state"
}

data class ValpasQueryRun(
    val id: ValpasQueryRunId,
    val externalQueryId: String,
    val state: ValpasQueryRunState,
    val startedPollingAt: HelsinkiDateTime,
    val startedDownloadingAt: HelsinkiDateTime?,
    val finishedAt: HelsinkiDateTime?,
    val fileUrls: List<String>?,
)

fun Database.Read.getMostRecentValpasQueryRun(): ValpasQueryRun? =
    createQuery {
            sql(
                """
                SELECT id, external_query_id, state, started_polling_at,
                       started_downloading_at, finished_at, file_urls
                FROM valpas_query_runs
                ORDER BY started_polling_at DESC, id DESC
                LIMIT 1
                """
            )
        }
        .exactlyOneOrNull<ValpasQueryRun>()

fun Database.Transaction.insertValpasQueryRun(
    externalQueryId: String,
    now: HelsinkiDateTime,
): ValpasQueryRunId =
    createUpdate {
            sql(
                """
                INSERT INTO valpas_query_runs (
                    external_query_id, state, started_polling_at
                ) VALUES (
                    ${bind(externalQueryId)},
                    ${bind(ValpasQueryRunState.STARTED)},
                    ${bind(now)}
                )
                RETURNING id
                """
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<ValpasQueryRunId>()

fun Database.Transaction.markValpasQueryRunFilesReady(
    id: ValpasQueryRunId,
    fileUrls: List<String>,
    now: HelsinkiDateTime,
) {
    createUpdate {
            sql(
                """
                UPDATE valpas_query_runs
                SET state = ${bind(ValpasQueryRunState.FILES_READY)},
                    file_urls = ${bind(fileUrls.toTypedArray())},
                    started_downloading_at = ${bind(now)}
                WHERE id = ${bind(id)}
                  AND state = ${bind(ValpasQueryRunState.STARTED)}
                """
            )
        }
        .updateExactlyOne()
}

fun Database.Transaction.markValpasQueryRunCompleted(id: ValpasQueryRunId, now: HelsinkiDateTime) {
    createUpdate {
            sql(
                """
                UPDATE valpas_query_runs
                SET state = ${bind(ValpasQueryRunState.COMPLETED)},
                    finished_at = ${bind(now)}
                WHERE id = ${bind(id)}
                  AND state = ${bind(ValpasQueryRunState.FILES_READY)}
                """
            )
        }
        .updateExactlyOne()
}

fun Database.Transaction.markValpasQueryRunFailed(id: ValpasQueryRunId, now: HelsinkiDateTime) {
    createUpdate {
            sql(
                """
                UPDATE valpas_query_runs
                SET state = ${bind(ValpasQueryRunState.FAILED)},
                    finished_at = ${bind(now)}
                WHERE id = ${bind(id)}
                  AND state IN (
                      ${bind(ValpasQueryRunState.STARTED)},
                      ${bind(ValpasQueryRunState.FILES_READY)}
                  )
                """
            )
        }
        .updateExactlyOne()
}
