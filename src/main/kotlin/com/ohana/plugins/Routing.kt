package com.ohana.plugins

import com.ohana.auth.controllers.AuthController
import com.ohana.health.controllers.HealthController
import com.ohana.household.controllers.HouseholdController
import com.ohana.members.controllers.MembersController
import com.ohana.tasks.controllers.TasksController
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val authController: AuthController by inject()
    val healthController: HealthController by inject()
    val membersController: MembersController by inject()
    val tasksController: TasksController by inject()
    val householdController: HouseholdController by inject()

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
            tasksController.apply {
                registerTaskRoutes()
            }
            householdController.apply {
                registerHouseholdRoutes()
            }
        }
    }
}
