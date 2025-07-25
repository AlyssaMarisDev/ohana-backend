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
        householdId: String? = null,
    ): Response =
        unitOfWork.execute { context ->
            val tags =
                if (householdId != null) {
                    // Validate that user is a member of the household
                    validator.validate(context, householdId, userId)

                    // Get tags for the household (including defaults)
                    context.tags.findByHouseholdIdWithDefaults(householdId)
                } else {
                    // Return only default tags when no household ID is provided
                    context.tags.findDefaultTags()
                }

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
