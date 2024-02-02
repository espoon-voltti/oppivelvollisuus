// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.domain

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate

data class CaseReportRequest(
    val start: LocalDate?,
    val end: LocalDate?
)

data class CaseReportRow(
    val openedAt: LocalDate,
    val birthYear: Int?,
    val ageAtCaseOpened: Int?,
    val gender: Gender?,
    val language: String,
    val municipalityInFinland: Boolean,
    val status: CaseStatus,
    val finishedReason: CaseFinishedReason?,
    val startedAtSchool: SchoolType?,
    val source: CaseSource,
    val sourceValpas: ValpasNotifier?,
    val sourceOther: OtherNotifier?,
    val schoolBackground: Set<SchoolBackground>,
    val caseBackgroundReasons: Set<CaseBackgroundReason>,
    val notInSchoolReason: NotInSchoolReason?,
    val eventTypes: Set<CaseEventType>
)

fun Handle.getCasesReport(request: CaseReportRequest): List<CaseReportRow> =
    createQuery(
        """
    SELECT 
        sc.opened_at,
        extract('year' FROM s.date_of_birth) AS birth_year,
        CASE WHEN s.date_of_birth IS NOT NULL 
            THEN date_part('year', age(sc.opened_at, s.date_of_birth)) 
        END AS age_at_case_opened,
        s.gender,
        s.language,
        s.municipality_in_finland,
        sc.status,
        sc.finished_reason,
        sc.started_at_school,
        sc.source,
        sc.source_valpas,
        sc.source_other,
        sc.school_background,
        sc.case_background_reasons,
        sc.not_in_school_reason,
        coalesce(
            (SELECT array_agg(DISTINCT type)
            FROM case_events ce
            WHERE ce.student_case_id = sc.id),
            '{}'::case_event_type[]
        ) AS event_types
    FROM student_cases sc
    JOIN students s on sc.student_id = s.id
    WHERE TRUE 
    ${request.start?.let { "AND sc.opened_at >= :start" } ?: ""}
    ${request.end?.let { "AND sc.opened_at <= :end" } ?: ""}
"""
    )
        .also { if (request.start != null) it.bind("start", request.start) }
        .also { if (request.end != null) it.bind("end", request.end) }
        .mapTo<CaseReportRow>()
        .list()
