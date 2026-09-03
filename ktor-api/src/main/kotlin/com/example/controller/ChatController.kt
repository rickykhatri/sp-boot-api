package com.example.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.HtmlUtils
import org.springframework.web.client.RestClient
import org.springframework.beans.factory.annotation.Value
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import com.example.service.nlp.NLPTranslationService

data class ChatRequest(
    val message: String,
    val history: List<ChatMessage> = emptyList()
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class OpenAiChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.2
)

data class OllamaChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false
)

data class SearchResult(
    val title: String,
    val url: String
)

@RestController
@RequestMapping("/api")
class ChatController(
    private val restClientBuilder: RestClient.Builder
) {

    @Value("\${chat.provider:ollama}")
    private lateinit var chatProvider: String

    @Value("\${chat.ollama.url:https://rkchat-ai.duckdns.org}")
    private lateinit var ollamaBaseUrl: String

    @Value("\${chat.ollama.model:llama3.2}")
    private lateinit var ollamaModel: String

    @Value("\${openai.api-key:}")
    private lateinit var openAiApiKey: String

    @Value("\${openai.model:gpt-4o-mini}")
    private lateinit var openAiModel: String

    @PostMapping("/chat")
    fun chat(@RequestBody request: ChatRequest): ResponseEntity<Map<String, String>> {
        return processChatRequest(request)
    }

    @PostMapping("/send-message")
    fun sendMessage(@RequestBody request: ChatRequest): ResponseEntity<Map<String, String>> {
        return processChatRequest(request)
    }

    private fun processChatRequest(request: ChatRequest): ResponseEntity<Map<String, String>> {
        val message = request.message.trim()
        val history = request.history.takeLast(12).filter { it.content.isNotBlank() }
        if (message.isEmpty()) {
            return ResponseEntity.badRequest().body(mapOf("reply" to "Message cannot be empty."))
        }

        // Handle translation requests first
        // if(message.lowercase().contains("translate")) {
        //     return ResponseEntity.ok(mapOf("Translated Text:\n" to NLPTranslationService().translate(message)))
        // }

        // Handle search requests first
        if (isSearchRequest(message)) {
            return runCatching { callGoogleSearch(stripSearchPrefix(message)) }
                .fold(
                    onSuccess = { reply -> ResponseEntity.ok(mapOf("reply" to reply.trim())) },
                    onFailure = { exception -> ResponseEntity.status(500).body(mapOf("reply" to "Search error: ${exception.message}")) }
                )
        }

        // Handle stock queries
        // if (isStockQuery(message)) {
        //     return runCatching { callGoogleSearch(buildStockSearchQuery(message)) }
        //         .fold(
        //             onSuccess = { reply -> ResponseEntity.ok(mapOf("reply" to reply.trim())) },
        //             onFailure = { exception -> ResponseEntity.status(500).body(mapOf("reply" to "Search error: ${exception.message}")) }
        //         )
        // }

        val provider = chatProvider.trim().lowercase()

        return try {
            val aiReply = when (provider) {
                "openai" -> callOpenAi(message, history)
                else -> callOllama(message, history)
            }

            val reply = if (shouldFallbackToGoogle(aiReply)) {
                callGoogleSearch(message)
            } else {
                aiReply
            }

            ResponseEntity.ok(mapOf("reply" to reply.trim()))
        } catch (exception: Exception) {
            val googleReply = runCatching { callGoogleSearch(message) }.getOrNull().orEmpty()
            if (googleReply.isNotBlank()) {
                ResponseEntity.ok(mapOf("reply" to googleReply.trim()))
            } else {
                ResponseEntity.status(500).body(mapOf("reply" to "Chat service error: ${exception.message}"))
            }
        }
    }

    private fun callOpenAi(message: String, history: List<ChatMessage>): String {
        val apiKey = if (openAiApiKey.isNotBlank()) openAiApiKey else System.getenv("OPENAI_API_KEY")
            ?: return "OPENAI_API_KEY is not configured."

        val model = if (openAiModel.isNotBlank()) openAiModel else System.getenv("OPENAI_MODEL") ?: "gpt-4o-mini"
        val client = restClientBuilder.build()

        val response = client.post()
            .uri("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .body(
                OpenAiChatCompletionRequest(
                    model = model,
                    messages = listOf(
                        ChatMessage("system", buildSystemPrompt()),
                        *history.toTypedArray(),
                        ChatMessage("user", message)
                    )
                )
            )
            .retrieve()
            .body(Map::class.java)

        val choices = response?.get("choices") as? List<*>
        val firstChoice = choices?.firstOrNull() as? Map<*, *>
        return ((firstChoice?.get("message") as? Map<*, *>)?.get("content")?.toString())
            ?: "No reply returned."
    }

    /**
     * Calls the Ollama API to get a response for the given message.
     * If the Ollama model or base URL is not configured, it defaults to "llama3.2" and "http://localhost:11434" respectively.
     * Returns the content of the response message or a default message if no reply is returned.
     * @param message The user message to send to the Ollama API.
     * @return The content of the response message or a default message if no reply is returned.
     * @throws Exception if there is an error during the API call.
     */
    // private fun callOllama(message: String, history: List<ChatMessage>): String {
    //     val model = if (ollamaModel.isNotBlank()) ollamaModel else "llama3.2"
    //     val baseUrl = if (ollamaBaseUrl.isNotBlank()) ollamaBaseUrl else "http://localhost:11434"
    //     val client = restClientBuilder.build()

    //     val response = client.post()
    //         .uri("$baseUrl/api/chat")
    //         .header("Content-Type", "application/json")
    //         .body(
    //             OllamaChatRequest(
    //                 model = model,
    //                 messages = listOf(
    //                     ChatMessage("system", buildSystemPrompt()),
    //                     *history.toTypedArray(),
    //                     ChatMessage("user", message)
    //                 )
    //             )
    //         )
    //         .retrieve()
    //         .body(Map::class.java)

    //     val messageObject = response?.get("message") as? Map<*, *>
    //     val reply = (messageObject?.get("content")?.toString())
    //         ?: response?.get("response")?.toString()
    //         ?: "No reply returned."

    //     return if (shouldFallbackToGoogle(reply)) {
    //         callGoogleSearch(message)
    //     } else {
    //         reply
    //     }
    // }

    private fun callOllama(message: String, history: List<ChatMessage>): String {

val model = if (ollamaModel.isNotBlank()) {
    ollamaModel
} else {
    "llama3.2"
}

val baseUrl = if (ollamaBaseUrl.isNotBlank()) {
    ollamaBaseUrl.trimEnd('/')
} else {
    "https://rkchat-ai.duckdns.org"
}

val client = restClientBuilder.build()

val response = client.post()
    .uri("$baseUrl/api/chat")
    .header("Content-Type", "application/json")
    .body(
        OllamaChatRequest(
            model = model,
            messages = listOf(
                ChatMessage("system", buildSystemPrompt()),
                *history.toTypedArray(),
                ChatMessage("user", message)
            )
        )
    )
    .retrieve()
    .body(Map::class.java)

val messageObject = response?.get("message") as? Map<*, *>

val reply =
    messageObject?.get("content")?.toString()
        ?: response?.get("response")?.toString()
        ?: "No reply returned."

return if (shouldFallbackToGoogle(reply)) {
    callGoogleSearch(message)
} else {
    reply
}

}


    private fun shouldFallbackToGoogle(reply: String): Boolean {
        val normalized = reply.lowercase()
        return listOf(
            "don't have direct access to real-time data",
            "don't have real-time access",
            "real-time access to current market data",
            "current market data",
            "specific stock prices",
            "knowledge cutoff",
            "most up-to-date information"
        ).any { normalized.contains(it) }
    }

    private fun isStockQuery(message: String): Boolean {
        val normalized = message.trim().lowercase()
        if (normalized.isBlank()) {
            return false
        }

        val stockWords = listOf(
            "stock",
            "share price",
            "share",
            "quote",
            "market price",
            "market cap",
            "price",
            "nifty",
            "sensex",
            "indices",
            "index"
        )

        if (stockWords.any { normalized.contains(it) }) {
            return true
        }

        return isTickerLikeSymbol(message.trim())
    }

    private fun buildStockSearchQuery(message: String): String {
        val normalized = message.trim()
        val upper = normalized.uppercase()
        return when {
            upper == "NIFTY" -> "NIFTY 50 index"
            upper == "SENSEX" -> "SENSEX index"
            isTickerLikeSymbol(normalized) -> "$upper stock price"
            else -> normalized
        }
    }

    private fun isTickerLikeSymbol(value: String): Boolean {
        val trimmed = value.trim()
        if (!trimmed.matches(Regex("^[A-Za-z0-9.\\-]{2,12}$"))) {
            return false
        }

        if (trimmed.any { it.isLowerCase() }) {
            return false
        }

        return true
    }

    private fun isSearchRequest(message: String): Boolean {
        val normalized = message.trim().lowercase()
        return normalized.startsWith("search ") ||
            normalized.startsWith("search:") ||
            normalized.startsWith("search for ") ||
            normalized.startsWith("find ") ||
            normalized.startsWith("look up ") ||
            normalized.startsWith("lookup ") ||
            normalized.startsWith("web search ") ||
            normalized.startsWith("show me ") ||
            normalized.startsWith("show me:")
    }

    private fun stripSearchPrefix(message: String): String {
        val normalized = message.trim()
        val prefixes = listOf(
            Regex("^search\\s*:\\s*", RegexOption.IGNORE_CASE),
            Regex("^search\\s+for\\s+", RegexOption.IGNORE_CASE),
            Regex("^search\\s+", RegexOption.IGNORE_CASE),
            Regex("^find\\s+", RegexOption.IGNORE_CASE),
            Regex("^look\\s+up\\s+", RegexOption.IGNORE_CASE),
            Regex("^lookup\\s+", RegexOption.IGNORE_CASE),
            Regex("^web\\s+search\\s+", RegexOption.IGNORE_CASE),
            Regex("^show\\s+me\\s*:\\s*", RegexOption.IGNORE_CASE),
            Regex("^show\\s+me\\s+", RegexOption.IGNORE_CASE)
        )

        return prefixes.fold(normalized) { current, prefix -> prefix.replace(current, "") }.trim().ifBlank { normalized }
    }

    private fun callGoogleSearch(query: String): String {
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8)
        val searchUrl = "https://www.google.com/search?hl=en&num=3&q=$encodedQuery"
        val client = restClientBuilder.build()

        val html = client.get()
            .uri(searchUrl)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"
            )
            .header("Accept-Language", "en-US,en;q=0.9")
            .retrieve()
            .body(String::class.java)
            ?: return "No search results found for: $query"

        val results = extractGoogleResults(html)
        if (results.isNotEmpty()) {
            return buildString {
                appendLine("Google results for: $query")
                results.forEachIndexed { index, result ->
                    appendLine("${index + 1}. ${result.title}")
                    appendLine(result.url)
                    if (index < results.lastIndex) {
                        appendLine()
                    }
                }
            }.trim()
        }

        val duckDuckGoResults = callDuckDuckGoSearch(query)
        if (duckDuckGoResults.isNotBlank()) {
            return duckDuckGoResults
        }

        return "No search results found for: $query"
    }

    private fun callDuckDuckGoSearch(query: String): String {
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8)
        val searchUrl = "https://html.duckduckgo.com/html/?q=$encodedQuery"
        val client = restClientBuilder.build()

        val html = client.get()
            .uri(searchUrl)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"
            )
            .header("Accept-Language", "en-US,en;q=0.9")
            .retrieve()
            .body(String::class.java)
            ?: return ""

        val results = extractDuckDuckGoResults(html)
        if (results.isEmpty()) {
            return ""
        }

        return buildString {
            appendLine("Results: $query")
            results.forEachIndexed { index, result ->
                appendLine("${index + 1}. ${result.title}")
                appendLine(result.url)
                if (index < results.lastIndex) {
                    appendLine()
                }
            }
        }.trim()
    }

    private fun extractGoogleResults(html: String): List<SearchResult> {
        val resultPattern = Regex(
            """<a[^>]+href="/url\?q=([^"&]+)[^"]*"[^>]*>.*?<h3[^>]*>(.*?)</h3>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )

        return resultPattern.findAll(html)
            .mapNotNull { match ->
                val encodedUrl = match.groupValues.getOrNull(1).orEmpty()
                val titleHtml = match.groupValues.getOrNull(2).orEmpty()
                val decodedUrl = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8).trim()
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

    private fun extractDuckDuckGoResults(html: String): List<SearchResult> {
        val resultPattern = Regex(
            """<a[^>]+class="result__a"[^>]+href="([^"]+)"[^>]*>(.*?)</a>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )

        return resultPattern.findAll(html)
            .mapNotNull { match ->
                val url = cleanHtml(match.groupValues.getOrNull(1).orEmpty())
                val title = cleanHtml(match.groupValues.getOrNull(2).orEmpty())

                if (url.isBlank() || title.isBlank()) {
                    null
                } else {
                    SearchResult(title = title, url = url)
                }
            }
            .distinctBy { it.url }
            .take(3)
            .toList()
    }

    private fun cleanHtml(value: String): String {
        return HtmlUtils.htmlUnescape(value)
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun buildSystemPrompt(): String {
        val today = LocalDate.now()
        return buildString {
            append("You are a concise, helpful assistant for this application. ")
            append("Use the conversation history to preserve context across turns. ")
            append("Format chat replies in Markdown so the chatbox can render them clearly. ")
            append("Use headings, bullet points, numbered lists, tables, fenced code blocks, and Markdown links when helpful. ")
            append("Keep related items on separate lines and avoid long unbroken paragraphs when a structured reply is better. ")
            append("When users ask about websites, pages, or URLs, prefer giving the direct website URL or a clearly labeled link so the UI can show a webpage preview. ")
            append("The current date is ")
            append(today)
            append(" and the current year is ")
            append(today.year)
            append(". ")
            append("For any age calculation or date-based answer, compute from this current date/year and do not use stale years or guessed dates. ")
            append("If a user asks for age from a birth date, calculate the exact age from the current date before answering.")
        }
    }
}