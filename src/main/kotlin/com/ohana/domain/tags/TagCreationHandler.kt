package com.ohana.domain.tags

import com.ohana.data.tags.Tag
import com.ohana.data.unitOfWork.UnitOfWork
import com.ohana.domain.permissions.TagPermissionManager
import com.ohana.domain.validators.HouseholdMemberValidator
import com.ohana.shared.Guid
import com.ohana.shared.exceptions.ConflictException
import java.time.Instant

class TagCreationHandler(
    private val unitOfWork: UnitOfWork,
    private val validator: HouseholdMemberValidator,
    private val tagPermissionManager: TagPermissionManager,
) {
    data class Request(
        val name: String,
        val color: String,
    )

    data class Response(
        val id: String,
        val name: String,
        val color: String,
        val householdId: String,
        val createdAt: String,
        val updatedAt: String,
    )

    suspend fun handle(
        userId: String,
        householdId: String,
        request: Request,
    ): Response =
        unitOfWork.execute { context ->
            // Validate user is member of household and get household member ID
            val householdMemberId = validator.validate(context, householdId, userId)

            // Check if tag with same name already exists in this household
            val existingTags =
                context.tags.findByHouseholdId(
                    householdId,
                )
            val tagWithSameName =
                existingTags.find {
                    it.name.equals(request.name, ignoreCase = true)
                }

            if (tagWithSameName != null) {
                throw ConflictException("A tag with the name '${request.name}' already exists in this household")
            }

            // Create new tag
            val now = Instant.now()
            val tag =
                Tag(
                    id = Guid.generate(),
                    name = request.name,
                    color = request.color,
                    householdId = householdId,
                    createdAt = now,
                    updatedAt = now,
                )

            val createdTag = context.tags.create(tag)

            // Give the creator permissions to view this tag
            tagPermissionManager.giveTagPermissionsToMember(context, householdMemberId, listOf(createdTag.id))

            Response(
                id = createdTag.id,
                name = createdTag.name,
                color = createdTag.color,
                householdId = createdTag.householdId,
                createdAt = createdTag.createdAt.toString(),
                updatedAt = createdTag.updatedAt.toString(),
            )
        }
}
