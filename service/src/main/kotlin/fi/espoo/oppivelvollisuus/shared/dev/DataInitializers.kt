// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.shared.dev

import fi.espoo.oppivelvollisuus.AttachmentId
import fi.espoo.oppivelvollisuus.AttachmentTypeId
import fi.espoo.oppivelvollisuus.CompanyId
import fi.espoo.oppivelvollisuus.DaycareDecisionId
import fi.espoo.oppivelvollisuus.DaycareId
import fi.espoo.oppivelvollisuus.PriceCatalogueId
import fi.espoo.oppivelvollisuus.PriceCatalogueRowId
import fi.espoo.oppivelvollisuus.ProviderUserId
import fi.espoo.oppivelvollisuus.ServiceOptionId
import fi.espoo.oppivelvollisuus.domain.common.ClosedInfo
import fi.espoo.oppivelvollisuus.domain.common.ReturnedInfo
import fi.espoo.oppivelvollisuus.domain.company.CompanyStatus
import fi.espoo.oppivelvollisuus.domain.daycare.DaycareStatus
import fi.espoo.oppivelvollisuus.domain.pricecatalogue.PriceCatalogueStatus
import fi.espoo.oppivelvollisuus.shared.db.Database
import fi.espoo.oppivelvollisuus.shared.time.HelsinkiDateTime
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import java.util.UUID
import kotlin.io.bufferedReader
import kotlin.io.readText
import kotlin.io.use
import kotlin.let
import org.springframework.core.io.ClassPathResource

private val logger = KotlinLogging.logger {}

fun Database.Transaction.runDevScript(devScriptName: String) {
    val path = "dev-data/$devScriptName"
    logger.info { "Running SQL script: $path" }
    ClassPathResource(path).inputStream.use {
        it.bufferedReader().readText().let { content -> execute { sql(content) } }
    }
}

fun Database.Transaction.resetDatabase() {
    execute { sql("SELECT reset_database()") }
}

data class DevAttachmentType(
    val id: AttachmentTypeId = AttachmentTypeId(UUID.randomUUID()),
    val nameFi: String,
    val nameSv: String,
    val enabledForCompanies: Boolean = false,
    val enabledForDaycares: Boolean = false,
    val required: Boolean = false,
    val displayOrder: Int,
)

fun Database.Transaction.devInsert(attachmentType: DevAttachmentType) = execute {
    sql(
        """
    INSERT INTO attachment_type (id, name_fi, name_sv, enabled_for_companies, enabled_for_daycares, required, display_order) 
    VALUES (${bind(attachmentType.id)}, ${bind(attachmentType.nameFi)}, ${bind(attachmentType.nameSv)}, ${bind(attachmentType.enabledForCompanies)}, ${bind(attachmentType.enabledForDaycares)}, ${bind(attachmentType.required)}, ${bind(attachmentType.displayOrder)})
"""
    )
}

// Predefined attachment types
object DevAttachmentTypes {
    // Company attachment types
    val COMPANY_TRADE_REGISTER =
        DevAttachmentType(
            id = AttachmentTypeId(UUID.fromString("85ef3320-a1ff-4cab-8d7f-2231e0d042fe")),
            nameFi = "Kaupparekisteriote tai yhdistysrekisteriote (Luotettava Kumppani)",
            nameSv =
                "Utdrag ur handelsregistret eller föreningsregistret (Pålitlig Partner / Luotettava Kumppani)",
            enabledForCompanies = true,
            required = true,
            displayOrder = 1,
        )
    val COMPANY_OTHER =
        DevAttachmentType(
            id = AttachmentTypeId(UUID.fromString("21ebc400-2213-43ee-90dc-db1c95661ca8")),
            nameFi = "Muut liitteet",
            nameSv = "Andra bilagor",
            enabledForCompanies = true,
            required = false,
            displayOrder = 10,
        )

    // Daycare attachment types
    val DAYCARE_BUILDING_PERMIT =
        DevAttachmentType(
            id = AttachmentTypeId(UUID.fromString("f62bd8e1-1e63-4bd5-bb7b-938032a1d58c")),
            nameFi = "Tilojen rakennuslupa",
            nameSv = "Bygglov för lokalerna",
            enabledForDaycares = true,
            required = true,
            displayOrder = 11,
        )
    val DAYCARE_OTHER =
        DevAttachmentType(
            id = AttachmentTypeId(UUID.fromString("639f57a8-91e2-4e36-a978-a506178f39f0")),
            nameFi = "Muut liitteet",
            nameSv = "Andra bilagor",
            enabledForDaycares = true,
            required = false,
            displayOrder = 16,
        )

    val all = listOf(COMPANY_TRADE_REGISTER, COMPANY_OTHER, DAYCARE_BUILDING_PERMIT, DAYCARE_OTHER)
}

fun Database.Transaction.insertDevAttachmentTypes() {
    DevAttachmentTypes.all.forEach { devInsert(it) }
}

data class DevServiceOption(
    val id: ServiceOptionId = ServiceOptionId(UUID.randomUUID()),
    val nameFi: String,
    val nameSv: String,
    val required: Boolean,
    val displayOrder: Int,
)

fun Database.Transaction.devInsert(serviceOption: DevServiceOption) = execute {
    sql(
        """
    INSERT INTO service_option (id, name_fi, name_sv, required, display_order) 
    VALUES (${bind(serviceOption.id)}, ${bind(serviceOption.nameFi)}, ${bind(serviceOption.nameSv)}, ${bind(serviceOption.required)}, ${bind(serviceOption.displayOrder)})
"""
    )
}

// Predefined service options
object DevServiceOptions {
    val OVER_20H_OVER_3Y =
        DevServiceOption(
            id = ServiceOptionId(UUID.fromString("5c47c314-cb76-4f66-bc27-720ff1e75b1a")),
            nameFi = "Yli 20 h/viikko, 3v täyttäneet lapset",
            nameSv = "Över 20 h/vecka, barn som fyllt 3 år",
            required = true,
            displayOrder = 10,
        )
    val OVER_20H_UNDER_3Y =
        DevServiceOption(
            id = ServiceOptionId(UUID.fromString("0fcb4b84-1aac-479e-be73-90ee85f3b20d")),
            nameFi = "Yli 20 h/viikko, alle 3v lapset",
            nameSv = "Över 20 h/vecka, barn under 3 år",
            required = true,
            displayOrder = 20,
        )
    val PRESCHOOL =
        DevServiceOption(
            id = ServiceOptionId(UUID.fromString("6c2b3912-38e4-4b2f-b247-faca8b5b006a")),
            nameFi = "Esiopetukseen liittyvä",
            nameSv = "I anslutning till förskoleundervisning",
            required = false,
            displayOrder = 100,
        )

    val all = listOf(OVER_20H_OVER_3Y, OVER_20H_UNDER_3Y, PRESCHOOL)
}

fun Database.Transaction.insertDevServiceOptions() {
    DevServiceOptions.all.forEach { devInsert(it) }
}

data class DevCompany(
    val id: CompanyId = CompanyId(UUID.randomUUID()),
    val providerUserId: ProviderUserId,
    val businessId: String = "1234567-8",
    val name: String = "Test Company Oy",
    val businessType: String = "Osakeyhtiö",
    val streetAddress: String = "Testikatu 1",
    val postalCode: String = "02300",
    val postOffice: String = "Espoo",
    val phone: String = "+358 50 1234567",
    val email: String = "info@testcompany.fi",
    val wwwAddress: String = "https://www.testcompany.fi",
    val contactPersonName: String = "Testi Henkilö",
    val contactPersonEmail: String = "testi.henkilo@testcompany.fi",
    val contactPersonPhone: String = "+358 40 1234567",
    val financeContactName: String = "Talous Henkilö",
    val financeContactEmail: String = "talous@testcompany.fi",
    val financeContactPhone: String = "+358 40 7654321",
    val financeStreetAddress: String = "Testikatu 1",
    val financePostalCode: String = "00100",
    val financePostOffice: String = "Helsinki",
    val financeIban: String = "FI2112345600000785",
    val financeBic: String = "NDEAFIHH",
    val status: CompanyStatus = CompanyStatus.APPLIED,
    val createdAt: HelsinkiDateTime = HelsinkiDateTime.now(),
    val modifiedAt: HelsinkiDateTime = HelsinkiDateTime.now(),
    val appliedAt: HelsinkiDateTime? = HelsinkiDateTime.now(),
    val returned: ReturnedInfo? = null,
    val verifiedAt: HelsinkiDateTime? = null,
    val acceptedAt: HelsinkiDateTime? = null,
    val rejectedAt: HelsinkiDateTime? = null,
    val closed: ClosedInfo? = null,
    val attachments: List<DevAttachmentInput> = emptyList(),
)

data class DevAttachmentInput(
    val attachmentTypeId: AttachmentTypeId,
    val attachmentId: AttachmentId = AttachmentId(UUID.randomUUID()),
    val fileName: String = "test-attachment.pdf",
)

fun Database.Transaction.devInsert(company: DevCompany) =
    execute {
            sql(
                """
    INSERT INTO company (id, created_at, business_id, name, business_type, street_address, postal_code, post_office, phone, email, www_address, contact_person_name, contact_person_email, contact_person_phone, finance_contact_name, finance_contact_email, finance_contact_phone, finance_street_address, finance_postal_code, finance_post_office, finance_iban, finance_bic, status, modified_at, applied_at, returned_at, returned_reason, verified_at, accepted_at, rejected_at, closed_at, closed_reason) 
    VALUES (${bind(company.id)}, ${bind(company.createdAt)}, ${bind(company.businessId)}, ${bind(company.name)}, ${bind(company.businessType)}, ${bind(company.streetAddress)}, ${bind(company.postalCode)}, ${bind(company.postOffice)}, ${bind(company.phone)}, ${bind(company.email)}, ${bind(company.wwwAddress)}, ${bind(company.contactPersonName)}, ${bind(company.contactPersonEmail)}, ${bind(company.contactPersonPhone)}, ${bind(company.financeContactName)}, ${bind(company.financeContactEmail)}, ${bind(company.financeContactPhone)}, ${bind(company.financeStreetAddress)}, ${bind(company.financePostalCode)}, ${bind(company.financePostOffice)}, ${bind(company.financeIban)}, ${bind(company.financeBic)}, ${bind(company.status)}, ${bind(company.modifiedAt)}, ${bind(company.appliedAt)}, ${bind(company.returned?.at)}, ${bind(company.returned?.reason)}, ${bind(company.verifiedAt)}, ${bind(company.acceptedAt)}, ${bind(company.rejectedAt)}, ${bind(company.closed?.at)}, ${bind(company.closed?.reason)})
"""
            )
        }
        .also {
            execute {
                sql(
                    """
        INSERT INTO company_acl (company_id, provider_user_id)
        VALUES (${bind(company.id)}, ${bind(company.providerUserId)})
        """
                )
            }

            // Insert attachments
            company.attachments.forEach { attachment ->
                devInsert(
                    DevAttachment(
                        id = attachment.attachmentId,
                        attachmentTypeId = attachment.attachmentTypeId,
                        companyId = company.id,
                        uploadedBy = company.providerUserId,
                        uploadedAt = company.createdAt,
                        fileName = attachment.fileName,
                    )
                )
            }
        }

data class DevAttachment(
    val id: AttachmentId = AttachmentId(UUID.randomUUID()),
    val createdAt: HelsinkiDateTime = HelsinkiDateTime.now(),
    val uploadedAt: HelsinkiDateTime = HelsinkiDateTime.now(),
    val uploadedBy: ProviderUserId? = null,
    val attachmentTypeId: AttachmentTypeId,
    val companyId: CompanyId? = null,
    val daycareId: DaycareId? = null,
    val fileName: String = "test-attachment.pdf",
)

fun Database.Transaction.devInsert(attachment: DevAttachment) = execute {
    sql(
        """
    INSERT INTO attachment (id, created_at, uploaded_at, uploaded_by, attachment_type_id, company_id, daycare_id, file_name) 
    VALUES (${bind(attachment.id)}, ${bind(attachment.createdAt)}, ${bind(attachment.uploadedAt)}, ${bind(attachment.uploadedBy)}, ${bind(attachment.attachmentTypeId)}, ${bind(attachment.companyId)}, ${bind(attachment.daycareId)}, ${bind(attachment.fileName)})
"""
    )
}

fun Database.Transaction.insertDevAttachmentsForCompany(
    companyId: CompanyId,
    uploadedBy: ProviderUserId? = null,
    uploadedAt: HelsinkiDateTime = HelsinkiDateTime.now(),
) {
    devInsert(
        DevAttachment(
            attachmentTypeId = DevAttachmentTypes.COMPANY_TRADE_REGISTER.id,
            companyId = companyId,
            uploadedBy = uploadedBy,
            uploadedAt = uploadedAt,
            fileName = "kaupparekisteriote.pdf",
        )
    )
    devInsert(
        DevAttachment(
            attachmentTypeId = DevAttachmentTypes.COMPANY_OTHER.id,
            companyId = companyId,
            uploadedBy = uploadedBy,
            uploadedAt = uploadedAt,
            fileName = "muut-liitteet.pdf",
        )
    )
}

fun Database.Transaction.insertDevAttachmentsForDaycare(
    daycareId: DaycareId,
    uploadedBy: ProviderUserId? = null,
    uploadedAt: HelsinkiDateTime = HelsinkiDateTime.now(),
) {
    devInsert(
        DevAttachment(
            attachmentTypeId = DevAttachmentTypes.DAYCARE_BUILDING_PERMIT.id,
            daycareId = daycareId,
            uploadedBy = uploadedBy,
            uploadedAt = uploadedAt,
            fileName = "rakennuslupa.pdf",
        )
    )
    devInsert(
        DevAttachment(
            attachmentTypeId = DevAttachmentTypes.DAYCARE_OTHER.id,
            daycareId = daycareId,
            uploadedBy = uploadedBy,
            uploadedAt = uploadedAt,
            fileName = "muut-liitteet.pdf",
        )
    )
}

data class DevDaycare(
    val id: DaycareId = DaycareId(UUID.randomUUID()),
    val companyId: CompanyId,
    val name: String = "Test Daycare",
    val streetAddress: String = "Päiväkotikatu 1",
    val postalCode: String = "02100",
    val postOffice: String = "Espoo",
    val phone: String = "+358 50 9876543",
    val email: String = "paivakoti@testcompany.fi",
    val wwwAddress: String = "https://www.testdaycare.fi",
    val supervisorName: String = "Johtaja Henkilö",
    val supervisorPhone: String = "+358 40 9876543",
    val supervisorEmail: String = "johtaja@testdaycare.fi",
    val status: DaycareStatus = DaycareStatus.APPLIED,
    val createdAt: HelsinkiDateTime = HelsinkiDateTime.now(),
    val modifiedAt: HelsinkiDateTime = HelsinkiDateTime.now(),
    val appliedAt: HelsinkiDateTime? = HelsinkiDateTime.now(),
    val returned: ReturnedInfo? = null,
    val verifiedAt: HelsinkiDateTime? = null,
    val decisionNumber: String? = null,
    val decidedAt: HelsinkiDateTime? = null,
    val deciderName: String? = null,
    val closed: ClosedInfo? = null,
    val attachments: List<DevAttachmentInput> = emptyList(),
) {
    fun withDecision(
        decisionNumber: String = "TEST-2025-001",
        decidedAt: HelsinkiDateTime = HelsinkiDateTime.now(),
        deciderName: String = "Päättäjä Henkilö",
    ): DevDaycare =
        copy(decisionNumber = decisionNumber, decidedAt = decidedAt, deciderName = deciderName)
}

fun Database.Transaction.devInsert(daycare: DevDaycare) {
    val decisionId =
        if (
            daycare.decisionNumber != null &&
                daycare.decidedAt != null &&
                daycare.deciderName != null
        ) {
            createUpdate {
                    sql(
                        """
                INSERT INTO daycare_decision (decision_number, decided_at, decider_name)
                VALUES (${bind(daycare.decisionNumber)}, ${bind(daycare.decidedAt)}, ${bind(daycare.deciderName)})
                RETURNING id
                """
                    )
                }
                .executeAndReturnGeneratedKeys()
                .exactlyOne<DaycareDecisionId>()
        } else {
            null
        }

    execute {
        sql(
            """
            INSERT INTO daycare (id, created_at, company_id, name, street_address, postal_code, post_office, phone, email, www_address, supervisor_name, supervisor_phone, supervisor_email, status, modified_at, applied_at, returned_at, returned_reason, verified_at, decision_id, closed_at, closed_reason) 
            VALUES (${bind(daycare.id)}, ${bind(daycare.createdAt)}, ${bind(daycare.companyId)}, ${bind(daycare.name)}, ${bind(daycare.streetAddress)}, ${bind(daycare.postalCode)}, ${bind(daycare.postOffice)}, ${bind(daycare.phone)}, ${bind(daycare.email)}, ${bind(daycare.wwwAddress)}, ${bind(daycare.supervisorName)}, ${bind(daycare.supervisorPhone)}, ${bind(daycare.supervisorEmail)}, ${bind(daycare.status)}, ${bind(daycare.modifiedAt)}, ${bind(daycare.appliedAt)}, ${bind(daycare.returned?.at)}, ${bind(daycare.returned?.reason)}, ${bind(daycare.verifiedAt)}, ${bind(decisionId)}, ${bind(daycare.closed?.at)}, ${bind(daycare.closed?.reason)})
            """
        )
    }

    // Insert attachments
    daycare.attachments.forEach { attachment ->
        devInsert(
            DevAttachment(
                id = attachment.attachmentId,
                attachmentTypeId = attachment.attachmentTypeId,
                daycareId = daycare.id,
                uploadedAt = daycare.createdAt,
                fileName = attachment.fileName,
            )
        )
    }
}

data class DevPriceCatalogue(
    val id: PriceCatalogueId = PriceCatalogueId(UUID.randomUUID()),
    val daycareId: DaycareId,
    val validFrom: LocalDate = LocalDate.now(),
    val status: PriceCatalogueStatus = PriceCatalogueStatus.APPLIED,
    val createdAt: HelsinkiDateTime = HelsinkiDateTime.now(),
    val modifiedAt: HelsinkiDateTime = HelsinkiDateTime.now(),
    val appliedAt: HelsinkiDateTime? = HelsinkiDateTime.now(),
    val returned: ReturnedInfo? = null,
    val verifiedAt: HelsinkiDateTime? = null,
    val acceptedAt: HelsinkiDateTime? = null,
    val rows: List<DevPriceCatalogueRow> = emptyList(),
) {
    fun withDefaultRows(): DevPriceCatalogue =
        copy(
            rows =
                listOf(
                    DevPriceCatalogueRow(
                        priceCatalogueId = id,
                        serviceOptionId = DevServiceOptions.OVER_20H_OVER_3Y.id,
                        priceCents = 75999,
                    ),
                    DevPriceCatalogueRow(
                        priceCatalogueId = id,
                        serviceOptionId = DevServiceOptions.OVER_20H_UNDER_3Y.id,
                        priceCents = 98700,
                    ),
                    DevPriceCatalogueRow(
                        priceCatalogueId = id,
                        serviceOptionId = DevServiceOptions.PRESCHOOL.id,
                        priceCents = null,
                    ),
                )
        )
}

fun Database.Transaction.devInsert(priceCatalogue: DevPriceCatalogue) =
    execute {
            sql(
                """
    INSERT INTO price_catalogue (id, created_at, daycare_id, valid_from, status, modified_at, applied_at, returned_at, returned_reason, verified_at, accepted_at) 
    VALUES (${bind(priceCatalogue.id)}, ${bind(priceCatalogue.createdAt)}, ${bind(priceCatalogue.daycareId)}, ${bind(priceCatalogue.validFrom)}, ${bind(priceCatalogue.status)}, ${bind(priceCatalogue.modifiedAt)}, ${bind(priceCatalogue.appliedAt)}, ${bind(priceCatalogue.returned?.at)}, ${bind(priceCatalogue.returned?.reason)}, ${bind(priceCatalogue.verifiedAt)}, ${bind(priceCatalogue.acceptedAt)})
"""
            )
        }
        .also {
            priceCatalogue.rows.forEach { row ->
                devInsert(row.copy(priceCatalogueId = priceCatalogue.id))
            }
        }

data class DevPriceCatalogueRow(
    val id: PriceCatalogueRowId = PriceCatalogueRowId(UUID.randomUUID()),
    val priceCatalogueId: PriceCatalogueId,
    val serviceOptionId: ServiceOptionId,
    val priceCents: Int?, // null means "not offered"
)

fun Database.Transaction.devInsert(priceCatalogueRow: DevPriceCatalogueRow) = execute {
    sql(
        """
    INSERT INTO price_catalogue_row (id, price_catalogue_id, service_option_id, price_cents) 
    VALUES (${bind(priceCatalogueRow.id)}, ${bind(priceCatalogueRow.priceCatalogueId)}, ${bind(priceCatalogueRow.serviceOptionId)}, ${bind(priceCatalogueRow.priceCents)})
"""
    )
}
