// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus

import fi.espoo.oppivelvollisuus.shared.Audit
import fi.espoo.oppivelvollisuus.shared.AuditId
import fi.espoo.oppivelvollisuus.shared.auth.AdUser
import java.util.UUID
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.inTransactionUnchecked
import org.springframework.beans.factory.annotation.Autowired
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
    @Autowired lateinit var jdbi: Jdbi

    @PostMapping("/user-login")
    fun userLogin(@RequestBody adUser: AdUser): AppUser =
        jdbi
            .inTransactionUnchecked { it.upsertAppUserFromAd(adUser) }
            .also { Audit.EspooUserLogin.log(targetId = AuditId(it.id)) }

    @GetMapping("/users/{id}")
    fun getUser(@PathVariable id: UUID): AppUser? = jdbi.inTransactionUnchecked {
        it.getAppUser(id)
    }
}
