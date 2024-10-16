package com.example.controllers

import com.example.services.GetSingleMemberService
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class MembersController(
    private val getSingleMemberService: GetSingleMemberService
) {
    fun Route.registerMemberRoutes() {
        route("/members") {
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                val member = id?.let { getSingleMemberService.run(it) }
                if (member != null) {
                    call.respond(member)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Member not found")
                }
            }
        }
    }
}

