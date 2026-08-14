package com.example.config

import com.example.tools.UserTool
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class McpToolConfig {

    @Bean
    fun mcpToolCallbacks(userTool: UserTool): ToolCallbackProvider {
        return MethodToolCallbackProvider.builder()
            .toolObjects(userTool)
            .build()
    }
}
