package com.ohana.api.auth.models

import com.ohana.domain.auth.LogoutHandler
import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException

data class LogoutRequest(
    val refreshToken: String?,
) {
    fun toDomain(): LogoutHandler.Request {
        validate(this)

        return LogoutHandler.Request(
            refreshToken = refreshToken!!,
        )
    }

    companion object {
        private fun validate(request: LogoutRequest) {
            val errors = mutableListOf<ValidationError>()

            if (request.refreshToken == null || request.refreshToken.isBlank()) {
                errors.add(ValidationError(LogoutHandler.Request::refreshToken.name, "Refresh token is required"))
            }

            if (errors.isNotEmpty()) {
                throw ValidationException("Validation failed", errors)
            }
        }
    }
}
