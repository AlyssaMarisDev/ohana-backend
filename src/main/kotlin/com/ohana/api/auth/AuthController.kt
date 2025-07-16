package com.ohana.api.auth

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ohana.domain.auth.MemberRegistrationHandler
import com.ohana.domain.auth.MemberSignInHandler
import com.ohana.plugins.validateAndReceive
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
            // Use annotation-based validation
            val member = call.validateAndReceive<MemberRegistrationHandler.Request>()

            val token = memberRegistrationHandler.handle(member)
            call.respond(HttpStatusCode.Created, token)
        }

        post("/login") {
            // Use annotation-based validation
            val member = call.validateAndReceive<MemberSignInHandler.Request>()
            val token = memberSignInHandler.handle(member)
            call.respond(HttpStatusCode.OK, token)
        }
    }
}
