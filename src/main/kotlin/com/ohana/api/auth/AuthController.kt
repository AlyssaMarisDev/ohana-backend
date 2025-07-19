package com.ohana.api.auth

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ohana.domain.auth.LogoutHandler
import com.ohana.domain.auth.MemberRegistrationHandler
import com.ohana.domain.auth.MemberSignInHandler
import com.ohana.domain.auth.TokenRefreshHandler
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
    private val tokenRefreshHandler: TokenRefreshHandler,
    private val logoutHandler: LogoutHandler,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val objectMapper = jacksonObjectMapper()

    fun Route.registerAuthRoutes() {
        post("/register") {
            // Use annotation-based validation
            val member = call.validateAndReceive<MemberRegistrationHandler.Request>()

            val response = memberRegistrationHandler.handle(member)
            call.respond(HttpStatusCode.Created, response)
        }

        post("/login") {
            // Use annotation-based validation
            val member = call.validateAndReceive<MemberSignInHandler.Request>()
            val response = memberSignInHandler.handle(member)
            call.respond(HttpStatusCode.OK, response)
        }

        post("/refresh") {
            // Use annotation-based validation
            val request = call.validateAndReceive<TokenRefreshHandler.Request>()
            val response = tokenRefreshHandler.handle(request)
            call.respond(HttpStatusCode.OK, response)
        }

        post("/logout") {
            // Use annotation-based validation
            val request = call.validateAndReceive<LogoutHandler.Request>()
            val response = logoutHandler.handle(request)
            call.respond(HttpStatusCode.OK, response)
        }
    }
}
