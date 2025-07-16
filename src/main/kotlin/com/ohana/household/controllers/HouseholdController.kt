package com.ohana.household.controllers

import com.ohana.exceptions.ValidationError
import com.ohana.exceptions.ValidationException
import com.ohana.household.handlers.*
import com.ohana.plugins.validateAndReceive
import com.ohana.utils.getUserId
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class HouseholdController(
    private val householdCreationHandler: HouseholdCreationHandler,
    private val householdGetByIdHandler: HouseholdGetByIdHandler,
    private val householdGetAllHandler: HouseholdGetAllHandler,
    private val householdInviteMemberHandler: HouseholdInviteMemberHandler,
    private val householdAcceptInviteHandler: HouseholdAcceptInviteHandler,
) {
    fun Route.registerHouseholdRoutes() {
        authenticate("auth-jwt") {
            route("/households") {
                post("") {
                    // Use annotation-based validation
                    val request = call.validateAndReceive<HouseholdCreationHandler.Request>()

                    val userId = getUserId(call.principal<JWTPrincipal>())

                    val response = householdCreationHandler.handle(userId, request)

                    call.respond(HttpStatusCode.Created, response)
                }

                get("/{householdId}") {
                    val userId = getUserId(call.principal<JWTPrincipal>())
                    val id =
                        call.parameters["householdId"]
                            ?: throw ValidationException(
                                "Household ID is required",
                                listOf(ValidationError("householdId", "Household ID is required")),
                            )

                    val response = householdGetByIdHandler.handle(id, userId)
                    call.respond(HttpStatusCode.OK, response)
                }

                get("") {
                    val userId = getUserId(call.principal<JWTPrincipal>())
                    val response = householdGetAllHandler.handle(userId)
                    call.respond(HttpStatusCode.OK, response)
                }

                post("/{householdId}/members") {
                    val userId = getUserId(call.principal<JWTPrincipal>())

                    val id =
                        call.parameters["householdId"]
                            ?: throw ValidationException(
                                "Household ID is required",
                                listOf(ValidationError("householdId", "Household ID is required")),
                            )

                    // Use annotation-based validation
                    val request = call.validateAndReceive<HouseholdInviteMemberHandler.Request>()

                    householdInviteMemberHandler.handle(userId, id, request)
                    call.respond(HttpStatusCode.OK)
                }

                post("/{householdId}/accept-invite") {
                    val userId = getUserId(call.principal<JWTPrincipal>())
                    val id =
                        call.parameters["householdId"]
                            ?: throw ValidationException(
                                "Household ID is required",
                                listOf(ValidationError("householdId", "Household ID is required")),
                            )

                    householdAcceptInviteHandler.handle(userId, id)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
