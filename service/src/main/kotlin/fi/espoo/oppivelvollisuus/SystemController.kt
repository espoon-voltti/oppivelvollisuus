// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus

import fi.espoo.oppivelvollisuus.AdUser
import fi.espoo.oppivelvollisuus.AppUser
import fi.espoo.oppivelvollisuus.getAppUser
import fi.espoo.oppivelvollisuus.shared.Audit
import fi.espoo.oppivelvollisuus.shared.AuditId
import fi.espoo.oppivelvollisuus.shared.db.Database
import fi.espoo.oppivelvollisuus.upsertAppUserFromAd
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
    @PostMapping("/user-login")
    fun userLogin(
        @RequestBody adUser: AdUser,
        db: Database
    ): AppUser =
        db.connect { it.transaction { tx -> tx.upsertAppUserFromAd(adUser) } }.also {
            Audit.USER_LOGIN.log(targetId = AuditId(it.id))
        }

    @GetMapping("/users/{id}")
    fun getUser(
        @PathVariable id: EspooUserId,
        db: Database
    ): AppUser? = db.connect { it.read { tx -> tx.getAppUser(id) } }
}
