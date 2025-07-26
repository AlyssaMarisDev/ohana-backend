package com.ohana.api.tags.models

import com.ohana.domain.tags.GetTagsHandler
import com.ohana.shared.Guid
import com.ohana.shared.exceptions.ValidationException

data class GetTagsRequest(
    val householdId: String,
) {
    fun toDomain(): GetTagsHandler.Request {
        // Validate householdId
        if (!Guid.isValid(householdId)) {
            throw ValidationException("Invalid householdId format")
        }

        return GetTagsHandler.Request(
            householdId = householdId,
        )
    }
}
