package com.ohana.task.handlers

import com.ohana.shared.HouseholdMemberValidator
import com.ohana.shared.TaskStatus
import com.ohana.shared.UnitOfWork
import com.ohana.task.entities.Task
import org.slf4j.LoggerFactory
import java.time.Instant

class TaskCreationHandler(
    private val unitOfWork: UnitOfWork,
    private val householdMemberValidator: HouseholdMemberValidator,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    data class Request(
        val id: String,
        val title: String,
        val description: String,
        val dueDate: Instant,
        val status: TaskStatus,
        val householdId: String,
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
        request: Request,
    ): Response =
        unitOfWork.execute { context ->
            householdMemberValidator.validate(context, request.householdId, userId)

            val task =
                context.tasks.create(
                    Task(
                        id = request.id,
                        title = request.title,
                        description = request.description,
                        dueDate = request.dueDate,
                        status = request.status,
                        createdBy = userId,
                        householdId = request.householdId,
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
