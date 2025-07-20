package com.ohana.api.auth

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ohana.api.auth.models.LogoutRequest
import com.ohana.api.auth.models.MemberRegistrationRequest
import com.ohana.api.auth.models.MemberSignInRequest
import com.ohana.api.auth.models.TokenRefreshRequest
import com.ohana.domain.auth.LogoutHandler
import com.ohana.domain.auth.MemberRegistrationHandler
import com.ohana.domain.auth.MemberSignInHandler
import com.ohana.domain.auth.TokenRefreshHandler
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
            val request = call.receive<MemberRegistrationRequest>()
            val domainRequest = request.toDomain()
            val response = memberRegistrationHandler.handle(domainRequest)
            call.respond(HttpStatusCode.Created, response)
        }

        post("/login") {
            val request = call.receive<MemberSignInRequest>()
            val domainRequest = request.toDomain()
            val response = memberSignInHandler.handle(domainRequest)
            call.respond(HttpStatusCode.OK, response)
        }

        post("/refresh") {
            val request = call.receive<TokenRefreshRequest>()
            val domainRequest = request.toDomain()
            val response = tokenRefreshHandler.handle(domainRequest)
            call.respond(HttpStatusCode.OK, response)
        }

        post("/logout") {
            val request = call.receive<LogoutRequest>()
            val domainRequest = request.toDomain()
            val response = logoutHandler.handle(domainRequest)
            call.respond(HttpStatusCode.OK, response)
        }
    }
}
