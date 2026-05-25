// SPDX-FileCopyrightText: 2023-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.valpas

import fi.espoo.oppivelvollisuus.ValpasIntegrationEnv
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.InputStream
import java.time.Duration
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue

private val logger = KotlinLogging.logger {}

private data class StartQueryResponse(val queryId: String)

private data class QueryStatusResponse(val status: String, val files: List<String> = emptyList())

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
            // Disable automatic redirect-following. The result-file URLs returned by
            // Valpas live under our configured baseUrl but redirect (302) to a presigned
            // URL on a different host (e.g. AWS S3). OkHttp would forward the
            // Authorization header to that cross-host target, leaking our Valpas
            // basic-auth credentials. downloadResultFile follows the redirect manually
            // with no Authorization header — the presigned URL self-authenticates via
            // its query-string signature.
            .followRedirects(false)
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
            return try {
                jsonMapper.readValue<StartQueryResponse>(text).queryId
            } catch (e: Exception) {
                throw ValpasIntegrationException("startQuery response unparseable: $text", e)
            }
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
            val response =
                try {
                    jsonMapper.readValue<QueryStatusResponse>(text)
                } catch (e: Exception) {
                    throw ValpasIntegrationException(
                        "getQueryStatus response unparseable: $text",
                        e,
                    )
                }
            return when (response.status) {
                "pending" -> ValpasQueryStatus.Pending
                "running" -> ValpasQueryStatus.Running
                "complete" -> ValpasQueryStatus.Complete(response.files)
                "failed" -> ValpasQueryStatus.Failed
                else -> throw ValpasIntegrationException("getQueryStatus unknown status: $text")
            }
        }
    }

    override fun downloadResultFile(url: String): ValpasResultFile {
        // Refuse URLs not under the configured Valpas baseUrl so a compromised
        // getQueryStatus response can't steer the authenticated GET to an attacker.
        require(url.startsWith("$baseUrl/")) {
            "downloadResultFile refused URL not under configured Valpas baseUrl"
        }
        val initialRequest =
            Request.Builder()
                .url(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .get()
                .build()
        httpClient.newCall(initialRequest).execute().use { resp ->
            if (resp.code in 300..399) {
                val location =
                    resp.header("Location")
                        ?: throw ValpasIntegrationException(
                            "downloadResultFile got ${resp.code} without Location header"
                        )
                // Drop the Authorization header on the redirect. Valpas redirects to a
                // presigned URL on a different host; forwarding our basic-auth credentials
                // cross-host would leak them. The presigned URL self-authenticates via
                // its query-string signature.
                val redirectRequest =
                    Request.Builder()
                        .url(location)
                        .header("Accept", "application/json")
                        .get()
                        .build()
                return httpClient.newCall(redirectRequest).execute().use { redirectResp ->
                    if (!redirectResp.isSuccessful) {
                        throw ValpasIntegrationException(
                            "downloadResultFile redirect target failed: status=${redirectResp.code}"
                        )
                    }
                    parseResultFile(redirectResp.body?.byteStream())
                }
            }
            if (!resp.isSuccessful) {
                throw ValpasIntegrationException("downloadResultFile failed: status=${resp.code}")
            }
            return parseResultFile(resp.body?.byteStream())
        }
    }

    private fun parseResultFile(stream: InputStream?): ValpasResultFile {
        if (stream == null) throw ValpasIntegrationException("downloadResultFile empty body")
        return jsonMapper.readValue(stream, ValpasResultFile::class.java)
    }
}
