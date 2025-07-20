package com.ohana.api.task.models

import com.ohana.domain.task.TaskGetAllHandler
import com.ohana.shared.Guid
import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException

data class TaskGetAllRequest(
    val householdIds: List<String>,
) {
    fun toDomain(): TaskGetAllHandler.Request {
        val errors = mutableListOf<ValidationError>()

        // Validate household IDs if provided
        householdIds.forEachIndexed { index, id ->
            if (!Guid.isValid(id)) {
                errors.add(ValidationError("householdIds[$index]", "Household ID must be a valid GUID"))
            }
        }

        if (errors.isNotEmpty()) {
            throw ValidationException("Validation failed", errors)
        }

        return TaskGetAllHandler.Request(householdIds)
    }
}
