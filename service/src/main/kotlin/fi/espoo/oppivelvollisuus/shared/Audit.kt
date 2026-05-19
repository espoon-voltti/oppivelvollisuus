// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.shared

import fi.espoo.oppivelvollisuus.Id
import fi.espoo.oppivelvollisuus.shared.logging.audit
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID

sealed interface AuditId {
    val value: Any

    @JvmInline value class One(override val value: Any) : AuditId

    @JvmInline value class Many(override val value: List<Any>) : AuditId

    companion object {
        operator fun invoke(value: Id<*>): AuditId = One(value)

        operator fun invoke(value: UUID): AuditId = One(value)

        operator fun invoke(value: String): AuditId = One(value)

        operator fun invoke(value: Collection<Id<*>>): AuditId = Many(value.toList())
    }

    operator fun plus(other: AuditId): AuditId =
        when (this) {
            is One -> {
                when (other) {
                    is One -> Many(listOf(value, other.value))
                    is Many -> Many(listOf(value) + other.value)
                }
            }

            is Many -> {
                when (other) {
                    is One -> Many(value + other.value)
                    is Many -> Many(value + other.value)
                }
            }
        }
}

enum class Audit(
    private val securityEvent: Boolean = false,
    private val securityLevel: String = "low",
) {
    EspooUserLogin(securityEvent = true, securityLevel = "medium"),
    EspooUserLoginAttempt(securityEvent = true, securityLevel = "low"),
    CreateStudent,
    GetDuplicateStudents,
    SearchStudents,
    GetStudent,
    UpdateStudent,
    DeleteStudent,
    CreateStudentCase,
    UpdateStudentCase,
    DeleteStudentCase,
    UpdateStudentCaseStatus,
    CreateCaseEvent,
    UpdateCaseEvent,
    DeleteCaseEvent,
    GetEmployees,
    GetCasesReport,
    DeleteOldStudents;

    private val eventCode = name

    class UseNamedArguments private constructor()

    fun log(
        // This is a hack to force passing all real parameters by name
        @Suppress("UNUSED_PARAMETER") vararg forceNamed: UseNamedArguments,
        targetId: AuditId? = null,
        objectId: AuditId? = null,
        meta: Map<String, Any?> = emptyMap(),
    ) {
        logger.audit(
            mapOf(
                "eventCode" to eventCode,
                "targetId" to targetId?.value,
                "objectId" to objectId?.value,
                "securityLevel" to securityLevel,
                "securityEvent" to securityEvent,
            ) + if (meta.isNotEmpty()) mapOf("meta" to meta) else emptyMap()
        ) {
            eventCode
        }
    }
}

private val logger = KotlinLogging.logger {}
