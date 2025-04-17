package com.ohana.members.controllers

import com.ohana.members.handlers.GetSingleMemberByIdHandler
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.component.KoinComponent

class MembersController(
    private val getSingleMemberByIdHandler: GetSingleMemberByIdHandler
) {
    fun Route.registerMemberRoutes() {
        route("/members") {
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                val response = id?.let { getSingleMemberByIdHandler.handle(it) }
                if (response != null) {
                    call.respond(response)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Member not found")
                }
            }
        }
    }
}

