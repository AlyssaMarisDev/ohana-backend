package com.ohana.api.household.models

import com.ohana.domain.household.HouseholdInviteMemberHandler
import com.ohana.shared.Guid
import com.ohana.shared.enums.HouseholdMemberRole
import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException

/**
 * Request model for inviting a member to a household
 *
 * @param memberId The ID of the member to invite
 * @param role The role the member will have in the household (ADMIN or MEMBER)
 * @param tagIds Optional list of tag IDs that the invited member will have permission to view.
 *              If provided, the member will be able to see tasks that have any of these tags.
 *              If empty or not provided, the member will not have any tag-specific permissions.
 */
data class HouseholdInviteMemberRequest(
    val memberId: String?,
    val role: String?,
    val tagIds: List<String>? = emptyList(),
) {
    fun toDomain(): HouseholdInviteMemberHandler.Request {
        val errors = mutableListOf<ValidationError>()

        if (memberId == null) {
            errors.add(ValidationError("memberId", "Member ID is required"))
        } else if (memberId.isBlank()) {
            errors.add(ValidationError("memberId", "Member ID cannot be blank"))
        } else if (!Guid.isValid(memberId)) {
            errors.add(ValidationError("memberId", "Member ID must be a valid GUID"))
        }

        if (role == null) {
            errors.add(ValidationError("role", "Role is required"))
        } else {
            try {
                HouseholdMemberRole.valueOf(role)
            } catch (e: IllegalArgumentException) {
                errors.add(ValidationError("role", "Role must be one of: ADMIN, MEMBER"))
            }
        }

        // Validate tag IDs if provided
        val validatedTagIds = mutableListOf<String>()
        tagIds?.forEach { tagId ->
            if (tagId.isBlank()) {
                errors.add(ValidationError("tagIds", "Tag ID cannot be blank"))
            } else if (!Guid.isValid(tagId)) {
                errors.add(ValidationError("tagIds", "Tag ID must be a valid GUID"))
            } else {
                validatedTagIds.add(tagId)
            }
        }

        if (errors.isNotEmpty()) {
            throw ValidationException("Validation failed", errors)
        }

        return HouseholdInviteMemberHandler.Request(
            memberId = memberId!!,
            role = HouseholdMemberRole.valueOf(role!!),
            tagIds = validatedTagIds,
        )
    }
}
