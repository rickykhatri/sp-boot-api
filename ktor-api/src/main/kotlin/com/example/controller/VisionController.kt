package com.example.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient
import org.springframework.web.multipart.MultipartFile
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.roundToInt

@RestController
@RequestMapping("/api")
class VisionController(
    private val restClientBuilder: RestClient.Builder
) {

    @Value("\${vision.ollama.url:http://localhost:11434}")
    private lateinit var visionOllamaUrl: String

    @Value("\${vision.ollama.model:llama3.2-vision}")
    private lateinit var visionOllamaModel: String

    @PostMapping(
        "/vision",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun analyzeImage(
        @RequestPart("image") image: MultipartFile,
        @RequestParam(value = "prompt", required = false, defaultValue = "Describe this image.") prompt: String
    ): ResponseEntity<Map<String, String>> {
        if (image.isEmpty) {
            return ResponseEntity.badRequest().body(mapOf("reply" to "Image file cannot be empty."))
        }

        return try {
            val reply = callOllamaVision(prompt.trim().ifBlank { "Describe this image." }, image.bytes)
            ResponseEntity.ok(mapOf("reply" to reply.trim()))
        } catch (exception: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                mapOf(
                    "reply" to (exception.message ?: "Unsupported image file.")
                )
            )
        } catch (exception: Exception) {
            ResponseEntity.status(500).body(mapOf("reply" to "Vision service error: ${exception.message}"))
        }
    }

    @GetMapping("/vision/status")
    fun visionStatus(): ResponseEntity<Map<String, Any>> {
        return try {
            val resolvedModel = resolveVisionModelOrNull()
            ResponseEntity.ok(
                mapOf(
                    "service" to "vision",
                    "available" to true,
                    "message" to if (resolvedModel != null) {
                        "vision service is available."
                    } else {
                        "vision service is available, but no vision-capable model is installed."
                    },
                    "model" to (resolvedModel ?: visionOllamaModel.ifBlank { "not configured" }),
                    "ready" to (resolvedModel != null)
                )
            )
        } catch (exception: Exception) {
            ResponseEntity.ok(
                mapOf(
                    "service" to "vision",
                    "available" to false,
                    "message" to "vision service is not ready: ${exception.message}"
                )
            )
        }
    }

    private fun callOllamaVision(prompt: String, imageBytes: ByteArray): String {
        val model = resolveVisionModel()
        val baseUrl = if (visionOllamaUrl.isNotBlank()) visionOllamaUrl else "http://localhost:11434"
        val imageBase64 = Base64.getEncoder().encodeToString(normalizeVisionImage(imageBytes))
        val client = restClientBuilder.build()

        val response = client.post()
            .uri("$baseUrl/api/chat")
            .header("Content-Type", "application/json")
            .body(
                OllamaVisionRequest(
                    model = model,
                    messages = listOf(
                        OllamaVisionMessage(
                            role = "user",
                            content = prompt,
                            images = listOf(imageBase64)
                        )
                    )
                )
            )
            .retrieve()
            .body(Map::class.java)

        val messageObject = response?.get("message") as? Map<*, *>
        return (messageObject?.get("content")?.toString())
            ?: response?.get("response")?.toString()
            ?: "No reply returned."
    }

    private fun normalizeVisionImage(imageBytes: ByteArray): ByteArray {
        val sourceImage = ImageIO.read(ByteArrayInputStream(imageBytes))
            ?: throw IllegalArgumentException(
                "Unsupported image file. Please upload a PNG, JPEG, GIF, WebP, or BMP image."
            )

        val maxDimension = 1024
        val scaleFactor = minOf(
            maxDimension.toDouble() / sourceImage.width.toDouble(),
            maxDimension.toDouble() / sourceImage.height.toDouble(),
            1.0
        )
        val normalizedWidth = max(1, (sourceImage.width * scaleFactor).roundToInt())
        val normalizedHeight = max(1, (sourceImage.height * scaleFactor).roundToInt())
        val targetType = if (sourceImage.colorModel.hasAlpha()) {
            BufferedImage.TYPE_INT_ARGB
        } else {
            BufferedImage.TYPE_INT_RGB
        }

        val normalizedImage = BufferedImage(normalizedWidth, normalizedHeight, targetType)
        val graphics = normalizedImage.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            if (!sourceImage.colorModel.hasAlpha()) {
                graphics.background = Color.WHITE
                graphics.clearRect(0, 0, normalizedWidth, normalizedHeight)
            } else {
                graphics.composite = AlphaComposite.Src
            }
            graphics.drawImage(sourceImage, 0, 0, normalizedWidth, normalizedHeight, null)
        } finally {
            graphics.dispose()
        }

        val outputStream = ByteArrayOutputStream()
        if (!ImageIO.write(normalizedImage, "png", outputStream)) {
            throw IllegalStateException("Unable to encode the uploaded image for vision analysis.")
        }

        return outputStream.toByteArray()
    }

    private fun resolveVisionModel(): String {
        return resolveVisionModelOrNull()
            ?: throw IllegalStateException("No vision-capable Ollama model is installed. Install a vision model and set VISION_OLLAMA_MODEL if needed.")
    }

    private fun resolveVisionModelOrNull(): String? {
        val baseUrl = if (visionOllamaUrl.isNotBlank()) visionOllamaUrl else "http://localhost:11434"
        val client = restClientBuilder.build()

        val response = client.get()
            .uri("$baseUrl/api/tags")
            .retrieve()
            .body(Map::class.java)

        val models = (response?.get("models") as? List<*>)
            .orEmpty()
            .mapNotNull { it as? Map<*, *> }

        val configuredModel = visionOllamaModel.trim()
        if (configuredModel.isNotBlank() && models.any { it["name"]?.toString() == configuredModel || it["model"]?.toString() == configuredModel }) {
            return configuredModel
        }

        val visionModel = models.firstOrNull { model ->
            val topLevelCapabilities = model["capabilities"] as? List<*>
            if (topLevelCapabilities?.any { it?.toString() == "vision" } == true) {
                return@firstOrNull true
            }

            val details = model["details"] as? Map<*, *>
            val nestedCapabilities = details?.get("capabilities") as? List<*>
            nestedCapabilities?.any { it?.toString() == "vision" } == true
        } ?: models.firstOrNull { model ->
            val name = model["name"]?.toString().orEmpty().lowercase()
            name.startsWith("llava") || name.contains("vision")
        }

        if (visionModel != null) {
            return visionModel["name"]?.toString()
                ?: visionModel["model"]?.toString()
                ?: configuredModel.ifBlank { null }
        }

        return null
    }
}
