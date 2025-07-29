package com.ohana.domain.tags

import com.ohana.data.tags.Tag
import com.ohana.data.unitOfWork.UnitOfWork
import com.ohana.domain.validators.HouseholdMemberValidator
import com.ohana.shared.exceptions.ConflictException
import com.ohana.shared.exceptions.NotFoundException
import java.time.Instant

class TagUpdateHandler(
    private val unitOfWork: UnitOfWork,
    private val validator: HouseholdMemberValidator,
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
        tagId: String,
        request: Request,
    ): Response =
        unitOfWork.execute { context ->
            // Validate user is member of household
            validator.validate(context, householdId, userId)

            // Fetch all tags for the household first
            val householdTags = context.tags.findByHouseholdId(householdId)

            // Find the tag to update
            val existingTag =
                householdTags.find { it.id == tagId }
                    ?: throw NotFoundException("Tag not found in this household")

            // Check if another tag with the same name already exists in this household
            val tagWithSameName =
                householdTags.find { tag ->
                    tag.id != tagId && tag.name.equals(request.name, ignoreCase = true)
                }

            if (tagWithSameName != null) {
                throw ConflictException("A tag with the name '${request.name}' already exists in this household")
            }

            // Update the tag
            val now = Instant.now()
            val updatedTag =
                Tag(
                    id = existingTag.id,
                    name = request.name,
                    color = request.color,
                    householdId = existingTag.householdId,
                    createdAt = existingTag.createdAt,
                    updatedAt = now,
                )

            val savedTag = context.tags.update(updatedTag)

            Response(
                id = savedTag.id,
                name = savedTag.name,
                color = savedTag.color,
                householdId = savedTag.householdId,
                createdAt = savedTag.createdAt.toString(),
                updatedAt = savedTag.updatedAt.toString(),
            )
        }
}
