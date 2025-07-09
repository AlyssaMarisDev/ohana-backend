package com.ohana.household.controllers

import com.ohana.exceptions.ValidationException
import com.ohana.household.handlers.HouseholdCreationHandler
import com.ohana.household.handlers.HouseholdGetAllHandler
import com.ohana.household.handlers.HouseholdGetByIdHandler
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class HouseholdController(
    private val householdCreationHandler: HouseholdCreationHandler,
    private val householdGetAllHandler: HouseholdGetAllHandler,
    private val householdGetByIdHandler: HouseholdGetByIdHandler,
) {
    fun Route.registerHouseholdRoutes() {
        authenticate("auth-jwt") {
            route("/households") {
                post("") {
                    val request = call.receive<HouseholdCreationHandler.Request>()
                    println("Received request: $request")

                    val userId =
                        call
                            .principal<JWTPrincipal>()
                            ?.payload
                            ?.getClaim("userId")
                            ?.asString()
                            ?: throw ValidationException("User ID is required")

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
            }
        }
    }
}
