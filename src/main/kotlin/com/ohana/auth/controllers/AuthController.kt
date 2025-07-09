package com.ohana.auth.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ohana.auth.handlers.MemberRegistrationHandler
import com.ohana.auth.handlers.MemberSignInHandler
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

class AuthController(
    private val memberRegistrationHandler: MemberRegistrationHandler,
    private val memberSignInHandler: MemberSignInHandler,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val objectMapper = jacksonObjectMapper()

    fun Route.registerAuthRoutes() {
        post("/register") {
            val member = call.receive<MemberRegistrationHandler.Request>()
            val errors = member.validate()

            if (errors.isNotEmpty()) {
                call.respond(HttpStatusCode.BadRequest, errors)
                return@post
            }

            val token = memberRegistrationHandler.handle(member)
            call.respond(HttpStatusCode.Created, token)
        }

        post("/login") {
            val member = call.receive<MemberSignInHandler.Request>()
            val token = memberSignInHandler.handle(member)
            call.respond(HttpStatusCode.OK, token)
        }
    }
}
