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
        val validationResult = validate(this)

        return TaskGetAllHandler.Request(
            householdIds = householdIds,
            dueDateFrom = validationResult.dueDateFrom,
            dueDateTo = validationResult.dueDateTo,
            completedDateFrom = validationResult.completedDateFrom,
            completedDateTo = validationResult.completedDateTo,
        )
    }

    companion object {
        private data class ValidationResult(
            val dueDateFrom: Instant?,
            val dueDateTo: Instant?,
            val completedDateFrom: Instant?,
            val completedDateTo: Instant?,
        )

        private fun validate(request: TaskGetAllRequest): ValidationResult {
            val errors = mutableListOf<ValidationError>()

            if (request.householdIds.any { !Guid.isValid(it) }) {
                errors.add(ValidationError(TaskGetAllHandler.Request::householdIds.name, "All household IDs must be valid GUIDs"))
            }

            // Validate due date range
            val dueDateFromInstant = validateAndParseDate(request.dueDateFrom, TaskGetAllHandler.Request::dueDateFrom.name, errors)
            val dueDateToInstant = validateAndParseDate(request.dueDateTo, TaskGetAllHandler.Request::dueDateTo.name, errors)

            // Validate completed date range
            val completedDateFromInstant =
                validateAndParseDate(request.completedDateFrom, TaskGetAllHandler.Request::completedDateFrom.name, errors)
            val completedDateToInstant =
                validateAndParseDate(request.completedDateTo, TaskGetAllHandler.Request::completedDateTo.name, errors)

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

            return ValidationResult(
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
}
