package com.example.controller

import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/mcp")
class McpSmokeController(
    private val toolCallbackProviders: List<ToolCallbackProvider>
) {

    @GetMapping("/smoke")
    fun smoke(): Map<String, Any> {
        val toolNames = toolCallbackProviders
            .flatMap { provider -> provider.toolCallbacks.toList() }
            .map { callback -> callback.toolDefinition.name() }
            .distinct()
            .sorted()

        return mapOf(
            "status" to "UP",
            "toolProviderCount" to toolCallbackProviders.size,
            "toolCount" to toolNames.size,
            "tools" to toolNames
        )
    }
}
