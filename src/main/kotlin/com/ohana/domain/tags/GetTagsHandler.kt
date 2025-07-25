package com.ohana.domain.tags

import com.ohana.data.unitOfWork.*
import com.ohana.domain.validators.HouseholdMemberValidator

class GetTagsHandler(
    private val unitOfWork: UnitOfWork,
    private val validator: HouseholdMemberValidator,
) {
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
        householdId: String,
    ): Response =
        unitOfWork.execute { context ->
            // Validate that user is a member of the household
            validator.validate(context, householdId, userId)

            // Get tags for the household
            val tags = context.tags.findByHouseholdIdWithDefaults(householdId)

            // Convert to response format
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
