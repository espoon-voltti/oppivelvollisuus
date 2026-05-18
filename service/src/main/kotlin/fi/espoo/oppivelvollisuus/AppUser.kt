// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus

import fi.espoo.oppivelvollisuus.shared.auth.AdUser
import fi.espoo.oppivelvollisuus.shared.db.Database
import fi.espoo.oppivelvollisuus.shared.time.HelsinkiDateTime
import org.jdbi.v3.core.mapper.PropagateNull

data class AppUser(
    val id: EspooUserId,
    val externalId: String,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val isActive: Boolean,
)

data class UserBasics(@param:PropagateNull val id: EspooUserId, val name: String)

fun Database.Transaction.upsertAppUserFromAd(adUser: AdUser, now: HelsinkiDateTime): AppUser =
    createQuery {
            sql(
                """
                INSERT INTO users (external_id, first_names, last_name, email, is_active, created)
                VALUES (${bind(adUser.externalId)}, ${bind(adUser.firstName)}, ${bind(adUser.lastName)}, ${bind(adUser.email)}, true, ${bind(now)})
                ON CONFLICT (external_id) DO UPDATE
                SET updated = ${bind(now)}, first_names = ${bind(adUser.firstName)}, last_name = ${bind(adUser.lastName)}, email = ${bind(adUser.email)}
                RETURNING id, external_id, first_name, last_name, email, is_active
                """
            )
        }
        .exactlyOne<AppUser>()

fun Database.Read.getActiveAppUsers(): List<AppUser> =
    createQuery {
            sql(
                """
                SELECT id, external_id, first_name, last_name, email, is_active
                FROM users
                WHERE NOT is_system_user AND is_active
                """
            )
        }
        .toList<AppUser>()

fun Database.Read.getAppUser(id: EspooUserId): AppUser? =
    createQuery {
            sql(
                """
                SELECT id, external_id, first_name, last_name, email, is_active
                FROM users
                WHERE id = ${bind(id)} AND NOT is_system_user
                """
            )
        }
        .exactlyOneOrNull<AppUser>()
