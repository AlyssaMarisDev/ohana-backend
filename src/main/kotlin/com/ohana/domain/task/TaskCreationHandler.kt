package com.ohana.domain.task

import com.ohana.data.task.*
import com.ohana.data.unitOfWork.*
import com.ohana.domain.validators.*
import com.ohana.shared.enums.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant

class TaskCreationHandler(
    private val unitOfWork: UnitOfWork,
    private val householdMemberValidator: HouseholdMemberValidator,
) {
    data class Request(
        @field:NotBlank(message = "Task ID is required")
        @field:Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "Task ID must be a valid GUID")
        val id: String,
        @field:NotBlank(message = "Title is required")
        @field:Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters long")
        val title: String,
        @field:NotBlank(message = "Description is required")
        @field:Size(min = 1, max = 1000, message = "Description must be between 1 and 1000 characters long")
        val description: String,
        @field:FutureDate(message = "Due date cannot be in the past")
        val dueDate: Instant,
        val status: TaskStatus,
    )

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
