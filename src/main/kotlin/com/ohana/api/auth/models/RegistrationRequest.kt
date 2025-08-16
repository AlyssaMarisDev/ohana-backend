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
        validate(this)

        return RegistrationHandler.Request(
            name = name!!,
            email = email!!,
            password = password!!,
        )
    }

    companion object {
        private fun validate(request: RegistrationRequest) {
            val errors = mutableListOf<ValidationError>()

            if (request.name == null || request.name.isBlank() || request.name.length < 3) {
                errors.add(ValidationError(RegistrationHandler.Request::name.name, "Name is required and must be at least 3 characters long"))
            }

            if (request.email == null || request.email.isBlank() || !request.email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
                errors.add(ValidationError(RegistrationHandler.Request::email.name, "Email is required and must be in a valid format"))
            }

            if (request.password == null || request.password.isBlank() || request.password.length < 8 ||
                !request.password.matches(Regex(".*[A-Z].*")) || !request.password.matches(Regex(".*[0-9].*"))) {
                errors.add(ValidationError(RegistrationHandler.Request::password.name, "Password is required and must be at least 8 characters long with at least one uppercase letter and one number"))
            }

            if (errors.isNotEmpty()) {
                throw ValidationException("Validation failed", errors)
            }
        }
    }
}
