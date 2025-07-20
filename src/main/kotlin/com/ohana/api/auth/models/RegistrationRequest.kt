package com.ohana.api.auth.models

import com.ohana.domain.auth.RegistrationHandler
import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException

data class RegistrationRequest(
    val name: String?,
    val email: String?,
    val password: String?,
) {
    fun toDomain(): RegistrationHandler.Request {
        val errors = mutableListOf<ValidationError>()

        if (name == null) {
            errors.add(ValidationError("name", "Name is required"))
        } else if (name.isBlank()) {
            errors.add(ValidationError("name", "Name cannot be blank"))
        } else if (name.length < 3) {
            errors.add(ValidationError("name", "Name must be at least 3 characters long"))
        }

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
        } else if (password.length < 8) {
            errors.add(ValidationError("password", "Password must be at least 8 characters long"))
        } else if (!password.matches(Regex(".*[A-Z].*"))) {
            errors.add(ValidationError("password", "Password must contain at least one uppercase letter"))
        } else if (!password.matches(Regex(".*[0-9].*"))) {
            errors.add(ValidationError("password", "Password must contain at least one number"))
        } else if (!password.matches(Regex(".*[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\|,.<>\\/?].*"))) {
            errors.add(ValidationError("password", "Password must contain at least one special character"))
        }

        if (errors.isNotEmpty()) {
            throw ValidationException("Validation failed", errors)
        }

        return RegistrationHandler.Request(
            name = name!!,
            email = email!!,
            password = password!!,
        )
    }
}
