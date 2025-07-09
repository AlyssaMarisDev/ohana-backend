package com.ohana.members.controllers

import com.ohana.exceptions.ValidationException
import com.ohana.members.handlers.MembersGetAllHandler
import com.ohana.members.handlers.MembersGetByIdHandler
import com.ohana.members.handlers.MembersUpdateByIdHandler
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class MembersController(
    private val membersGetByIdHandler: MembersGetByIdHandler,
    private val membersGetAllHandler: MembersGetAllHandler,
    private val membersUpdateByIdHandler: MembersUpdateByIdHandler,
) {
    fun Route.registerMemberRoutes() {
        authenticate("auth-jwt") {
            route("/members") {
                get("/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: throw ValidationException("Member ID must be an integer")
                    val response = membersGetByIdHandler.handle(id)
                    call.respond(HttpStatusCode.OK, response)
                }

                get("") {
                    val response = membersGetAllHandler.handle()
                    call.respond(HttpStatusCode.OK, response)
                }

                put("/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: throw ValidationException("Member ID must be an integer")
                    val request = call.receive<MembersUpdateByIdHandler.Request>()
                    val response = membersUpdateByIdHandler.handle(id, request)
                    call.respond(HttpStatusCode.OK, response)
                }
            }
        }
    }
}
