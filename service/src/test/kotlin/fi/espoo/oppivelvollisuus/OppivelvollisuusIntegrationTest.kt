// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus

import fi.espoo.oppivelvollisuus.domain.attachment.AttachmentControllerEspoo
import fi.espoo.oppivelvollisuus.domain.attachment.AttachmentControllerProvider
import fi.espoo.oppivelvollisuus.domain.company.CompanyControllerEspoo
import fi.espoo.oppivelvollisuus.domain.company.CompanyControllerProviders
import fi.espoo.oppivelvollisuus.domain.company.CompanyCreateRequest
import fi.espoo.oppivelvollisuus.domain.company.CompanyUpdateRequest
import fi.espoo.oppivelvollisuus.domain.daycare.DaycareControllerEspoo
import fi.espoo.oppivelvollisuus.domain.daycare.DaycareControllerProvider
import fi.espoo.oppivelvollisuus.domain.daycare.DaycareCreateRequest
import fi.espoo.oppivelvollisuus.domain.daycare.DaycareDecisionRequest
import fi.espoo.oppivelvollisuus.domain.daycare.DaycareUpdateRequest
import fi.espoo.oppivelvollisuus.domain.pricecatalogue.PriceCatalogueControllerEspoo
import fi.espoo.oppivelvollisuus.domain.pricecatalogue.PriceCatalogueControllerProvider
import fi.espoo.oppivelvollisuus.domain.pricecatalogue.PriceCatalogueUpsertRequest
import fi.espoo.oppivelvollisuus.shared.auth.AdUser
import fi.espoo.oppivelvollisuus.shared.auth.AuthenticatedUser
import fi.espoo.oppivelvollisuus.shared.auth.SfiUser
import fi.espoo.oppivelvollisuus.shared.time.MockAppClock
import java.time.LocalDate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.mock.web.MockMultipartFile

abstract class OppivelvollisuusIntegrationTest() : FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired private lateinit var systemController: SystemController
    @Autowired private lateinit var companyControllerProviders: CompanyControllerProviders
    @Autowired private lateinit var companyControllerEspoo: CompanyControllerEspoo
    @Autowired private lateinit var daycareControllerProvider: DaycareControllerProvider
    @Autowired private lateinit var daycareControllerEspoo: DaycareControllerEspoo
    @Autowired
    private lateinit var priceCatalogueControllerProvider: PriceCatalogueControllerProvider
    @Autowired private lateinit var priceCatalogueControllerEspoo: PriceCatalogueControllerEspoo
    @Autowired private lateinit var attachmentControllerProvider: AttachmentControllerProvider
    @Autowired private lateinit var attachmentControllerEspoo: AttachmentControllerEspoo

    protected val mockClock = MockAppClock(2025, 3, 5, 12, 0, 0)

    protected val attachmentResource = ClassPathResource("attachments-fixtures/test-attachment.png")

    // Provider user wrapper functions
    protected fun loginProviderUser(): AuthenticatedUser.ProviderUser =
        systemController
            .providerUserLogin(
                db = dbInstance(),
                user = AuthenticatedUser.SystemInternalUser,
                clock = mockClock,
                sfiUser =
                    SfiUser(
                        socialSecurityNumber = "110580-888D",
                        firstName = "Matti",
                        lastName = "Meikäläinen",
                    ),
            )
            .let { AuthenticatedUser.ProviderUser(it.id) }

    protected fun loginSecondProviderUser(): AuthenticatedUser.ProviderUser =
        systemController
            .providerUserLogin(
                db = dbInstance(),
                user = AuthenticatedUser.SystemInternalUser,
                clock = mockClock,
                sfiUser =
                    SfiUser(
                        socialSecurityNumber = "220590-999X",
                        firstName = "Toinen",
                        lastName = "Tarjoaja",
                    ),
            )
            .let { AuthenticatedUser.ProviderUser(it.id) }

    protected fun uploadAttachment(
        user: AuthenticatedUser.ProviderUser,
        typeId: AttachmentTypeId,
        fileName: String = "test-attachment.png",
    ) =
        attachmentControllerProvider.uploadAttachment(
            dbInstance(),
            user,
            mockClock,
            typeId,
            MockMultipartFile("file", fileName, "image/png", attachmentResource.inputStream),
        )

    protected fun createCompany(user: AuthenticatedUser.ProviderUser, body: CompanyCreateRequest) =
        companyControllerProviders.createCompany(dbInstance(), user, mockClock, body)

    protected fun createDaycare(
        user: AuthenticatedUser.ProviderUser,
        companyId: CompanyId,
        body: DaycareCreateRequest,
    ) = daycareControllerProvider.createDaycare(dbInstance(), user, companyId, body, mockClock)

    protected fun getCompanyAsProvider(user: AuthenticatedUser.ProviderUser, companyId: CompanyId) =
        companyControllerProviders.getCompany(dbInstance(), user, companyId)

    protected fun getOwnCompanies(user: AuthenticatedUser.ProviderUser) =
        companyControllerProviders.getOwnCompanies(dbInstance(), user)

    protected fun updateCompany(
        user: AuthenticatedUser.ProviderUser,
        companyId: CompanyId,
        body: CompanyUpdateRequest,
    ) = companyControllerProviders.updateCompany(dbInstance(), user, companyId, body, mockClock)

    protected fun deleteDraftCompany(user: AuthenticatedUser.ProviderUser, companyId: CompanyId) =
        companyControllerProviders.deleteDraftCompany(dbInstance(), user, companyId)

    protected fun getDaycareAsProvider(
        user: AuthenticatedUser.ProviderUser,
        companyId: CompanyId,
        daycareId: DaycareId,
    ) = daycareControllerProvider.getDaycare(dbInstance(), user, companyId, daycareId)

    protected fun updateDaycare(
        user: AuthenticatedUser.ProviderUser,
        companyId: CompanyId,
        daycareId: DaycareId,
        body: DaycareUpdateRequest,
    ) =
        daycareControllerProvider.updateDaycare(
            dbInstance(),
            user,
            companyId,
            daycareId,
            body,
            mockClock,
        )

    protected fun deleteDraftDaycare(
        user: AuthenticatedUser.ProviderUser,
        companyId: CompanyId,
        daycareId: DaycareId,
    ) = daycareControllerProvider.deleteDraftDaycare(dbInstance(), user, companyId, daycareId)

    protected fun updatePriceCatalogue(
        user: AuthenticatedUser.ProviderUser,
        companyId: CompanyId,
        daycareId: DaycareId,
        priceCatalogueId: PriceCatalogueId,
        body: PriceCatalogueUpsertRequest,
    ) =
        priceCatalogueControllerProvider.updatePriceCatalogue(
            dbInstance(),
            user,
            companyId,
            daycareId,
            priceCatalogueId,
            body,
            mockClock,
        )

    protected fun createPriceCatalogue(
        user: AuthenticatedUser.ProviderUser,
        companyId: CompanyId,
        daycareId: DaycareId,
        body: PriceCatalogueUpsertRequest,
    ) =
        priceCatalogueControllerProvider.createPriceCatalogue(
            dbInstance(),
            user,
            companyId,
            daycareId,
            body,
            mockClock,
        )

    // Espoo user wrapper functions
    protected fun loginEspooUser(): AuthenticatedUser.EspooUser =
        systemController
            .espooUserLogin(
                db = dbInstance(),
                user = AuthenticatedUser.SystemInternalUser,
                clock = mockClock,
                adUser =
                    AdUser(
                        externalId = "espoo-test-user",
                        firstName = "Kaisa",
                        lastName = "Käsittelijä",
                        email = "kaisa.kasittelija@espoo.fi",
                    ),
            )
            .let { AuthenticatedUser.EspooUser(it.id) }

    protected fun getAllCompanies(user: AuthenticatedUser.EspooUser) =
        companyControllerEspoo.getAllCompanies(dbInstance(), user)

    protected fun getCompany(user: AuthenticatedUser.EspooUser, companyId: CompanyId) =
        companyControllerEspoo.getCompany(dbInstance(), user, companyId)

    protected fun getDaycares(user: AuthenticatedUser.EspooUser, companyId: CompanyId) =
        daycareControllerEspoo.getDaycares(dbInstance(), user, companyId)

    protected fun getDaycare(
        user: AuthenticatedUser.EspooUser,
        companyId: CompanyId,
        daycareId: DaycareId,
    ) = daycareControllerEspoo.getDaycare(dbInstance(), user, companyId, daycareId)

    protected fun downloadAttachment(
        user: AuthenticatedUser.EspooUser,
        attachmentId: AttachmentId,
    ) = attachmentControllerEspoo.downloadAttachment(dbInstance(), user, attachmentId)

    protected fun verifyCompany(user: AuthenticatedUser.EspooUser, companyId: CompanyId) =
        companyControllerEspoo.verifyCompany(dbInstance(), user, mockClock, companyId)

    protected fun unverifyCompany(user: AuthenticatedUser.EspooUser, companyId: CompanyId) =
        companyControllerEspoo.unverifyCompany(dbInstance(), user, companyId)

    protected fun verifyDaycare(
        user: AuthenticatedUser.EspooUser,
        companyId: CompanyId,
        daycareId: DaycareId,
    ) = daycareControllerEspoo.verifyDaycare(dbInstance(), user, mockClock, companyId, daycareId)

    protected fun unverifyDaycare(
        user: AuthenticatedUser.EspooUser,
        companyId: CompanyId,
        daycareId: DaycareId,
    ) = daycareControllerEspoo.unverifyDaycare(dbInstance(), user, companyId, daycareId)

    protected fun returnDaycareForFixing(
        user: AuthenticatedUser.EspooUser,
        companyId: CompanyId,
        daycareId: DaycareId,
        reason: String,
    ) =
        daycareControllerEspoo.returnDaycareForFixing(
            dbInstance(),
            user,
            mockClock,
            companyId,
            daycareId,
            DaycareControllerEspoo.DaycareReturnRequest(reason),
        )

    protected fun verifyPriceCatalogue(
        user: AuthenticatedUser.EspooUser,
        companyId: CompanyId,
        daycareId: DaycareId,
        priceCatalogueId: PriceCatalogueId,
    ) =
        priceCatalogueControllerEspoo.verifyPriceCatalogue(
            dbInstance(),
            user,
            mockClock,
            companyId,
            daycareId,
            priceCatalogueId,
        )

    protected fun unverifyPriceCatalogue(
        user: AuthenticatedUser.EspooUser,
        companyId: CompanyId,
        daycareId: DaycareId,
        priceCatalogueId: PriceCatalogueId,
    ) =
        priceCatalogueControllerEspoo.unverifyPriceCatalogue(
            dbInstance(),
            user,
            companyId,
            daycareId,
            priceCatalogueId,
        )

    protected fun returnPriceCatalogueForFixing(
        user: AuthenticatedUser.EspooUser,
        companyId: CompanyId,
        daycareId: DaycareId,
        priceCatalogueId: PriceCatalogueId,
        reason: String,
    ) =
        priceCatalogueControllerEspoo.returnPriceCatalogueForFixing(
            dbInstance(),
            user,
            mockClock,
            companyId,
            daycareId,
            priceCatalogueId,
            PriceCatalogueControllerEspoo.PriceCatalogueReturnRequest(reason),
        )

    protected fun returnCompanyForFixing(
        user: AuthenticatedUser.EspooUser,
        companyId: CompanyId,
        reason: String,
    ) =
        companyControllerEspoo.returnCompanyForFixing(
            dbInstance(),
            user,
            mockClock,
            companyId,
            CompanyControllerEspoo.CompanyReturnRequest(reason),
        )

    protected fun acceptCompany(user: AuthenticatedUser.EspooUser, companyId: CompanyId) =
        companyControllerEspoo.acceptCompany(dbInstance(), user, mockClock, companyId)

    protected fun rejectCompany(user: AuthenticatedUser.EspooUser, companyId: CompanyId) =
        companyControllerEspoo.rejectCompany(dbInstance(), user, mockClock, companyId)

    protected fun acceptDaycare(
        user: AuthenticatedUser.EspooUser,
        companyId: CompanyId,
        daycareId: DaycareId,
        decision: DaycareDecisionRequest,
    ) =
        daycareControllerEspoo.acceptDaycare(
            dbInstance(),
            user,
            mockClock,
            companyId,
            daycareId,
            decision,
        )

    protected fun rejectDaycare(
        user: AuthenticatedUser.EspooUser,
        companyId: CompanyId,
        daycareId: DaycareId,
        decision: DaycareDecisionRequest,
    ) =
        daycareControllerEspoo.rejectDaycare(
            dbInstance(),
            user,
            mockClock,
            companyId,
            daycareId,
            decision,
        )

    protected fun acceptPriceCatalogue(
        user: AuthenticatedUser.EspooUser,
        companyId: CompanyId,
        daycareId: DaycareId,
        priceCatalogueId: PriceCatalogueId,
    ) =
        priceCatalogueControllerEspoo.acceptPriceCatalogue(
            dbInstance(),
            user,
            mockClock,
            companyId,
            daycareId,
            priceCatalogueId,
        )

    protected fun closeDaycare(
        user: AuthenticatedUser.EspooUser,
        companyId: CompanyId,
        daycareId: DaycareId,
        reason: String,
        closedAt: LocalDate = mockClock.now().toLocalDate(),
    ) =
        daycareControllerEspoo.closeDaycare(
            dbInstance(),
            user,
            mockClock,
            companyId,
            daycareId,
            DaycareControllerEspoo.DaycareCloseRequest(reason, closedAt),
        )

    protected fun closeCompany(
        user: AuthenticatedUser.EspooUser,
        companyId: CompanyId,
        reason: String,
        closedAt: LocalDate = mockClock.now().toLocalDate(),
    ) =
        companyControllerEspoo.closeCompany(
            dbInstance(),
            user,
            mockClock,
            companyId,
            CompanyControllerEspoo.CompanyCloseRequest(reason, closedAt),
        )
}
