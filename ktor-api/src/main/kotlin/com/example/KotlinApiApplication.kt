package com.example

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KotlinApiApplication

fun main(args: Array<String>) {
	runApplication<KotlinApiApplication>(*args)
}
