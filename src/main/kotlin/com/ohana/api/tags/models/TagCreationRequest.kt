package com.ohana.api.tags.models

import com.ohana.domain.tags.TagCreationHandler
import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException

data class TagCreationRequest(
    val name: String,
    val color: String,
) {
    fun toDomain(): TagCreationHandler.Request {
        validate(this)

        return TagCreationHandler.Request(
            name = name.trim(),
            color = color.uppercase(),
        )
    }

    companion object {
        private fun validate(request: TagCreationRequest) {
            val errors = mutableListOf<ValidationError>()

            if (request.name.isBlank() || request.name.length > 50) {
                errors.add(ValidationError(TagCreationHandler.Request::name.name, "Tag name is required and must be between 1 and 50 characters"))
            }

            if (!request.color.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                errors.add(ValidationError(TagCreationHandler.Request::color.name, "Color must be a valid hex color (e.g., #FF0000)"))
            }

            if (errors.isNotEmpty()) {
                throw ValidationException("Validation failed", errors)
            }
        }
    }
}
