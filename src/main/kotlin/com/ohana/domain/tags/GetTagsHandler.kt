package com.ohana.domain.tags

import com.ohana.data.unitOfWork.*
import com.ohana.domain.permissions.TagPermissionManager
import com.ohana.domain.validators.HouseholdMemberValidator

class GetTagsHandler(
    private val unitOfWork: UnitOfWork,
    private val validator: HouseholdMemberValidator,
    private val tagPermissionManager: TagPermissionManager,
) {
    data class Request(
        val householdId: String,
    )

    data class Response(
        val tags: List<TagResponse>,
    )

    data class TagResponse(
        val id: String,
        val name: String,
        val color: String,
    )

    suspend fun handle(
        userId: String,
        request: Request,
    ): Response =
        unitOfWork.execute { context ->
            // Validate user is member of household and get household member ID
            val householdMemberId = validator.validate(context, request.householdId, userId)

            // Get user viewable tags for the household
            val tags = tagPermissionManager.getUserViewableTags(context, householdMemberId)

            Response(
                tags =
                    tags.map { tag ->
                        TagResponse(
                            id = tag.id,
                            name = tag.name,
                            color = tag.color,
                        )
                    },
            )
        }
}
