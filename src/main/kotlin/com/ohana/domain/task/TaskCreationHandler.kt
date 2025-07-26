package com.ohana.domain.task

import com.ohana.data.task.*
import com.ohana.data.unitOfWork.*
import com.ohana.domain.tags.TaskTagManager
import com.ohana.domain.validators.*
import com.ohana.shared.enums.*
import java.time.Instant

class TaskCreationHandler(
    private val unitOfWork: UnitOfWork,
    private val householdMemberValidator: HouseholdMemberValidator,
    private val taskTagManager: TaskTagManager,
) {
    data class Request(
        val id: String,
        val title: String,
        val description: String,
        val dueDate: Instant?,
        val status: TaskStatus,
        val householdId: String,
        val tagIds: List<String>,
    )

    data class Response(
        val id: String,
        val title: String,
        val description: String,
        val dueDate: Instant?,
        val status: TaskStatus,
        val completedAt: Instant?,
        val createdBy: String,
        val householdId: String,
        val tagIds: List<String>,
    )

    suspend fun handle(
        userId: String,
        request: Request,
    ): Response =
        unitOfWork.execute { context ->
            householdMemberValidator.validate(context, request.householdId, userId)

            // Set completed_at if the task is created with COMPLETED status
            val completedAt = if (request.status == TaskStatus.COMPLETED) Instant.now() else null

            val task =
                context.tasks.create(
                    Task(
                        id = request.id,
                        title = request.title,
                        description = request.description,
                        dueDate = request.dueDate,
                        status = request.status,
                        completedAt = completedAt,
                        createdBy = userId,
                        householdId = request.householdId,
                    ),
                )

            val tags = taskTagManager.assignTagsToTask(context, task.id, request.tagIds)

            Response(
                id = task.id,
                title = task.title,
                description = task.description,
                dueDate = task.dueDate,
                status = task.status,
                completedAt = task.completedAt,
                createdBy = task.createdBy,
                householdId = task.householdId,
                tagIds = tags.map { it.id },
            )
        }
}
