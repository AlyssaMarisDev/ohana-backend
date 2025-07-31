package com.ohana.domain.household

import com.ohana.data.household.*
import com.ohana.data.unitOfWork.*
import com.ohana.domain.permissions.TagPermissionManager
import com.ohana.domain.tags.DefaultTagService
import com.ohana.shared.enums.HouseholdMemberRole
import com.ohana.shared.enums.HouseholdMemberStatus
import java.time.Instant
import java.util.UUID

class HouseholdCreationHandler(
    private val unitOfWork: UnitOfWork,
    private val tagPermissionManager: TagPermissionManager,
    private val defaultTagService: DefaultTagService,
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
                        status = HouseholdMemberStatus.ACTIVE,
                        isDefault = true,
                        invitedBy = userId,
                        joinedAt = Instant.now(),
                    ),
                )

            // Create default tags for the household
            val defaultTags = defaultTagService.createDefaultTags(context, request.id)

            // Create permissions for the household member and give them access to all default tags
            val tagIds = defaultTags.map { it.id }
            tagPermissionManager.createPermissionsWithTags(context, householdMember.id, tagIds)

            // Return response
            Response(
                id = household.id,
                name = household.name,
                description = household.description,
            )
        }
}
