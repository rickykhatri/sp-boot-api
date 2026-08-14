
package com.example.service.nlp
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class NLPTranslationService(
    private val restTemplate: RestTemplate = RestTemplate()
) {

    fun translate(text: String): String {
        val body = mapOf(
            "q" to text,
            "source" to "en",
            "target" to "es",
            "format" to "text"
        )

        val response = restTemplate.postForObject(
            "https://libretranslate.com/translate",
            body,
            Map::class.java
        )

        return response?.get("translatedText") as String
    }
}