package com.ohana.household.controllers

import com.ohana.exceptions.ValidationException
import com.ohana.household.handlers.*
import com.ohana.utils.getUserId
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class HouseholdController(
    private val householdAcceptInviteHandler: HouseholdAcceptInviteHandler,
    private val householdCreationHandler: HouseholdCreationHandler,
    private val householdGetAllHandler: HouseholdGetAllHandler,
    private val householdGetByIdHandler: HouseholdGetByIdHandler,
    private val householdInviteMemberHandler: HouseholdInviteMemberHandler,
) {
    fun Route.registerHouseholdRoutes() {
        authenticate("auth-jwt") {
            route("/households") {
                post("") {
                    val request = call.receive<HouseholdCreationHandler.Request>()
                    println("Received request: $request")

                    val userId = getUserId(call.principal<JWTPrincipal>())

                    val response = householdCreationHandler.handle(userId, request)

                    call.respond(HttpStatusCode.OK, response)
                }

                get("/{id}") {
                    val id = call.parameters["id"] ?: throw ValidationException("Household ID is required")
                    val response = householdGetByIdHandler.handle(id)
                    call.respond(HttpStatusCode.OK, response)
                }

                get("") {
                    val response = householdGetAllHandler.handle()
                    call.respond(HttpStatusCode.OK, response)
                }

                post("/{id}/members") {
                    val userId = getUserId(call.principal<JWTPrincipal>())

                    val id = call.parameters["id"] ?: throw ValidationException("Household ID is required")
                    val request = call.receive<HouseholdInviteMemberHandler.Request>()
                    householdInviteMemberHandler.handle(userId, id, request)
                    call.respond(HttpStatusCode.OK)
                }

                post("/{id}/accept-invite") {
                    val userId = getUserId(call.principal<JWTPrincipal>())
                    val id = call.parameters["id"] ?: throw ValidationException("Household ID is required")
                    householdAcceptInviteHandler.handle(userId, id)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
