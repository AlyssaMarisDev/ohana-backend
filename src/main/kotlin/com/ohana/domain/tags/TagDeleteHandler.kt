package com.ohana.domain.tags

import com.ohana.data.unitOfWork.UnitOfWork
import com.ohana.domain.validators.HouseholdMemberValidator
import com.ohana.shared.exceptions.NotFoundException

class TagDeleteHandler(
    private val unitOfWork: UnitOfWork,
    private val validator: HouseholdMemberValidator,
) {
    data class Response(
        val success: Boolean,
        val message: String,
    )

    suspend fun handle(
        userId: String,
        householdId: String,
        tagId: String,
    ): Response =
        unitOfWork.execute { context ->
            // Validate user is member of household
            validator.validate(context, householdId, userId)

            // Find the tag to delete
            val existingTag =
                context.tags.findById(tagId)
                    ?: throw NotFoundException("Tag not found in this household")

            // Delete associated task tags first (cascading delete)
            context.taskTags.deleteByTagId(tagId)

            // Delete the tag
            val deleted = context.tags.deleteById(tagId)

            if (!deleted) {
                throw NotFoundException("Failed to delete tag")
            }

            Response(
                success = true,
                message = "Tag '${existingTag.name}' has been deleted successfully",
            )
        }
}
