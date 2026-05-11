// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus

import fi.espoo.oppivelvollisuus.config.audit
import fi.espoo.oppivelvollisuus.domain.AdUser
import fi.espoo.oppivelvollisuus.domain.AppUser
import fi.espoo.oppivelvollisuus.domain.getAppUser
import fi.espoo.oppivelvollisuus.domain.upsertAppUserFromAd
import fi.espoo.oppivelvollisuus.shared.auth.AuthenticatedUser
import fi.espoo.oppivelvollisuus.shared.db.Database
import io.opentelemetry.api.trace.Tracer
import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for "system" endpoints intended to be only called from api-gateway
 * as the system internal user
 */
@RestController
@RequestMapping("/system")
class SystemController {
    @Autowired
    lateinit var jdbi: Jdbi

    @Autowired
    lateinit var tracer: Tracer

    private val logger = KotlinLogging.logger {}

    private fun db() = Database(jdbi, tracer)

    @PostMapping("/user-login")
    fun userLogin(
        @RequestBody adUser: AdUser
    ): AppUser =
        db().connect { it.transaction { tx -> tx.upsertAppUserFromAd(adUser) } }.also {
            logger.audit(AuthenticatedUser.EspooUser(it.id), "USER_LOGIN")
        }

    @GetMapping("/users/{id}")
    fun getUser(
        @PathVariable id: EspooUserId
    ): AppUser? = db().connect { it.read { tx -> tx.getAppUser(id) } }
}
