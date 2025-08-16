package com.ohana.api.tags.models

import com.ohana.domain.tags.TagGetAllHandler
import com.ohana.shared.Guid
import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException

data class TagGetAllRequest(
    val householdId: String,
) {
    fun toDomain(): TagGetAllHandler.Request {
        validate(this)

        return TagGetAllHandler.Request(
            householdId = householdId,
        )
    }

    companion object {
        private fun validate(request: TagGetAllRequest) {
            val errors = mutableListOf<ValidationError>()

            if (!Guid.isValid(request.householdId)) {
                errors.add(ValidationError(TagGetAllHandler.Request::householdId.name, "Household ID must be a valid GUID"))
            }

            if (errors.isNotEmpty()) {
                throw ValidationException("Validation failed", errors)
            }
        }
    }
}
