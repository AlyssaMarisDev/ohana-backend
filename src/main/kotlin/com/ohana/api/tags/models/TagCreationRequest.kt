package com.ohana.api.tags.models

import com.ohana.domain.tags.TagCreationHandler
import com.ohana.shared.exceptions.ValidationException

data class TagCreationRequest(
    val name: String,
    val color: String,
) {
    fun toDomain(): TagCreationHandler.Request {
        // Validate name
        if (name.isBlank()) {
            throw ValidationException("Tag name cannot be empty")
        }
        if (name.length > 50) {
            throw ValidationException("Tag name cannot exceed 50 characters")
        }

        // Validate color (hex color format)
        if (!color.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
            throw ValidationException("Color must be a valid hex color (e.g., #FF0000)")
        }

        return TagCreationHandler.Request(
            name = name.trim(),
            color = color.uppercase(),
        )
    }
}
