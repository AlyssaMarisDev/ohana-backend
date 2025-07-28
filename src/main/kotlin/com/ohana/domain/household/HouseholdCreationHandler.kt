package com.ohana.domain.household

import com.ohana.data.household.*
import com.ohana.data.unitOfWork.*
import com.ohana.domain.permissions.TagPermissionManager
import com.ohana.shared.enums.HouseholdMemberRole
import java.time.Instant
import java.util.UUID

class HouseholdCreationHandler(
    private val unitOfWork: UnitOfWork,
    private val tagPermissionManager: TagPermissionManager,
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
            val householdMember =
                context.households.createMember(
                    HouseholdMember(
                        id = UUID.randomUUID().toString(),
                        householdId = request.id,
                        memberId = userId,
                        role = HouseholdMemberRole.ADMIN,
                        isActive = true,
                        isDefault = true,
                        invitedBy = userId,
                        joinedAt = Instant.now(),
                    ),
                )

            // Create default permissions for the household member and give them access to all default tags
            tagPermissionManager.createDefaultPermissions(context, householdMember.id)

            // Return response
            Response(
                id = household.id,
                name = household.name,
                description = household.description,
            )
        }
}
