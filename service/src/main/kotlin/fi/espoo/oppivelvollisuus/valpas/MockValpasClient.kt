// SPDX-FileCopyrightText: 2023-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.valpas

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

class MockValpasClient : ValpasClient {
    var nextStartReturnsQueryId: String = "mock-query-id"
    val statusByQueryId: MutableMap<String, ValpasQueryStatus> = ConcurrentHashMap()
    val fileContentsByUrl: MutableMap<String, ValpasResultFile> = ConcurrentHashMap()

    override fun startQuery(): String {
        logger.info { "MockValpasClient.startQuery() -> $nextStartReturnsQueryId" }
        return nextStartReturnsQueryId
    }

    override fun getQueryStatus(queryId: String): ValpasQueryStatus {
        val status =
            statusByQueryId[queryId]
                ?: throw ValpasIntegrationException(
                    "MockValpasClient: no status configured for $queryId"
                )
        logger.info { "MockValpasClient.getQueryStatus($queryId) -> $status" }
        return status
    }

    override fun downloadResultFile(url: String): ValpasResultFile {
        val file =
            fileContentsByUrl[url]
                ?: throw ValpasIntegrationException("MockValpasClient: no file configured for $url")
        logger.info { "MockValpasClient.downloadResultFile($url) -> ${file.oppijat.size} oppijat" }
        return file
    }

    /** Test convenience: stage a complete result and the file behind it. */
    fun stageCompleteResult(queryId: String, oppijat: List<ValpasOppija>) {
        val url = "https://mock-valpas/${UUID.randomUUID()}.json"
        statusByQueryId[queryId] = ValpasQueryStatus.Complete(listOf(url))
        fileContentsByUrl[url] = ValpasResultFile(oppijat = oppijat)
    }
}
