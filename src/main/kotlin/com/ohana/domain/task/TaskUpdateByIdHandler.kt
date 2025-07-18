package com.ohana.domain.task

import com.ohana.data.unitOfWork.*
import com.ohana.domain.validators.*
import com.ohana.shared.enums.TaskStatus
import com.ohana.shared.exceptions.NotFoundException
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

class TaskUpdateByIdHandler(
    private val unitOfWork: UnitOfWork,
    private val householdMemberValidator: HouseholdMemberValidator,
) {
    data class Request(
        @field:NotBlank(message = "Title is required")
        @field:Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters long")
        val title: String,
        @field:Size(max = 1000, message = "Description must be at most 1000 characters long")
        val description: String,
        @field:FutureDate(message = "Due date cannot be in the past")
        val dueDate: Instant,
        val status: TaskStatus,
    )

    data class Response(
        val id: String,
        val title: String,
        val description: String?,
        val dueDate: Instant,
        val status: TaskStatus?,
        val createdBy: String,
        val householdId: String,
    )

    suspend fun handle(
        userId: String,
        id: String,
        householdId: String,
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
