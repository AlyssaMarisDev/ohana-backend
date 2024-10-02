package com.example.plugins

import com.example.data.MemberRepository
import io.ktor.server.application.*
import io.ktor.server.routing.*
import com.example.routes.memberRoutes
import com.example.services.GetSingleMemberService

fun Application.configureRouting() {
    routing {
        route("/api/v1") {
            memberRoutes(GetSingleMemberService(MemberRepository()))
        }
    }
}
