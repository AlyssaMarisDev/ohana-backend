package com.ohana.health.controllers

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class HealthController {
    fun Route.registerHealthRoutes() {
        get("/health") { call.respond(HttpStatusCode.OK, "OK") }
    }
}
