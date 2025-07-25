package com.ohana.domain.task

import com.ohana.data.unitOfWork.*
import com.ohana.domain.validators.HouseholdMemberValidator
import com.ohana.shared.enums.TaskStatus

class TaskGetAllHandler(
    private val unitOfWork: UnitOfWork,
    private val householdMemberValidator: HouseholdMemberValidator,
    private val taskTagManager: TaskTagManager,
) {
    data class Request(
        val householdIds: List<String>,
    )

    data class Response(
        val id: String,
        val title: String,
        val description: String,
        val dueDate: java.time.Instant,
        val status: TaskStatus,
        val createdBy: String,
        val householdId: String,
        val tags: List<TaskTagResponse>,
    )

    data class TaskTagResponse(
        val id: String,
        val name: String,
        val color: String,
    )

    suspend fun handle(
        userId: String,
        request: Request,
    ): List<Response> =
        unitOfWork.execute { context ->
            val effectiveHouseholdIds = getEffectiveHouseholdIds(context, request.householdIds, userId)

            val tasks = context.tasks.findByHouseholdIds(effectiveHouseholdIds)
            val taskIds = tasks.map { it.id }

            val tags = taskTagManager.getTasksTags(context, taskIds)

            tasks.map { task ->
                Response(
                    id = task.id,
                    title = task.title,
                    description = task.description,
                    dueDate = task.dueDate,
                    status = task.status,
                    createdBy = task.createdBy,
                    householdId = task.householdId,
                    tags =
                        tags[task.id]?.map {
                            TaskTagResponse(
                                id = it.id,
                                name = it.name,
                                color = it.color,
                            )
                        } ?: emptyList(),
                )
            }
        }

    private fun getEffectiveHouseholdIds(
        context: UnitOfWorkContext,
        householdIds: List<String>,
        userId: String,
    ): List<String> {
        if (householdIds.isEmpty()) {
            return context.households.findByMemberId(userId).map { it.id }
        } else {
            householdIds.forEach { householdId ->
                householdMemberValidator.validate(context, householdId, userId)
            }
            return householdIds
        }
    }
}
