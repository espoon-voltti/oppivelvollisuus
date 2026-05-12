// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.shared.asyncjob

import fi.espoo.oppivelvollisuus.AttachmentId
import fi.espoo.oppivelvollisuus.CompanyId
import fi.espoo.oppivelvollisuus.DaycareId
import fi.espoo.oppivelvollisuus.PriceCatalogueId
import fi.espoo.oppivelvollisuus.shared.time.HelsinkiDateTime
import java.time.Duration
import java.util.UUID
import kotlin.reflect.KClass

data class AsyncJobType<T : Any>(
    val payloadClass: KClass<T>
) {
    val name: String = payloadClass.simpleName!!

    override fun toString(): String = name

    companion object {
        fun <T : Any> ofPayload(payload: T): AsyncJobType<T> = AsyncJobType(payload.javaClass.kotlin)
    }
}

sealed interface AsyncJob {
    data class DeleteOrphanAttachment(
        val id: AttachmentId
    ) : AsyncJob

    data class RunScheduledJob(
        val job: String
    ) : AsyncJob

    data class SendEspooNotificationNewCompanyApplication(
        val companyId: CompanyId
    ) : AsyncJob

    data class SendProviderNotificationNewCompanyApplication(
        val companyId: CompanyId,
        val email: String,
    ) : AsyncJob

    data class SendEspooNotificationNewDaycareApplication(
        val companyId: CompanyId,
        val daycareId: DaycareId,
    ) : AsyncJob

    data class SendProviderNotificationNewDaycareApplication(
        val companyId: CompanyId,
        val daycareId: DaycareId,
        val email: String,
    ) : AsyncJob

    data class SendEspooNotificationDaycareVerified(
        val companyId: CompanyId,
        val daycareId: DaycareId,
    ) : AsyncJob

    data class SendProviderNotificationDaycareVerified(
        val companyId: CompanyId,
        val daycareId: DaycareId,
        val email: String,
    ) : AsyncJob

    data class SendEspooNotificationDaycareAccepted(
        val companyId: CompanyId,
        val daycareId: DaycareId,
        val decisionNumber: String,
    ) : AsyncJob

    data class SendProviderNotificationDaycareAccepted(
        val companyId: CompanyId,
        val daycareId: DaycareId,
        val email: String,
        val decisionNumber: String,
    ) : AsyncJob

    data class SendEspooNotificationDaycareRejected(
        val companyId: CompanyId,
        val daycareId: DaycareId,
        val decisionNumber: String,
    ) : AsyncJob

    data class SendProviderNotificationDaycareRejected(
        val companyId: CompanyId,
        val daycareId: DaycareId,
        val email: String,
        val decisionNumber: String,
    ) : AsyncJob

    data class SendProviderNotificationCompanyReturned(
        val companyId: CompanyId,
        val email: String,
        val returnedReason: String,
    ) : AsyncJob

    data class SendProviderNotificationDaycareReturned(
        val companyId: CompanyId,
        val daycareId: DaycareId,
        val email: String,
        val returnedReason: String,
    ) : AsyncJob

    data class SendEspooNotificationPriceCatalogueApplied(
        val companyId: CompanyId,
        val daycareId: DaycareId,
        val priceCatalogueId: PriceCatalogueId,
    ) : AsyncJob

    data class SendProviderNotificationPriceCatalogueApplied(
        val companyId: CompanyId,
        val daycareId: DaycareId,
        val priceCatalogueId: PriceCatalogueId,
        val email: String,
    ) : AsyncJob

    data class SendEspooNotificationPriceCatalogueVerified(
        val companyId: CompanyId,
        val daycareId: DaycareId,
        val priceCatalogueId: PriceCatalogueId,
    ) : AsyncJob

    data class SendProviderNotificationPriceCatalogueVerified(
        val companyId: CompanyId,
        val daycareId: DaycareId,
        val priceCatalogueId: PriceCatalogueId,
        val email: String,
    ) : AsyncJob

    data class SendEspooNotificationPriceCatalogueAccepted(
        val companyId: CompanyId,
        val daycareId: DaycareId,
        val priceCatalogueId: PriceCatalogueId,
    ) : AsyncJob

    data class SendProviderNotificationPriceCatalogueAccepted(
        val companyId: CompanyId,
        val daycareId: DaycareId,
        val priceCatalogueId: PriceCatalogueId,
        val email: String,
    ) : AsyncJob

    data class SendProviderNotificationPriceCatalogueReturned(
        val companyId: CompanyId,
        val daycareId: DaycareId,
        val priceCatalogueId: PriceCatalogueId,
        val email: String,
        val returnedReason: String,
    ) : AsyncJob

    companion object {
        val main =
            AsyncJobRunner.Pool(
                AsyncJobPool.Id(AsyncJob::class, "main"),
                AsyncJobPool.Config(concurrency = 2),
                setOf(DeleteOrphanAttachment::class, RunScheduledJob::class),
            )
        val email =
            AsyncJobRunner.Pool(
                AsyncJobPool.Id(AsyncJob::class, "email"),
                AsyncJobPool.Config(concurrency = 1),
                setOf(
                    SendEspooNotificationNewCompanyApplication::class,
                    SendProviderNotificationNewCompanyApplication::class,
                    SendEspooNotificationNewDaycareApplication::class,
                    SendProviderNotificationNewDaycareApplication::class,
                    SendEspooNotificationDaycareVerified::class,
                    SendProviderNotificationDaycareVerified::class,
                    SendEspooNotificationDaycareAccepted::class,
                    SendProviderNotificationDaycareAccepted::class,
                    SendEspooNotificationDaycareRejected::class,
                    SendProviderNotificationDaycareRejected::class,
                    SendProviderNotificationCompanyReturned::class,
                    SendProviderNotificationDaycareReturned::class,
                    SendEspooNotificationPriceCatalogueApplied::class,
                    SendProviderNotificationPriceCatalogueApplied::class,
                    SendEspooNotificationPriceCatalogueVerified::class,
                    SendProviderNotificationPriceCatalogueVerified::class,
                    SendEspooNotificationPriceCatalogueAccepted::class,
                    SendProviderNotificationPriceCatalogueAccepted::class,
                    SendProviderNotificationPriceCatalogueReturned::class,
                ),
            )
    }
}

data class JobParams<T : Any>(
    val payload: T,
    val retryCount: Int,
    val retryInterval: Duration,
    val runAt: HelsinkiDateTime,
)

data class ClaimedJobRef<T : Any>(
    val jobId: UUID,
    val jobType: AsyncJobType<T>,
    val txId: Long,
    val remainingAttempts: Int,
)

data class WorkPermit(
    val availableAt: HelsinkiDateTime
)
