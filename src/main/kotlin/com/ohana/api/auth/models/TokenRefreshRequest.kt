package com.ohana.api.auth.models

import com.ohana.domain.auth.TokenRefreshHandler
import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException

data class TokenRefreshRequest(
    val refreshToken: String?,
) {
    fun toDomain(): TokenRefreshHandler.Request {
        val errors = mutableListOf<ValidationError>()

        if (refreshToken == null) {
            errors.add(ValidationError("refreshToken", "Refresh token is required"))
        } else if (refreshToken.isBlank()) {
            errors.add(ValidationError("refreshToken", "Refresh token cannot be blank"))
        }

        if (errors.isNotEmpty()) {
            throw ValidationException("Validation failed", errors)
        }

        return TokenRefreshHandler.Request(
            refreshToken = refreshToken!!,
        )
    }
}
