package com.ohana.task.handlers

import com.ohana.exceptions.ValidationError
import com.ohana.shared.Guid
import com.ohana.shared.HouseholdMemberValidator
import com.ohana.shared.TaskStatus
import com.ohana.shared.UnitOfWork
import com.ohana.shared.Validatable
import com.ohana.task.entities.Task
import java.time.Instant

class TaskCreationHandler(
    private val unitOfWork: UnitOfWork,
    private val householdMemberValidator: HouseholdMemberValidator,
) {
    data class Request(
        val id: String,
        val title: String,
        val description: String,
        val dueDate: Instant,
        val status: TaskStatus,
    ) : Validatable {
        override fun validate(): List<ValidationError> {
            val errors = mutableListOf<ValidationError>()

            if (id.isEmpty()) errors.add(ValidationError("id", "Task ID is required"))
            if (!Guid.isValid(id)) errors.add(ValidationError("id", "Task ID must be a valid GUID"))
            if (title.isEmpty()) errors.add(ValidationError("title", "Title is required"))
            if (title.length < 1) errors.add(ValidationError("title", "Title must be at least 1 character long"))
            if (title.length > 255) errors.add(ValidationError("title", "Title must be at most 255 characters long"))
            if (description.isEmpty()) errors.add(ValidationError("description", "Description is required"))
            if (description.length < 1) errors.add(ValidationError("description", "Description must be at least 1 character long"))
            if (description.length > 1000) errors.add(ValidationError("description", "Description must be at most 1000 characters long"))
            if (dueDate.isBefore(Instant.now())) errors.add(ValidationError("dueDate", "Due date cannot be in the past"))

            return errors
        }
    }

    data class Response(
        val id: String,
        val title: String,
        val description: String,
        val dueDate: Instant,
        val status: TaskStatus,
        val createdBy: String,
        val householdId: String,
    )

    suspend fun handle(
        userId: String,
        householdId: String,
        request: Request,
    ): Response =
        unitOfWork.execute { context ->
            householdMemberValidator.validate(context, householdId, userId)

            val task =
                context.tasks.create(
                    Task(
                        id = request.id,
                        title = request.title,
                        description = request.description,
                        dueDate = request.dueDate,
                        status = request.status,
                        createdBy = userId,
                        householdId = householdId,
                    ),
                )

            Response(
                id = task.id,
                title = task.title,
                description = task.description,
                dueDate = task.dueDate,
                status = task.status,
                createdBy = task.createdBy,
                householdId = task.householdId,
            )
        }
}
