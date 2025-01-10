package com.example

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    embeddedServer(Netty, environment = applicationEngineEnvironment {
        module {
            routing {
                get("/") {
                    call.respondText("Hello, World!")
                }
            }
        }
        connector {
            port = 9090
            host = "0.0.0.0"
        }
    }).start(wait = true)
}
