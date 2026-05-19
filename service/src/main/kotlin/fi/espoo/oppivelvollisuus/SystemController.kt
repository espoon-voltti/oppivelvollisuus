// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus

import fi.espoo.oppivelvollisuus.shared.Audit
import fi.espoo.oppivelvollisuus.shared.AuditId
import fi.espoo.oppivelvollisuus.shared.auth.AdUser
import fi.espoo.oppivelvollisuus.shared.db.Database
import fi.espoo.oppivelvollisuus.shared.time.AppClock
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for "system" endpoints intended to be only called from api-gateway as the system
 * internal user
 */
@RestController
@RequestMapping("/system")
class SystemController {
    @PostMapping("/user-login")
    fun userLogin(db: Database, clock: AppClock, @RequestBody adUser: AdUser): AppUser =
        db.connect { dbc ->
                dbc.transaction { tx -> tx.upsertAppUserFromAd(adUser, now = clock.now()) }
            }
            .also { Audit.EspooUserLogin.log(targetId = AuditId(it.id)) }

    @GetMapping("/users/{id}")
    fun getUser(db: Database, @PathVariable id: EspooUserId): AppUser? = db.connect { dbc ->
        dbc.read { it.getAppUser(id) }
    }
}
