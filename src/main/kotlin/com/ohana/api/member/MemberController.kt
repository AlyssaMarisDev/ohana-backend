package com.ohana.api.member

import com.ohana.api.member.models.MemberUpdateRequest
import com.ohana.api.utils.getUserId
import com.ohana.domain.member.MemberGetAllHandler
import com.ohana.domain.member.MemberGetByIdHandler
import com.ohana.domain.member.MemberUpdateByIdHandler
import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class MemberController(
    private val memberGetByIdHandler: MemberGetByIdHandler,
    private val memberGetAllHandler: MemberGetAllHandler,
    private val memberUpdateByIdHandler: MemberUpdateByIdHandler,
) {
    fun Route.registerMemberRoutes() {
        authenticate("auth-jwt") {
            route("") {
                get("/members/{memberId}") {
                    val id =
                        call.parameters["memberId"]
                            ?: throw ValidationException(
                                "Member ID is required",
                                listOf(ValidationError("memberId", "Member ID is required")),
                            )

                    val userId = getUserId(call.principal())
                    val response = memberGetByIdHandler.handle(userId, id)
                    call.respond(HttpStatusCode.OK, response)
                }

                put("/members/{memberId}") {
                    val id =
                        call.parameters["memberId"]
                            ?: throw ValidationException(
                                "Member ID is required",
                                listOf(ValidationError("memberId", "Member ID is required")),
                            )

                    val request = call.receive<MemberUpdateRequest>()
                    val domainRequest = request.toDomain()

                    val userId = getUserId(call.principal())
                    val response = memberUpdateByIdHandler.handle(userId, id, domainRequest)
                    call.respond(HttpStatusCode.OK, response)
                }
            }

            route("/households/{householdId}/members") {
                get("") {
                    val householdId =
                        call.parameters["householdId"]
                            ?: throw ValidationException(
                                "Household ID is required",
                                listOf(ValidationError("householdId", "Household ID is required")),
                            )

                    val userId = getUserId(call.principal())
                    val response = memberGetAllHandler.handle(userId, householdId)
                    call.respond(HttpStatusCode.OK, response)
                }
            }
        }
    }
}
