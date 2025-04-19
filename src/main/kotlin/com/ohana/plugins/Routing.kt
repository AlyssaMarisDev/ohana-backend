package com.ohana.plugins

import com.ohana.auth.controllers.AuthController
import com.ohana.health.controllers.HealthController
import com.ohana.members.controllers.MembersController
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val membersController: MembersController by inject()
    val healthController: HealthController by inject()
    val authController: AuthController by inject()

    routing {
        route("/api/v1") {
            membersController.apply {
                registerMemberRoutes()
            }
            healthController.apply {
                registerHealthRoutes()
            }
            authController.apply {
                registerAuthRoutes()
            }
        }
    }
}
