package com.ohana.api.member.models

import com.ohana.domain.member.MemberUpdateByIdHandler
import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException

data class MemberUpdateRequest(
    val name: String?,
    val age: Int?,
    val gender: String?,
) {
    fun toDomain(): MemberUpdateByIdHandler.Request {
        val errors = mutableListOf<ValidationError>()

        // Validate name
        if (name == null) {
            errors.add(ValidationError("name", "Name is required"))
        } else if (name.isBlank()) {
            errors.add(ValidationError("name", "Name cannot be blank"))
        } else if (name.length > 255) {
            errors.add(ValidationError("name", "Name must be at most 255 characters long"))
        }

        // Validate age (optional field)
        if (age != null && age < 0) {
            errors.add(ValidationError("age", "Age must be a positive number"))
        }

        // Validate gender (optional field)
        if (gender != null && gender.isBlank()) {
            errors.add(ValidationError("gender", "Gender cannot be blank if provided"))
        }

        if (errors.isNotEmpty()) {
            throw ValidationException("Validation failed", errors)
        }

        return MemberUpdateByIdHandler.Request(
            name = name!!,
            age = age,
            gender = gender,
        )
    }
}
