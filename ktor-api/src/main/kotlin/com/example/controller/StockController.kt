package com.example.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/api/stocks")
class StockController(
    private val restClientBuilder: RestClient.Builder
) {

    @Value("\${stocks.google.url:https://www.google.com}")
    private lateinit var googleBaseUrl: String

    @GetMapping("/chart")
    fun stockChart(
        @RequestParam symbol: String,
        @RequestParam(defaultValue = "1mo") range: String,
        @RequestParam(defaultValue = "1d") interval: String
    ): ResponseEntity<Map<String, Any>> {
        val resolvedSymbol = symbol.trim().uppercase()
        if (resolvedSymbol.isBlank()) {
            return ResponseEntity.badRequest().body(mapOf("reply" to "Stock symbol cannot be empty."))
        }

        return try {
            val response = searchGoogleForStock(resolvedSymbol, range.trim(), interval.trim())
            ResponseEntity.ok(response)
        } catch (exception: Exception) {
            ResponseEntity.status(500).body(
                mapOf(
                    "reply" to "Unable to load stock chart for $resolvedSymbol: ${exception.message}"
                )
            )
        }
    }

    @GetMapping("/status")
    fun stockStatus(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(
            mapOf(
                "service" to "stocks",
                "available" to true,
                "message" to "stock search service is available."
            )
        )
    }

    private fun searchGoogleForStock(symbol: String, range: String, interval: String): Map<String, Any> {
        val client = restClientBuilder.build()
        val queryVariants = listOf(
            "$symbol stock price",
            "$symbol share price",
            "$symbol quote",
            "$symbol stock chart",
            "$symbol index",
            "$symbol market price"
        ).distinct()

        val directSearchUrl = buildGoogleSearchUrl("$symbol stock price", range, interval)

        val results = queryVariants.asSequence()
            .map { query ->
                val url = buildGoogleSearchUrl(query, range, interval)
                val html = client.get()
                    .uri(url)
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"
                    )
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .retrieve()
                    .body(String::class.java)

                html?.let { extractGoogleResults(it) }.orEmpty()
            }
            .firstOrNull { it.isNotEmpty() }
            .orEmpty()

        if (results.isEmpty()) {
            return mapOf(
                "reply" to buildString {
                    appendLine("Google search page opened for $symbol, but no parseable result links were found.")
                    appendLine("Open directly: $directSearchUrl")
                }.trim(),
                "searchUrl" to directSearchUrl
            )
        }

        return mapOf(
            "reply" to buildString {
                appendLine("Google results for: $symbol stock price")
                appendLine("Open directly: $directSearchUrl")
                appendLine()
                results.forEachIndexed { index, result ->
                    appendLine("${index + 1}. ${result.title}")
                    appendLine(result.url)
                    if (index < results.lastIndex) {
                        appendLine()
                    }
                }
            }.trim()
        )
    }

    private fun buildGoogleSearchUrl(query: String, range: String, interval: String): String {
        val encodedQuery = URLEncoder.encode(
            "$query ${range.ifBlank { "1mo" }} ${interval.ifBlank { "1d" }}",
            StandardCharsets.UTF_8
        )
        return "$googleBaseUrl/search?hl=en&num=5&q=$encodedQuery"
    }

    private fun extractGoogleResults(html: String): List<SearchResult> {
        val resultPattern = Regex(
            """<a[^>]+href="([^"]+)"[^>]*>.*?<h3[^>]*>(.*?)</h3>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )

        return resultPattern.findAll(html)
            .mapNotNull { match ->
                val href = match.groupValues.getOrNull(1).orEmpty()
                val titleHtml = match.groupValues.getOrNull(2).orEmpty()
                val decodedUrl = extractGoogleTargetUrl(href)
                val title = cleanHtml(titleHtml)

                if (decodedUrl.isBlank() || title.isBlank()) {
                    null
                } else {
                    SearchResult(title = title, url = decodedUrl)
                }
            }
            .distinctBy { it.url }
            .take(3)
            .toList()
    }

    private fun extractGoogleTargetUrl(href: String): String {
        val normalizedHref = href.trim()
        if (normalizedHref.isBlank()) {
            return ""
        }

        val absoluteHref = when {
            normalizedHref.startsWith("http://") || normalizedHref.startsWith("https://") -> normalizedHref
            normalizedHref.startsWith("/url?") -> "$googleBaseUrl$normalizedHref"
            else -> return ""
        }

        val uri = runCatching { URI(absoluteHref) }.getOrNull() ?: return ""
        val query = uri.rawQuery.orEmpty()
        val params = query.split('&')
            .mapNotNull { entry ->
                val separatorIndex = entry.indexOf('=')
                if (separatorIndex <= 0) {
                    null
                } else {
                    val key = entry.substring(0, separatorIndex)
                    val value = entry.substring(separatorIndex + 1)
                    key to value
                }
            }
            .toMap()

        val candidate = params["url"] ?: params["q"] ?: params["u"] ?: uri.toString()
        val decodedCandidate = URLDecoder.decode(candidate, StandardCharsets.UTF_8).trim()

        if (decodedCandidate.contains("google.com/search", ignoreCase = true)) {
            return ""
        }

        return decodedCandidate
    }

    private fun cleanHtml(value: String): String {
        return value
            .replace(Regex("<[^>]+>"), "")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
    }
}