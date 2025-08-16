package com.ohana.api.tags.models

import com.ohana.domain.tags.TagUpdateHandler
import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException

data class TagUpdateRequest(
    val name: String,
    val color: String,
) {
    fun toDomain(): TagUpdateHandler.Request {
        validate(this)

        return TagUpdateHandler.Request(
            name = name.trim(),
            color = color.uppercase(),
        )
    }

    companion object {
        private fun validate(request: TagUpdateRequest) {
            val errors = mutableListOf<ValidationError>()

            if (request.name.isBlank() || request.name.length > 50) {
                errors.add(ValidationError(TagUpdateHandler.Request::name.name, "Tag name is required and must be between 1 and 50 characters"))
            }

            if (!request.color.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                errors.add(ValidationError(TagUpdateHandler.Request::color.name, "Color must be a valid hex color (e.g., #FF0000)"))
            }

            if (errors.isNotEmpty()) {
                throw ValidationException("Validation failed", errors)
            }
        }
    }
}
