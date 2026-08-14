package com.example.controller

import org.springframework.web.bind.annotation.*

data class MessageRequest(val name: String, val age: Int, val email: String)
data class MessageResponse(val name: String, val age: Int = 30, val email: String = "")

@RestController
@RequestMapping("/api")
class HelloController {

    @GetMapping("/hello")
    fun hello(): MessageResponse {
        return MessageResponse("Hello, World!")
    }

    @GetMapping("/hello/{name}")
    fun helloName(@PathVariable name: String): MessageResponse {
        return MessageResponse("Hello, $name!")
    }

    @PostMapping("/hello")
    fun helloPost(@RequestBody request: MessageRequest): MessageResponse {
        return MessageResponse(request.name, request.age, request.email)
    }

    @PostMapping("/showJson")
    fun showJson(@RequestBody request: Map<String, Any?>): Map<String, Any?> {
        return request
    }

    @PostMapping("/math/add")
    fun add(@RequestBody request: Map<String, Int>): Map<String, Int> {
        val a = request["a"] ?: 0
        val b = request["b"] ?: 0
        return mapOf("result" to (a + b))
    }

    @GetMapping("/ro/eta")
    fun roEta(): Map<String, Any?> {
        return mapOf(
            "statusCode" to 200,
            "status" to "SUCCESS",
            "requestTrackingId" to "8282360168546010",
            "errorMsg" to mapOf("msg" to "no data available"),
            "data" to mapOf(
                "wasteStreams" to mapOf(
                    "OTHERS" to mapOf(
                        "services" to listOf(
                            mapOf(
                                "customerId" to "000234074863012",
                                "serviceId" to "1",
                                "lineOfBusiness" to "ROLLOFF",
                                "wasteStream" to "DEMO",
                                "category" to "OPEN MARKET",
                                "pickupScheduleInfo" to mapOf(
                                    "schedule" to "At your request",
                                    "pickupDates" to listOf("06-17-2026")
                                ),
                                "eta" to listOf(
                                    mapOf(
                                        "code" to "UnabletoComputeETA",
                                        "message" to "We are gathering information about your route",
                                        "messageHead" to listOf("ETA Coming Soon"),
                                        "ticketNumber" to "263120",
                                        "loadType" to "DEL",
                                        "liveChat" to false
                                    )
                                )
                            )
                        )
                    )
                ),
                "errors" to mapOf(
                    "serverErrors" to emptyList<Any>(),
                    "customerNotFound" to emptyList<Any>()
                )
            ),
            "loggedInStatus" to "loggedIn"
        )
    }
}
