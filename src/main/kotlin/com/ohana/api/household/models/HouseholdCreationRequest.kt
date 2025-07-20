package com.ohana.api.household.models

import com.ohana.domain.household.HouseholdCreationHandler
import com.ohana.shared.Guid
import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException

data class HouseholdCreationRequest(
    val id: String? = Guid.generate(),
    val name: String?,
    val description: String?,
) {
    fun toDomain(): HouseholdCreationHandler.Request {
        val errors = mutableListOf<ValidationError>()

        if (id == null) {
            errors.add(ValidationError("id", "Household ID is required"))
        } else if (id.isBlank()) {
            errors.add(ValidationError("id", "Household ID cannot be blank"))
        } else if (!Guid.isValid(id)) {
            errors.add(ValidationError("id", "Household ID must be a valid GUID"))
        }

        if (name == null) {
            errors.add(ValidationError("name", "Household name is required"))
        } else if (name.isBlank()) {
            errors.add(ValidationError("name", "Household name cannot be blank"))
        } else if (name.length > 255) {
            errors.add(ValidationError("name", "Household name must be at most 255 characters long"))
        }

        if (description == null) {
            errors.add(ValidationError("description", "Household description is required"))
        } else if (description.isBlank()) {
            errors.add(ValidationError("description", "Household description cannot be blank"))
        } else if (description.length > 1000) {
            errors.add(ValidationError("description", "Household description must be at most 1000 characters long"))
        }

        if (errors.isNotEmpty()) {
            throw ValidationException("Validation failed", errors)
        }

        return HouseholdCreationHandler.Request(
            id = id ?: Guid.generate(),
            name = name!!,
            description = description!!,
        )
    }
}
