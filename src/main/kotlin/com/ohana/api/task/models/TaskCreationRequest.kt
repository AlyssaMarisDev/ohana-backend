package com.ohana.api.task.models

import com.ohana.domain.task.TaskCreationHandler
import com.ohana.shared.Guid
import com.ohana.shared.enums.TaskStatus
import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException
import java.time.Instant

data class TaskCreationRequest(
    val id: String? = Guid.generate(),
    val title: String?,
    val description: String?,
    val dueDate: Instant?,
    val status: String?,
    val householdId: String?,
) {
    fun toDomain(): TaskCreationHandler.Request {
        val errors = mutableListOf<ValidationError>()

        if (id == null) {
            errors.add(ValidationError("id", "Task ID is required"))
        } else if (id.isBlank()) {
            errors.add(ValidationError("id", "Task ID cannot be blank"))
        } else if (!Guid.isValid(id)) {
            errors.add(ValidationError("id", "Task ID must be a valid GUID"))
        }

        if (title == null) {
            errors.add(ValidationError("title", "Title is required"))
        } else if (title.isBlank()) {
            errors.add(ValidationError("title", "Title cannot be blank"))
        } else if (title.length > 255) {
            errors.add(ValidationError("title", "Title must be at most 255 characters long"))
        }

        if (description == null) {
            errors.add(ValidationError("description", "Description is required"))
        } else if (description.length > 1000) {
            errors.add(ValidationError("description", "Description must be at most 1000 characters long"))
        }

        if (dueDate == null) {
            errors.add(ValidationError("dueDate", "Due date is required"))
        }

        if (status == null) {
            errors.add(ValidationError("status", "Status is required"))
        } else {
            try {
                TaskStatus.valueOf(status)
            } catch (e: IllegalArgumentException) {
                errors.add(ValidationError("status", "Status must be one of: PENDING, IN_PROGRESS, COMPLETED"))
            }
        }

        if (householdId == null) {
            errors.add(ValidationError("householdId", "Household ID is required"))
        } else if (householdId.isBlank()) {
            errors.add(ValidationError("householdId", "Household ID cannot be blank"))
        } else if (!Guid.isValid(householdId)) {
            errors.add(ValidationError("householdId", "Household ID must be a valid GUID"))
        }

        if (errors.isNotEmpty()) {
            throw ValidationException("Validation failed", errors)
        }

        return TaskCreationHandler.Request(
            id = id ?: Guid.generate(),
            title = title!!,
            description = description!!,
            dueDate = dueDate!!,
            status = TaskStatus.valueOf(status!!),
            householdId = householdId!!,
        )
    }
}
