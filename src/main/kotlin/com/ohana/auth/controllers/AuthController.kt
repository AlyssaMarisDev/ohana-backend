package com.ohana.auth.controllers

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.ohana.auth.handlers.RegisterNewMemberHandler
import com.ohana.auth.handlers.MemberSignInHandler
import io.ktor.server.request.*
import io.ktor.http.*
import com.ohana.auth.exceptions.AuthorizationException
import org.koin.core.component.KoinComponent

class AuthController(
    private val registerNewMemberHandler: RegisterNewMemberHandler,
    private val memberSignInHandler: MemberSignInHandler
) {
    fun Route.registerAuthRoutes() {
        post("/register") {
            val member = call.receive<RegisterNewMemberHandler.Request>()
            val token = registerNewMemberHandler.handle(member)
            call.respond(HttpStatusCode.Created, token)
        }

        post("/login") {
            val member = call.receive<MemberSignInHandler.Request>()

            try {
                val token = memberSignInHandler.handle(member)
                call.respond(HttpStatusCode.OK, token)
            } catch (e: AuthorizationException) {
                call.respond(HttpStatusCode.Unauthorized, e.message ?: "Invalid email or password")
            }
        }
    }
}
