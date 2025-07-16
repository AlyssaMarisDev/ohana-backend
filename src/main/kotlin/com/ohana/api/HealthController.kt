package com.ohana.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

class HealthController {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun Route.registerHealthRoutes() {
        get("/health") {
            logger.info("Health check received")
            call.respond(HttpStatusCode.OK, "OK")
        }
    }
}
