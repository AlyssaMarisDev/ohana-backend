package com.ohana.utils

import com.ohana.exceptions.ValidationException
import io.ktor.server.auth.jwt.JWTPrincipal

fun getUserId(principal: JWTPrincipal?): String =
    principal
        ?.payload
        ?.getClaim("userId")
        ?.asString() ?: throw ValidationException("User ID is required")
