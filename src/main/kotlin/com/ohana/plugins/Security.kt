package com.ohana.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureSecurity() {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "Ohana"
            verifier(
                JWT
                    .require(Algorithm.HMAC256("secret"))
                    .withAudience("Ohana")
                    .withIssuer("https://ohana.com")
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains("Ohana")) JWTPrincipal(credential.payload) else null
            }
        }
    }
}
