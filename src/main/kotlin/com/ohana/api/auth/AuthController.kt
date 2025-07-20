package com.ohana.api.auth

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ohana.api.auth.models.LoginRequest
import com.ohana.api.auth.models.LogoutRequest
import com.ohana.api.auth.models.RegistrationRequest
import com.ohana.api.auth.models.TokenRefreshRequest
import com.ohana.domain.auth.LoginHandler
import com.ohana.domain.auth.LogoutHandler
import com.ohana.domain.auth.RegistrationHandler
import com.ohana.domain.auth.TokenRefreshHandler
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

class AuthController(
    private val registrationHandler: RegistrationHandler,
    private val loginHandler: LoginHandler,
    private val tokenRefreshHandler: TokenRefreshHandler,
    private val logoutHandler: LogoutHandler,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val objectMapper = jacksonObjectMapper()

    fun Route.registerAuthRoutes() {
        post("/register") {
            val request = call.receive<RegistrationRequest>()
            val domainRequest = request.toDomain()
            val response = registrationHandler.handle(domainRequest)
            call.respond(HttpStatusCode.Created, response)
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val domainRequest = request.toDomain()
            val response = loginHandler.handle(domainRequest)
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
