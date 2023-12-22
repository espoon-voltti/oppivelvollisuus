package fi.espoo.oppivelvollisuus.domain

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate

data class CaseReportRow(
    val openedAt: LocalDate,
    val source: CaseSource,
    val gender: Gender?,
    val language: String,
    val status: CaseStatus,
    val finishedReason: CaseFinishedReason?,
    val startedAtSchool: SchoolType?
)

fun Handle.getCasesReport(): List<CaseReportRow> =
    createQuery(
        """
    SELECT 
        sc.opened_at,
        sc.source,
        s.gender,
        s.language,
        sc.status,
        sc.finished_reason,
        sc.started_at_school
    FROM student_cases sc
    JOIN students s on sc.student_id = s.id
"""
    ).mapTo<CaseReportRow>().list()
