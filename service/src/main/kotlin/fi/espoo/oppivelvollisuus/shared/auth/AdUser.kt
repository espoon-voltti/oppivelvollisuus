// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.shared.auth

data class AdUser(
    val externalId: String,
    val firstName: String,
    val lastName: String,
    val email: String?,
)
