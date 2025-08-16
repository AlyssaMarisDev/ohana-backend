package com.ohana.api.auth.models

import com.ohana.domain.auth.LoginHandler
import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException

data class LoginRequest(
    val email: String?,
    val password: String?,
) {
    fun toDomain(): LoginHandler.Request {
        validate(this)

        return LoginHandler.Request(
            email = email!!,
            password = password!!,
        )
    }

    companion object {
        private fun validate(request: LoginRequest) {
            val errors = mutableListOf<ValidationError>()

            if (request.email == null || request.email.isBlank() || !request.email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
                errors.add(ValidationError(LoginHandler.Request::email.name, "Email is required and must be in a valid format"))
            }

            if (request.password == null || request.password.isBlank()) {
                errors.add(ValidationError(LoginHandler.Request::password.name, "Password is required"))
            }

            if (errors.isNotEmpty()) {
                throw ValidationException("Validation failed", errors)
            }
        }
    }
}
