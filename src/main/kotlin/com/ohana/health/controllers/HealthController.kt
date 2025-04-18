package com.ohana.health.controllers

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.HttpStatusCode
import org.koin.core.component.KoinComponent
import io.ktor.server.auth.*

class HealthController {
    fun Route.registerHealthRoutes() {
        get("/health") {
            call.respond(HttpStatusCode.OK, "OK")
        }
    }
}
