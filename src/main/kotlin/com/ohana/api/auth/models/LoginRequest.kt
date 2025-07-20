package com.ohana.api.auth.models

import com.ohana.domain.auth.LoginHandler
import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException

data class LoginRequest(
    val email: String?,
    val password: String?,
) {
    fun toDomain(): LoginHandler.Request {
        val errors = mutableListOf<ValidationError>()

        if (email == null) {
            errors.add(ValidationError("email", "Email is required"))
        } else if (email.isBlank()) {
            errors.add(ValidationError("email", "Email cannot be blank"))
        } else if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
            errors.add(ValidationError("email", "Invalid email format"))
        }

        if (password == null) {
            errors.add(ValidationError("password", "Password is required"))
        } else if (password.isBlank()) {
            errors.add(ValidationError("password", "Password cannot be blank"))
        }

        if (errors.isNotEmpty()) {
            throw ValidationException("Validation failed", errors)
        }

        return LoginHandler.Request(
            email = email!!,
            password = password!!,
        )
    }
}
