package com.ohana.task.handlers

import com.ohana.shared.HouseholdMemberValidator
import com.ohana.shared.UnitOfWork
import com.ohana.shared.UnitOfWorkContext

class TaskGetAllHandler(
    private val unitOfWork: UnitOfWork,
    private val householdMemberValidator: HouseholdMemberValidator,
) {
    data class Response(
        val id: String,
        val title: String,
        val description: String,
        val dueDate: java.time.Instant,
        val status: com.ohana.shared.TaskStatus,
        val createdBy: String,
        val householdId: String,
    )

    suspend fun handle(
        householdIds: List<String>,
        userId: String,
    ): List<Response> =
        unitOfWork.execute { context ->
            val effectiveHouseholdIds = getEffectiveHouseholdIds(context, householdIds, userId)

            // Fetch tasks for all households in a single database query
            context.tasks.findByHouseholdIds(effectiveHouseholdIds).map { task ->
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

    private fun getEffectiveHouseholdIds(
        context: UnitOfWorkContext,
        householdIds: List<String>,
        userId: String,
    ): List<String> {
        if (householdIds.isEmpty()) {
            // If no household IDs provided, get all households the user has access to
            return context.households.findByMemberId(userId).map { it.id }
        } else {
            // Validate that the user has access to all specified households
            householdIds.forEach { householdId ->
                householdMemberValidator.validate(context, householdId, userId)
            }
            return householdIds
        }
    }
}
