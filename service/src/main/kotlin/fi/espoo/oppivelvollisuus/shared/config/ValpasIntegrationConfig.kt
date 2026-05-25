// SPDX-FileCopyrightText: 2023-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.shared.config

import fi.espoo.oppivelvollisuus.ValpasIntegrationEnv
import fi.espoo.oppivelvollisuus.valpas.HttpValpasClient
import fi.espoo.oppivelvollisuus.valpas.MockValpasClient
import fi.espoo.oppivelvollisuus.valpas.ValpasClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.json.JsonMapper

@Configuration
class ValpasIntegrationConfig {
    private val logger = KotlinLogging.logger {}

    @Bean
    fun valpasClient(env: ValpasIntegrationEnv, jsonMapper: JsonMapper): ValpasClient =
        if (env.enabled) {
            logger.info { "Using HttpValpasClient with baseUrl=${env.opintopolkuBaseUrl}" }
            HttpValpasClient(env, jsonMapper)
        } else {
            logger.info { "Valpas integration disabled — using MockValpasClient" }
            MockValpasClient()
        }
}
