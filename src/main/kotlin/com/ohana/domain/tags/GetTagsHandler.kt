package com.ohana.domain.tags

import com.ohana.data.unitOfWork.*
import com.ohana.domain.validators.HouseholdMemberValidator

class GetTagsHandler(
    private val unitOfWork: UnitOfWork,
    private val validator: HouseholdMemberValidator,
    private val tagPermissionManager: TagPermissionManager,
) {
    data class Request(
        val householdId: String? = null,
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
            val tags =
                if (request.householdId != null) {
                    // Validate user is member of household
                    val householdMemberId = validator.validate(context, request.householdId, userId)

                    tagPermissionManager.getUserViewableTags(context, householdMemberId)
                } else {
                    // Get default tags only
                    context.tags.findDefaultTags().toList()
                }

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
