package com.ohana.api.task.models

import com.ohana.domain.task.TaskGetAllHandler
import com.ohana.shared.Guid
import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException
import java.time.Instant

data class TaskGetAllRequest(
    val householdIds: List<String>,
    val dueDateFrom: String? = null,
    val dueDateTo: String? = null,
    val completedDateFrom: String? = null,
    val completedDateTo: String? = null,
) {
    fun toDomain(): TaskGetAllHandler.Request {
        val errors = mutableListOf<ValidationError>()

        householdIds.forEachIndexed { index, id ->
            if (!Guid.isValid(id)) {
                errors.add(ValidationError("householdIds[$index]", "Household ID must be a valid GUID"))
            }
        }

        // Validate due date range
        val dueDateFromInstant = validateAndParseDate(dueDateFrom, "dueDateFrom", errors)
        val dueDateToInstant = validateAndParseDate(dueDateTo, "dueDateTo", errors)

        // Validate completed date range
        val completedDateFromInstant = validateAndParseDate(completedDateFrom, "completedDateFrom", errors)
        val completedDateToInstant = validateAndParseDate(completedDateTo, "completedDateTo", errors)

        // Validate date range logic
        if (dueDateFromInstant != null && dueDateToInstant != null && dueDateFromInstant.isAfter(dueDateToInstant)) {
            errors.add(ValidationError("dueDateRange", "Due date 'from' must be before or equal to 'to'"))
        }

        if (completedDateFromInstant != null &&
            completedDateToInstant != null &&
            completedDateFromInstant.isAfter(completedDateToInstant)
        ) {
            errors.add(ValidationError("completedDateRange", "Completed date 'from' must be before or equal to 'to'"))
        }

        if (errors.isNotEmpty()) {
            throw ValidationException("Validation failed", errors)
        }

        return TaskGetAllHandler.Request(
            householdIds = householdIds,
            dueDateFrom = dueDateFromInstant,
            dueDateTo = dueDateToInstant,
            completedDateFrom = completedDateFromInstant,
            completedDateTo = completedDateToInstant,
        )
    }

    private fun validateAndParseDate(
        dateString: String?,
        fieldName: String,
        errors: MutableList<ValidationError>,
    ): Instant? {
        if (dateString == null) return null

        return try {
            Instant.parse(dateString)
        } catch (e: Exception) {
            errors.add(ValidationError(fieldName, "Date must be in ISO 8601 format (e.g., 2023-12-01T10:00:00Z)"))
            null
        }
    }
}
