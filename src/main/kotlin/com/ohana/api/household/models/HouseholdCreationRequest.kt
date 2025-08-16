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
        validate(this)

        return HouseholdCreationHandler.Request(
            id = id ?: Guid.generate(),
            name = name!!,
            description = description!!,
        )
    }

    companion object {
        private fun validate(request: HouseholdCreationRequest) {
            val errors = mutableListOf<ValidationError>()

            if (request.id == null || !Guid.isValid(request.id)) {
                errors.add(ValidationError(HouseholdCreationHandler.Request::id.name, "Household ID is required and must be a valid GUID"))
            }

            if (request.name == null || request.name.isBlank() || request.name.length > 255) {
                errors.add(ValidationError(HouseholdCreationHandler.Request::name.name, "Household name is required and must be between 1 and 255 characters"))
            }

            if (request.description == null || request.description.length > 1000) {
                errors.add(ValidationError(HouseholdCreationHandler.Request::description.name, "Household description is required and must be at most 1000 characters long"))
            }

            if (errors.isNotEmpty()) {
                throw ValidationException("Validation failed", errors)
            }
        }
    }
}
