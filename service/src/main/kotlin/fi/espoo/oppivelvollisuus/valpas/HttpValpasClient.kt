// SPDX-FileCopyrightText: 2025-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.valpas

import fi.espoo.oppivelvollisuus.ValpasIntegrationEnv
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import tools.jackson.databind.json.JsonMapper

private val logger = KotlinLogging.logger {}

class HttpValpasClient(env: ValpasIntegrationEnv, private val jsonMapper: JsonMapper) :
    ValpasClient {
    private val baseUrl =
        requireNotNull(env.opintopolkuBaseUrl) {
                "ValpasIntegrationEnv.opintopolkuBaseUrl must be set when the integration is enabled"
            }
            .trimEnd('/')
    private val kuntaOid =
        requireNotNull(env.kuntaOid) {
            "ValpasIntegrationEnv.kuntaOid must be set when the integration is enabled"
        }
    private val authHeader =
        Credentials.basic(
            requireNotNull(env.username) {
                "ValpasIntegrationEnv.username must be set when the integration is enabled"
            },
            requireNotNull(env.password) {
                    "ValpasIntegrationEnv.password must be set when the integration is enabled"
                }
                .value,
        )

    private val httpClient: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(60))
            .writeTimeout(Duration.ofSeconds(30))
            .followRedirects(true)
            .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override fun startQuery(): String {
        val body =
            jsonMapper.writeValueAsString(
                mapOf(
                    "type" to "eiSuoritaOppivelvollisuutta",
                    "format" to "application/json",
                    "kuntaOid" to kuntaOid,
                    "vainAktiivisetKuntailmoitukset" to true,
                )
            )
        val request =
            Request.Builder()
                .url("$baseUrl/koski/valpas/api/massaluovutus")
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .post(body.toRequestBody(jsonMediaType))
                .build()
        httpClient.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw ValpasIntegrationException(
                    "startQuery failed: status=${resp.code} body=$text"
                )
            }
            val node = jsonMapper.readTree(text)
            return node.get("queryId")?.asString()
                ?: throw ValpasIntegrationException("startQuery response missing queryId: $text")
        }
    }

    override fun getQueryStatus(queryId: String): ValpasQueryStatus {
        val request =
            Request.Builder()
                .url("$baseUrl/koski/valpas/api/massaluovutus/$queryId")
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .get()
                .build()
        httpClient.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw ValpasIntegrationException(
                    "getQueryStatus failed: status=${resp.code} body=$text"
                )
            }
            val node = jsonMapper.readTree(text)
            return when (node.get("status")?.asString()) {
                "pending" -> {
                    ValpasQueryStatus.Pending
                }

                "running" -> {
                    ValpasQueryStatus.Running
                }

                "complete" -> {
                    val filesNode =
                        node.get("files")
                            ?: throw ValpasIntegrationException(
                                "complete response missing files: $text"
                            )
                    val urls: List<String> =
                        (filesNode as Iterable<*>)
                            .filterIsInstance<tools.jackson.databind.JsonNode>()
                            .map { it.asString() }
                    ValpasQueryStatus.Complete(urls)
                }

                "failed" -> {
                    ValpasQueryStatus.Failed
                }

                else -> {
                    throw ValpasIntegrationException("getQueryStatus unknown status: $text")
                }
            }
        }
    }

    override fun downloadResultFile(url: String): ValpasResultFile {
        val request =
            Request.Builder()
                .url(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .get()
                .build()
        httpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw ValpasIntegrationException("downloadResultFile failed: status=${resp.code}")
            }
            val stream =
                resp.body?.byteStream()
                    ?: throw ValpasIntegrationException("downloadResultFile empty body")
            return jsonMapper.readValue(stream, ValpasResultFile::class.java)
        }
    }
}
