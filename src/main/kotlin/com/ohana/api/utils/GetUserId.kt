package com.ohana.api.utils

import com.ohana.exceptions.AuthorizationException
import io.ktor.server.auth.jwt.JWTPrincipal

fun getUserId(principal: JWTPrincipal?): String =
    principal
        ?.payload
        ?.getClaim("userId")
        ?.asString() ?: throw AuthorizationException("User is not authenticated")
