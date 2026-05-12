// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.shared.dev

import fi.espoo.oppivelvollisuus.shared.db.Database
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.io.ClassPathResource
import kotlin.io.bufferedReader
import kotlin.io.readText
import kotlin.io.use
import kotlin.let

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

// Body intentionally stubbed during Phase 2 — Phase 3.8 rewrites this for
// oppivelvollisuus's domain entities. The vakaseteli body referenced
// CompanyId/DaycareId etc. that don't exist here.
