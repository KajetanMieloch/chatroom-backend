package com.example

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import io.ktor.server.plugins.callloging.*

fun main() {
    embeddedServer(Netty, port = 9090, host = "0.0.0.0") {
        install(CallLogging) // Enable request logging
        routing {
            get("/") {
                call.respondText("Hello, World!")
            }
            post("/createRoom") {
                try {
                    val request = call.receive<CreateRoomRequest>() // Parse the JSON request
                    val response = JsonObject(
                        mapOf(
                            "status" to JsonPrimitive("success"),
                            "roomName" to JsonPrimitive(request.roomName),
                            "message" to JsonPrimitive("Room created successfully!")
                        )
                    )
                    call.respondText(response.toString(), ContentType.Application.Json)
                } catch (e: Exception) {
                    call.application.log.error("Error parsing request: ${e.localizedMessage}")
                    call.respond(HttpStatusCode.BadRequest, "Invalid Request")
                }
            }
        }
    }.start(wait = true)
}


@Serializable
data class CreateRoomRequest(
    val roomName: String,
    val password: String? = null
)
