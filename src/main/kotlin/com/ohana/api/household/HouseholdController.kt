package com.ohana.api.household

import com.ohana.api.household.models.HouseholdCreationRequest
import com.ohana.api.household.models.HouseholdInviteMemberRequest
import com.ohana.api.utils.getUserId
import com.ohana.domain.household.HouseholdAcceptInviteHandler
import com.ohana.domain.household.HouseholdCreationHandler
import com.ohana.domain.household.HouseholdGetAllHandler
import com.ohana.domain.household.HouseholdGetByIdHandler
import com.ohana.domain.household.HouseholdInviteMemberHandler
import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException
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
                    val request = call.receive<HouseholdCreationRequest>()
                    val domainRequest = request.toDomain()

                    val userId = getUserId(call.principal<JWTPrincipal>())

                    val response = householdCreationHandler.handle(userId, domainRequest)

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

                    val response = householdGetByIdHandler.handle(userId, id)
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

                    val request = call.receive<HouseholdInviteMemberRequest>()
                    val domainRequest = request.toDomain()

                    householdInviteMemberHandler.handle(userId, id, domainRequest)
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
