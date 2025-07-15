package com.ohana.member.controllers

import com.ohana.exceptions.ValidationError
import com.ohana.exceptions.ValidationException
import com.ohana.member.handlers.*
import com.ohana.shared.ObjectValidator
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class MemberController(
    private val objectValidator: ObjectValidator,
    private val memberGetByIdHandler: MemberGetByIdHandler,
    private val memberGetAllHandler: MemberGetAllHandler,
    private val memberUpdateByIdHandler: MemberUpdateByIdHandler,
) {
    fun Route.registerMemberRoutes() {
        authenticate("auth-jwt") {
            route("/members") {
                get("/{memberId}") {
                    val id =
                        call.parameters["memberId"]
                            ?: throw ValidationException(
                                "Member ID is required",
                                listOf(ValidationError("memberId", "Member ID is required")),
                            )
                    val response = memberGetByIdHandler.handle(id)
                    call.respond(HttpStatusCode.OK, response)
                }

                get("") {
                    val response = memberGetAllHandler.handle()
                    call.respond(HttpStatusCode.OK, response)
                }

                put("/{memberId}") {
                    val id =
                        call.parameters["memberId"]
                            ?: throw ValidationException(
                                "Member ID is required",
                                listOf(ValidationError("memberId", "Member ID is required")),
                            )
                    val request = call.receive<MemberUpdateByIdHandler.Request>()
                    objectValidator.validate(request)

                    val response = memberUpdateByIdHandler.handle(id, request)
                    call.respond(HttpStatusCode.OK, response)
                }
            }
        }
    }
}
