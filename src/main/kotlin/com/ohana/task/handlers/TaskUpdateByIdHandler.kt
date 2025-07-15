package com.ohana.task.handlers

import com.ohana.exceptions.NotFoundException
import com.ohana.exceptions.ValidationError
import com.ohana.shared.HouseholdMemberValidator
import com.ohana.shared.UnitOfWork
import com.ohana.shared.Validatable
import java.time.Instant

class TaskUpdateByIdHandler(
    private val unitOfWork: UnitOfWork,
    private val householdMemberValidator: HouseholdMemberValidator,
) {
    data class Request(
        val title: String,
        val description: String,
        val dueDate: Instant,
        val status: com.ohana.shared.TaskStatus,
    ) : Validatable {
        override fun validate(): List<ValidationError> {
            val errors = mutableListOf<ValidationError>()

            if (title.isEmpty()) errors.add(ValidationError("title", "Title is required"))
            if (title.length < 1) errors.add(ValidationError("title", "Title must be at least 1 character long"))
            if (title.length > 255) errors.add(ValidationError("title", "Title must be at most 255 characters long"))
            if (description.length >
                1000
            ) {
                errors.add(ValidationError("description", "Description must be at most 1000 characters long"))
            }
            if (dueDate.isBefore(Instant.now())) errors.add(ValidationError("dueDate", "Due date cannot be in the past"))

            return errors
        }
    }

    data class Response(
        val id: String,
        val title: String,
        val description: String?,
        val dueDate: Instant,
        val status: com.ohana.shared.TaskStatus?,
        val createdBy: String,
        val householdId: String,
    )

    suspend fun handle(
        id: String,
        householdId: String,
        userId: String,
        request: Request,
    ): Response =
        unitOfWork.execute { context ->
            householdMemberValidator.validate(context, householdId, userId)

            val existingTask = context.tasks.findById(id) ?: throw NotFoundException("Task not found")

            if (existingTask.householdId != householdId) {
                throw NotFoundException("Task not found in this household")
            }

            val updatedTask =
                context.tasks.update(
                    existingTask.copy(
                        title = request.title,
                        description = request.description,
                        dueDate = request.dueDate,
                        status = request.status,
                    ),
                )

            Response(
                id = updatedTask.id,
                title = updatedTask.title,
                description = updatedTask.description,
                dueDate = updatedTask.dueDate,
                status = updatedTask.status,
                createdBy = updatedTask.createdBy,
                householdId = updatedTask.householdId,
            )
        }
}
