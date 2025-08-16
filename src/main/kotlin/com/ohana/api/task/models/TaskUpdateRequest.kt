package com.ohana.api.task.models

import com.ohana.domain.task.TaskUpdateByIdHandler
import com.ohana.shared.Guid
import com.ohana.shared.enums.TaskStatus
import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException
import java.time.Instant

data class TaskUpdateRequest(
    val title: String?,
    val description: String?,
    val dueDate: Instant?,
    val status: String?,
    val tagIds: List<String>? = emptyList(),
) {
    fun toDomain(): TaskUpdateByIdHandler.Request {
        validate(this)

        return TaskUpdateByIdHandler.Request(
            title = title!!,
            description = description!!,
            dueDate = dueDate,
            status = TaskStatus.valueOf(status!!),
            tagIds = tagIds ?: emptyList(),
        )
    }

    companion object {
        private fun validate(request: TaskUpdateRequest) {
            val errors = mutableListOf<ValidationError>()

            if (request.title == null || request.title.isBlank() || request.title.length > 255) {
                errors.add(ValidationError(TaskUpdateByIdHandler.Request::title.name, "Title is required and must be between 1 and 255 characters"))
            }

            if (request.description == null || request.description.length > 1000) {
                errors.add(ValidationError(TaskUpdateByIdHandler.Request::description.name, "Description is required and must be at most 1000 characters long"))
            }

            if (request.status == null) {
                errors.add(ValidationError(TaskUpdateByIdHandler.Request::status.name, "Status is required and must be one of: ${TaskStatus.allTaskStatusesString()}"))
            } else {
                try {
                    TaskStatus.valueOf(request.status)
                } catch (e: IllegalArgumentException) {
                    errors.add(ValidationError(TaskUpdateByIdHandler.Request::status.name, "Status must be one of: ${TaskStatus.allTaskStatusesString()}"))
                }
            }

            if (request.tagIds?.any { tagId -> !Guid.isValid(tagId) } == true) {
                errors.add(ValidationError(TaskUpdateByIdHandler.Request::tagIds.name, "All tag IDs must be valid GUIDs"))
            }

            if (errors.isNotEmpty()) {
                throw ValidationException("Validation failed", errors)
            }
        }
    }
}
