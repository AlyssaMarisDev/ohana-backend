package com.ohana.api.tags.models

import com.ohana.domain.tags.TagGetAllHandler
import com.ohana.shared.Guid
import com.ohana.shared.exceptions.ValidationException

data class TagGetAllRequest(
    val householdId: String,
) {
    fun toDomain(): TagGetAllHandler.Request {
        // Validate householdId
        if (!Guid.isValid(householdId)) {
            throw ValidationException("Invalid householdId format")
        }

        return TagGetAllHandler.Request(
            householdId = householdId,
        )
    }
}
