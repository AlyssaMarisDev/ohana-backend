package com.ohana.plugins

import com.ohana.members.controllers.MembersController
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val membersController: MembersController by inject()

    routing {
        route("/api/v1") {
            membersController.apply {
                registerMemberRoutes()
            }
        }
    }
}
