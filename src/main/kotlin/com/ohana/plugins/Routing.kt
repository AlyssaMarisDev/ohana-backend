package com.ohana.plugins

import com.ohana.auth.controllers.AuthController
import com.ohana.health.controllers.HealthController
import com.ohana.household.controllers.HouseholdController
import com.ohana.member.controllers.MemberController
import com.ohana.task.controllers.TaskController
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val authController: AuthController by inject()
    val healthController: HealthController by inject()
    val memberController: MemberController by inject()
    val taskController: TaskController by inject()
    val householdController: HouseholdController by inject()

    routing {
        route("/api/v1") {
            memberController.apply {
                registerMemberRoutes()
            }
            healthController.apply {
                registerHealthRoutes()
            }
            authController.apply {
                registerAuthRoutes()
            }
            taskController.apply {
                registerTaskRoutes()
            }
            householdController.apply {
                registerHouseholdRoutes()
            }
        }
    }
}
