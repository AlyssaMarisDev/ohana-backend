package com.ohana.api.auth.models

import com.ohana.domain.auth.TokenRefreshHandler
import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException

data class TokenRefreshRequest(
    val refreshToken: String?,
) {
    fun toDomain(): TokenRefreshHandler.Request {
        validate(this)

        return TokenRefreshHandler.Request(
            refreshToken = refreshToken!!,
        )
    }

    companion object {
        private fun validate(request: TokenRefreshRequest) {
            val errors = mutableListOf<ValidationError>()

            if (request.refreshToken == null || request.refreshToken.isBlank()) {
                errors.add(ValidationError(TokenRefreshHandler.Request::refreshToken.name, "Refresh token is required"))
            }

            if (errors.isNotEmpty()) {
                throw ValidationException("Validation failed", errors)
            }
        }
    }
}
