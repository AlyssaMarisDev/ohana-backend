package com.ohana.members.controllers

import com.ohana.exceptions.ValidationException
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
                    val id = call.parameters["id"]?.toIntOrNull() ?: throw ValidationException("Member ID must be an integer")
                    val response = getSingleMemberByIdHandler.handle(id)
                    call.respond(HttpStatusCode.OK, response)
                }

                get("") {
                    val response = getAllMembersHandler.handle()
                    call.respond(HttpStatusCode.OK, response)
                }

                put("/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: throw ValidationException("Member ID must be an integer")
                    val request = call.receive<UpdateMemberByIdHandler.Request>()
                    val response = updateMemberByIdHandler.handle(id, request)
                    call.respond(HttpStatusCode.OK, response)
                }
            }
        }
    }
}
