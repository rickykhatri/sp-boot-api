
package com.example.tools

import com.example.entity.User
import com.example.service.UserService
import org.springframework.stereotype.Component
import org.springframework.ai.tool.annotation.Tool


@Component
class UserTool(private val userService: UserService) {
    
    /**
     * Returns a string representation of all users in the database, 
     * formatted as "ID: {id}, Name: {name}, Email: {email}" for each user.
     */
    @Tool(description = "Fetch all user info.")
    fun getUserInfo(): String {
        return userService.getAllUsers().joinToString(separator = "\n") { user: User ->
            "ID: ${user.id}, Name: ${user.name}, Email: ${user.email}"
        }
    }
}