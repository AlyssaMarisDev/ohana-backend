package com.ohana.members.controllers

import com.ohana.members.handlers.GetAllMembersHandler
import com.ohana.members.handlers.GetSingleMemberByIdHandler
import com.ohana.members.handlers.UpdateMemberByIdHandler
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class MembersController(
    private val getSingleMemberByIdHandler: GetSingleMemberByIdHandler,
    private val getAllMembersHandler: GetAllMembersHandler,
    private val updateMemberByIdHandler: UpdateMemberByIdHandler,
) {
    fun Route.registerMemberRoutes() {
        authenticate("auth-jwt") {
            route("/members") {
                get("/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull()
                    val response = id?.let { getSingleMemberByIdHandler.handle(it) }
                    if (response != null) {
                        call.respond(HttpStatusCode.OK, response)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Member not found")
                    }
                }

                get("") {
                    val response = getAllMembersHandler.handle()
                    call.respond(HttpStatusCode.OK, response)
                }

                put("/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull()
                    val request = call.receive<UpdateMemberByIdHandler.Request>()
                    val response = id?.let { updateMemberByIdHandler.handle(it, request) }
                    if (response != null) {
                        call.respond(HttpStatusCode.OK, response)
                    }
                }
            }
        }
    }
}
