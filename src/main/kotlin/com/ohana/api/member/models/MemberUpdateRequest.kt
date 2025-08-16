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
        validate(this)

        return MemberUpdateByIdHandler.Request(
            name = name!!,
            age = age,
            gender = gender,
        )
    }

    companion object {
        private fun validate(request: MemberUpdateRequest) {
            val errors = mutableListOf<ValidationError>()

            if (request.name == null || request.name.isBlank() || request.name.length > 255) {
                errors.add(ValidationError(MemberUpdateByIdHandler.Request::name.name, "Name is required and must be between 1 and 255 characters"))
            }

            if (request.age != null && request.age < 0) {
                errors.add(ValidationError(MemberUpdateByIdHandler.Request::age.name, "Age must be a positive number"))
            }

            if (request.gender != null && request.gender.isBlank()) {
                errors.add(ValidationError(MemberUpdateByIdHandler.Request::gender.name, "Gender cannot be blank if provided"))
            }

            if (errors.isNotEmpty()) {
                throw ValidationException("Validation failed", errors)
            }
        }
    }
}
