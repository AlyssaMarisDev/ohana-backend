package com.ohana.api.task.models

import com.ohana.domain.task.TaskUpdateByIdHandler
import com.ohana.shared.enums.TaskStatus
import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException
import java.time.Instant

data class TaskUpdateRequest(
    val title: String?,
    val description: String?,
    val dueDate: Instant?,
    val status: String?,
) {
    fun toDomain(): TaskUpdateByIdHandler.Request {
        val errors = mutableListOf<ValidationError>()

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

        if (errors.isNotEmpty()) {
            throw ValidationException("Validation failed", errors)
        }

        return TaskUpdateByIdHandler.Request(
            title = title!!,
            description = description!!,
            dueDate = dueDate!!,
            status = TaskStatus.valueOf(status!!),
        )
    }
}
