package com.example.controller

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

// ── External API response models ─────────────────────────────────────────────

data class LoginRequest(
    val username: String,
    val password: String,
    val locale: String = "en_US"
)

data class ExternalPost(
    val id: Int = 0,
    val userId: Int = 0,
    val title: String = "",
    val body: String = ""
)

data class ExternalComment(
    val id: Int = 0,
    val postId: Int = 0,
    val name: String = "",
    val email: String = "",
    val body: String = ""
)

data class ExternalTodo(
    val id: Int = 0,
    val userId: Int = 0,
    val title: String = "",
    val completed: Boolean = false
)

// ── Controller ────────────────────────────────────────────────────────────────

/**
 * Fetches data from JSONPlaceholder (https://jsonplaceholder.typicode.com)
 * — a free, public REST API used for testing and prototyping.
 *
 * Base route: /api/external
 */
@RestController
@RequestMapping("/api/external")
class ExternalApiController {

    private val baseUrl = "https://qa2-rest-api.wm.com/"
    private val client = RestClient.create(baseUrl)

    // ── Posts ──────────────────────────────────────────────────────────────────

    @GetMapping("/versions/{language}") 
    fun getVersionByLanguage(@PathVariable language: String): ResponseEntity<Any> {
        return try {
            val methodName = if(language.equals("english", ignoreCase = true)) "/mobile/englishversions.json" else "/mobile/frenchversions.json"
            val response = client.get()
                .uri(methodName)
                .retrieve()
                .body(Any::class.java)
            ResponseEntity.ok(response)
        } catch (e: RestClientResponseException) {
            ResponseEntity.status(e.rawStatusCode).body(mapOf("error" to e.responseBodyAsString))
        } catch (e: Exception) {
            ResponseEntity.status(500).body(mapOf("error" to e.message))
        }
    }

    // ── Authentication ─────────────────────────────────────────────────────────
    @PostMapping("/user")
    fun authenticate(@RequestBody loginRequest: LoginRequest): ResponseEntity<Any> {
        return try {
            val response = client.post()
                .uri("/user/authenticate")
                .header("apiKey", "5CA8C6B74EC100626933")
                .header("Content-Type", "application/json; charset=UTF-8")
                .body(loginRequest)
                .retrieve()
                .body(Any::class.java)
            ResponseEntity.ok(response)
        } catch (e: RestClientResponseException) {
            ResponseEntity.status(e.rawStatusCode).body(mapOf("error" to e.responseBodyAsString + " — " + baseUrl + "user/authenticate"))
        } catch (e: Exception) {
            ResponseEntity.status(500).body(mapOf("error" to e.message+ " — " + baseUrl + "user/authenticate"))
        }
    }

    @GetMapping("/accounts/{userId}")
    fun getAccounts(
        @PathVariable userId: String,
        @RequestParam timestamp: Long,
        @RequestParam(defaultValue = "en_US") lang: String,
        @RequestHeader("token", required = false) token: String?,
        @RequestHeader("oktaToken", required = false) oktaToken: String?,
        @RequestHeader("apiKey", required = false) apiKey: String?
    ): ResponseEntity<Any> {
        return try {
            val resolvedToken = token?.takeIf { it.isNotBlank() } ?: oktaToken?.takeIf { it.isNotBlank() }
            val resolvedOktaToken = oktaToken?.takeIf { it.isNotBlank() } ?: token?.takeIf { it.isNotBlank() }

            val request = client.get()
                .uri("/authorize/user/{userId}/accounts?timestamp={timestamp}&lang={lang}", userId, timestamp, lang)

            resolvedToken?.let { request.header("token", it) }
            resolvedOktaToken?.let { request.header("oktaToken", it) }
            apiKey?.takeIf { it.isNotBlank() }?.let { request.header("apiKey", it) }
            request.header("Origin", "https://qa.wm.com")
            request.header("User-Agent", "My WM/7.6-dev/Dalvik/2.1.0 (Linux; U; Android 17; sdk_gphone16k_arm64 Build/CP21.260330.005)")

            val response = request
                .retrieve()
                .body(Any::class.java)

            ResponseEntity.ok(response)
        } catch (e: RestClientResponseException) {
            ResponseEntity.status(e.rawStatusCode).body(
                mapOf(
                    "error" to e.responseBodyAsString,
                    "path" to "$baseUrl authorize/user/$userId/accounts"
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(500).body(
                mapOf(
                    "error" to e.message,
                    "path" to "$baseUrl authorize/user/$userId/accounts"
                )
            )
        }
    }
}
