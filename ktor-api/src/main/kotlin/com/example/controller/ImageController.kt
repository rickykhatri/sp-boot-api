package com.example.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/api")
class ImageController(
    private val restClientBuilder: RestClient.Builder
) {

    @Value("\${image.provider:pollinations}")
    private lateinit var imageProvider: String

    @Value("\${image.pollinations.url:https://image.pollinations.ai}")
    private lateinit var imagePollinationsUrl: String

    @Value("\${image.generator.url:http://localhost:7860}")
    private lateinit var imageGeneratorUrl: String

    @PostMapping("/image")
    fun generateImage(@RequestBody request: ImageGenerationRequest): ResponseEntity<Map<String, String>> {
        val prompt = request.prompt.trim()
        if (prompt.isEmpty()) {
            return ResponseEntity.badRequest().body(mapOf("reply" to "Prompt cannot be empty."))
        }

        return try {
            val imageBase64 = generateImageBase64(prompt)
            ResponseEntity.ok(
                mapOf(
                    "reply" to "Image generated for: $prompt using ${resolveImageProviderName()}",
                    "imageBase64" to imageBase64,
                    "imageDataUrl" to "data:image/png;base64,$imageBase64"
                )
            )
        } catch (exception: Exception) {
            ResponseEntity.status(500).body(mapOf("reply" to "Image generation error: ${exception.message}"))
        }
    }

    @GetMapping("/image/status")
    fun imageStatus(): ResponseEntity<Map<String, Any>> {
        val provider = imageProvider.trim().lowercase()
        return buildStatusResponse(
            serviceName = "image",
            checkUrl = if (provider == "pollinations") {
                "$imagePollinationsUrl/prompt/test"
            } else {
                "$imageGeneratorUrl/sdapi/v1/sd-models"
            }
        )
    }

    private fun callStableDiffusion(prompt: String): String {
        val client = restClientBuilder.build()
        val response = client.post()
            .uri("$imageGeneratorUrl/sdapi/v1/txt2img")
            .header("Content-Type", "application/json")
            .body(
                StableDiffusionTxt2ImgRequest(
                    prompt = prompt,
                    steps = 20,
                    width = 768,
                    height = 768
                )
            )
            .retrieve()
            .body(Map::class.java)

        val images = response?.get("images") as? List<*>
        val firstImage = images?.firstOrNull()?.toString()
        return firstImage ?: throw IllegalStateException("No generated image returned by the local image generator.")
    }

    private fun callPollinationsImage(prompt: String): String {
        val client = restClientBuilder.build()
        val encodedPrompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8)
        val imageBytes = client.get()
            .uri("$imagePollinationsUrl/prompt/$encodedPrompt?width=768&height=768&nologo=true&safe=true")
            .retrieve()
            .body(ByteArray::class.java)
            ?: throw IllegalStateException("No image returned by the free image generator.")

        return java.util.Base64.getEncoder().encodeToString(imageBytes)
    }

    private fun generateImageBase64(prompt: String): String {
        return when (imageProvider.trim().lowercase()) {
            "pollinations" -> callPollinationsImage(prompt)
            "local", "stable-diffusion", "automatic1111" -> {
                ensureImageGeneratorAvailable()
                callStableDiffusion(prompt)
            }
            else -> {
                ensureImageGeneratorAvailable()
                callStableDiffusion(prompt)
            }
        }
    }

    private fun resolveImageProviderName(): String {
        return when (imageProvider.trim().lowercase()) {
            "pollinations" -> ""
            "local", "stable-diffusion", "automatic1111" -> "local Stable Diffusion"
            else -> imageProvider.ifBlank { "local Stable Diffusion" }
        }
    }

    private fun ensureImageGeneratorAvailable() {
        val client = restClientBuilder.build()
        try {
            client.get()
                .uri("$imageGeneratorUrl/sdapi/v1/sd-models")
                .retrieve()
                .toBodilessEntity()
        } catch (exception: Exception) {
            throw IllegalStateException(
                "Image generator is not reachable at $imageGeneratorUrl. Start the local Stable Diffusion backend or set IMAGE_GENERATOR_URL to a running service.",
                exception
            )
        }
    }

    private fun buildStatusResponse(serviceName: String, checkUrl: String): ResponseEntity<Map<String, Any>> {
        return try {
            val client = restClientBuilder.build()
            client.get()
                .uri(checkUrl)
                .retrieve()
                .toBodilessEntity()

            ResponseEntity.ok(
                mapOf(
                    "service" to serviceName,
                    "available" to true,
                    "message" to "$serviceName service is available."
                )
            )
        } catch (exception: Exception) {
            ResponseEntity.ok(
                mapOf(
                    "service" to serviceName,
                    "available" to false,
                    "message" to "$serviceName service is not reachable: ${exception.message}"
                )
            )
        }
    }
}
