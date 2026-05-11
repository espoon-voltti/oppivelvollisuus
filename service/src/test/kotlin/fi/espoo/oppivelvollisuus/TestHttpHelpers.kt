// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

private const val KEY_ID = "oppivelvollisuus-api-gateway"

private val rsaAlgorithm: Algorithm by lazy {
    val pemContent =
        object {}::class.java
            .getResourceAsStream("/local-development/jwt_private_key.pem")!!
            .bufferedReader()
            .use { it.readText() }
    val keyContent =
        pemContent
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
    val keyBytes = Base64.getDecoder().decode(keyContent)
    val privateKey =
        KeyFactory
            .getInstance("RSA")
            .generatePrivate(PKCS8EncodedKeySpec(keyBytes)) as RSAPrivateKey
    Algorithm.RSA256(null, privateKey)
}

fun makeTestToken(subject: String): String =
    JWT
        .create()
        .withKeyId(KEY_ID)
        .withSubject(subject)
        .sign(rsaAlgorithm)

data class HttpResult(
    val statusCode: Int,
    val body: String,
    val contentType: String?
)

class TestHttpClient(
    private val port: Int
) {
    private val client: HttpClient = HttpClient.newHttpClient()

    fun get(
        path: String,
        token: String? = null
    ): HttpResult = send(HttpRequest.newBuilder().uri(URI.create("http://localhost:$port$path")).GET(), token)

    fun post(
        path: String,
        body: String,
        token: String? = null
    ): HttpResult =
        send(
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$port$path"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)),
            token
        )

    fun put(
        path: String,
        body: String,
        token: String? = null
    ): HttpResult =
        send(
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$port$path"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body)),
            token
        )

    private fun send(
        builder: HttpRequest.Builder,
        token: String?
    ): HttpResult {
        if (token != null) {
            builder.header("Authorization", "Bearer $token")
        }
        val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        return HttpResult(
            statusCode = response.statusCode(),
            body = response.body(),
            contentType = response.headers().firstValue("Content-Type").orElse(null)
        )
    }
}
