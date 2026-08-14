package com.example.controller

data class ImageGenerationRequest(
    val prompt: String
)

data class OllamaVisionMessage(
    val role: String,
    val content: String,
    val images: List<String>
)

data class OllamaVisionRequest(
    val model: String,
    val messages: List<OllamaVisionMessage>,
    val stream: Boolean = false
)

data class StableDiffusionTxt2ImgRequest(
    val prompt: String,
    val steps: Int = 20,
    val width: Int = 512,
    val height: Int = 512
)
