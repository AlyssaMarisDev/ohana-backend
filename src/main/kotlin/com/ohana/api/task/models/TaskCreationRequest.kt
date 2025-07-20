package com.ohana.api.task.models

import com.ohana.domain.task.TaskCreationHandler
import com.ohana.shared.enums.TaskStatus
import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException
import java.time.Instant
import java.util.UUID

data class TaskCreationRequest(
    val title: String?,
    val description: String?,
    val dueDate: Instant?,
    val status: String?,
) {
    fun toDomain(): TaskCreationHandler.Request {
        val errors = mutableListOf<ValidationError>()

        // Validate title
        if (title == null) {
            errors.add(ValidationError("title", "Title cannot be blank"))
        } else if (title.isBlank()) {
            errors.add(ValidationError("title", "Title cannot be blank"))
        } else if (title.length > 255) {
            errors.add(ValidationError("title", "Title must be at most 255 characters long"))
        }

        // Validate description
        if (description == null) {
            errors.add(ValidationError("description", "Description cannot be blank"))
        } else if (description.isBlank()) {
            errors.add(ValidationError("description", "Description cannot be blank"))
        } else if (description.length > 1000) {
            errors.add(ValidationError("description", "Description must be at most 1000 characters long"))
        }

        // Validate due date
        if (dueDate == null) {
            errors.add(ValidationError("dueDate", "Due date is required"))
        } else if (dueDate.isBefore(Instant.now())) {
            errors.add(ValidationError("dueDate", "Due date cannot be in the past"))
        }

        // Validate status
        if (status == null) {
            errors.add(ValidationError("status", "Status is required"))
        } else {
            try {
                TaskStatus.valueOf(status)
            } catch (e: IllegalArgumentException) {
                errors.add(ValidationError("status", "Status must be one of: PENDING, IN_PROGRESS, COMPLETED"))
            }
        }

        if (errors.isNotEmpty()) {
            throw ValidationException("Validation failed", errors)
        }

        return TaskCreationHandler.Request(
            id = UUID.randomUUID().toString(),
            title = title!!,
            description = description!!,
            dueDate = dueDate!!,
            status = TaskStatus.valueOf(status!!),
        )
    }
}
