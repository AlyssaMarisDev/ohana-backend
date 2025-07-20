package com.ohana.api.auth.models

import com.ohana.domain.auth.LogoutHandler
import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException

data class LogoutRequest(
    val refreshToken: String?,
) {
    fun toDomain(): LogoutHandler.Request {
        val errors = mutableListOf<ValidationError>()

        if (refreshToken == null) {
            errors.add(ValidationError("refreshToken", "Refresh token is required"))
        } else if (refreshToken.isBlank()) {
            errors.add(ValidationError("refreshToken", "Refresh token cannot be blank"))
        }

        if (errors.isNotEmpty()) {
            throw ValidationException("Validation failed", errors)
        }

        return LogoutHandler.Request(
            refreshToken = refreshToken!!,
        )
    }
}
