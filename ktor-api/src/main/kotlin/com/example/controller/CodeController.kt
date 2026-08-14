package com.example.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

data class KotlinRunRequest(
    val code: String
)

data class KotlinRunResponse(
    val success: Boolean,
    val output: String,
    val error: String? = null
)

@RestController
@RequestMapping("/api/code")
class CodeController {

    @PostMapping("/run")
    fun runKotlin(@RequestBody request: KotlinRunRequest): ResponseEntity<KotlinRunResponse> {
        val code = request.code.trim()
        if (code.isBlank()) {
            return ResponseEntity.badRequest().body(KotlinRunResponse(false, "", "Code cannot be empty."))
        }

        if (code.length > 6000) {
            return ResponseEntity.badRequest().body(KotlinRunResponse(false, "", "Code is too long."))
        }

        val blockedPatterns = listOf(
            "System.exit",
            "Runtime.getRuntime",
            "ProcessBuilder",
            "java.io.",
            "java.nio.file.",
            "File(",
            "Files.",
            "Thread.sleep",
            "exec("
        )

        if (blockedPatterns.any { code.contains(it) }) {
            return ResponseEntity.status(400).body(
                KotlinRunResponse(
                    success = false,
                    output = "",
                    error = "This runner only allows simple Kotlin snippets and blocks file, process, and system calls."
                )
            )
        }

        val executor = Executors.newSingleThreadExecutor()
        return try {
            val future = executor.submit<KotlinRunResponse> {
                val engine = createKotlinScriptEngine()
                    ?: return@submit KotlinRunResponse(false, "", "Kotlin script engine is not available.")

                executeScript(engine, code)
            }

            val result = future.get(4, TimeUnit.SECONDS)
            ResponseEntity.ok(result)
        } catch (exception: Exception) {
            ResponseEntity.status(500).body(
                KotlinRunResponse(
                    success = false,
                    output = "",
                    error = exception.message ?: "Unable to run Kotlin code."
                )
            )
        } finally {
            executor.shutdownNow()
        }
    }

    private fun createKotlinScriptEngine(): ScriptEngine? {
        val manager = ScriptEngineManager()
        return manager.getEngineByName("kotlin")
            ?: manager.getEngineByExtension("kts")
    }

    private fun executeScript(engine: ScriptEngine, code: String): KotlinRunResponse {
        val stdout = ByteArrayOutputStream()
        val originalOut = System.out
        val hasMainFunction = Regex("fun\\s+main\\s*\\(", RegexOption.IGNORE_CASE).containsMatchIn(code)

        return try {
            System.setOut(PrintStream(stdout, true, Charsets.UTF_8))
            val result = engine.eval(code)
            val mainResult = if (hasMainFunction) {
                runCatching { engine.eval("main()") }.getOrNull()
            } else {
                null
            }
            System.out.flush()

            val outputText = buildString {
                val printed = stdout.toString(Charsets.UTF_8)
                if (printed.isNotBlank()) {
                    append(printed.trimEnd())
                }

                if (result != null && result.toString() != "kotlin.Unit") {
                    if (isNotEmpty()) {
                        appendLine()
                    }
                    append(result.toString())
                }

                if (mainResult != null && mainResult.toString() != "kotlin.Unit") {
                    if (isNotEmpty()) {
                        appendLine()
                    }
                    append(mainResult.toString())
                }
            }.trim()

            KotlinRunResponse(true, outputText.ifBlank { "No output returned." })
        } catch (exception: Exception) {
            KotlinRunResponse(false, stdout.toString(Charsets.UTF_8).trim(), exception.message ?: "Execution failed.")
        } finally {
            System.setOut(originalOut)
        }
    }
}
