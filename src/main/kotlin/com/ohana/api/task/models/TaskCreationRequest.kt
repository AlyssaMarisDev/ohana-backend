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
    val tagIds: List<String>? = emptyList(),
) {
    fun toDomain(): TaskCreationHandler.Request {
        validate(this)

        return TaskCreationHandler.Request(
            id = id ?: Guid.generate(),
            title = title!!,
            description = description!!,
            dueDate = dueDate,
            status = TaskStatus.valueOf(status!!),
            householdId = householdId!!,
            tagIds = tagIds ?: emptyList(),
        )
    }

    companion object {
        private fun validate(request: TaskCreationRequest) {
          val errors = mutableListOf<ValidationError>()

          if (request.id == null || !Guid.isValid(request.id)) {
              errors.add(ValidationError(TaskCreationHandler.Request::id.name, "Task ID is required and must be a valid GUID"))
          }

          if (request.title == null || request.title.isBlank() || request.title.length > 255) {
              errors.add(ValidationError(TaskCreationHandler.Request::title.name, "Title is required and must be between 1 and 255 characters"))
          }

          if (request.description == null || request.description.length > 1000) {
              errors.add(ValidationError(TaskCreationHandler.Request::description.name, "Description is required and must be at most 1000 characters long"))
          }

          if (request.status == null) {
              errors.add(ValidationError(TaskCreationHandler.Request::status.name, "Status is required and must be one of: ${TaskStatus.allTaskStatusesString()}"))
          } else {
              try {
                  TaskStatus.valueOf(request.status)
              } catch (e: IllegalArgumentException) {
                  errors.add(ValidationError(TaskCreationHandler.Request::status.name, "Status must be one of: ${TaskStatus.allTaskStatusesString()}"))
              }
          }

          if (request.householdId == null || !Guid.isValid(request.householdId)) {
              errors.add(ValidationError(TaskCreationHandler.Request::householdId.name, "Household ID is required and must be a valid GUID"))
          }

          if (request.tagIds?.any { tagId -> !Guid.isValid(tagId) } == true) {
              errors.add(ValidationError(TaskCreationHandler.Request::tagIds.name, "All tag IDs must be valid GUIDs"))
          }

          if (errors.isNotEmpty()) {
              throw ValidationException("Validation failed", errors)
          }
        }
    }
}
