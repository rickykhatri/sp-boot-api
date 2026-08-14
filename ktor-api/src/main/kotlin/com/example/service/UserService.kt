package com.example.service

import com.example.entity.User
import com.example.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(private val userRepository: UserRepository) {

    fun getAllUsers(): List<User> = userRepository.findAll()

    fun getUserById(id: Long): User? = userRepository.findById(id).orElse(null)

    fun createUser(user: User): User = userRepository.save(user)

    fun updateUser(id: Long, updated: User): User? {
        if (!userRepository.existsById(id)) return null
        return userRepository.save(updated.copy(id = id))
    }

    fun deleteUser(id: Long): Boolean {
        if (!userRepository.existsById(id)) return false
        userRepository.deleteById(id)
        return true
    }
}
