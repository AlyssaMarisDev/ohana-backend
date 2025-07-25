package com.ohana.domain.household

import com.ohana.data.household.*
import com.ohana.data.unitOfWork.*
import com.ohana.shared.enums.HouseholdMemberRole
import java.time.Instant
import java.util.UUID

class HouseholdCreationHandler(
    private val unitOfWork: UnitOfWork,
) {
    data class Request(
        val id: String,
        val name: String,
        val description: String,
    )

    data class Response(
        val id: String,
        val name: String,
        val description: String,
    )

    suspend fun handle(
        userId: String,
        request: Request,
    ): Response =
        unitOfWork.execute { context ->
            // Create household
            val household =
                context.households.create(
                    Household(
                        id = request.id,
                        name = request.name,
                        description = request.description,
                        createdBy = userId,
                    ),
                )

            // Create household member (admin)
            context.households.createMember(
                HouseholdMember(
                    id = UUID.randomUUID().toString(),
                    householdId = request.id,
                    memberId = userId,
                    role = HouseholdMemberRole.ADMIN,
                    isActive = true,
                    invitedBy = userId,
                    joinedAt = Instant.now(),
                ),
            )

            // Create default tags for the household
            createDefaultTags(context, request.id)

            // Return response
            Response(
                id = household.id,
                name = household.name,
                description = household.description,
            )
        }

    private fun createDefaultTags(
        context: UnitOfWorkContext,
        householdId: String,
    ) {
        val defaultTags =
            listOf(
                Tag(
                    id = UUID.randomUUID().toString(),
                    name = "metas",
                    color = "#3B82F6", // Blue
                    householdId = householdId,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                ),
                Tag(
                    id = UUID.randomUUID().toString(),
                    name = "adult",
                    color = "#EF4444", // Red
                    householdId = householdId,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                ),
                Tag(
                    id = UUID.randomUUID().toString(),
                    name = "work",
                    color = "#10B981", // Green
                    householdId = householdId,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                ),
                Tag(
                    id = UUID.randomUUID().toString(),
                    name = "kids",
                    color = "#F59E0B", // Amber
                    householdId = householdId,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                ),
                Tag(
                    id = UUID.randomUUID().toString(),
                    name = "chores",
                    color = "#8B5CF6", // Purple
                    householdId = householdId,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                ),
            )

        defaultTags.forEach { tag ->
            context.tags.create(tag)
        }
    }
}
