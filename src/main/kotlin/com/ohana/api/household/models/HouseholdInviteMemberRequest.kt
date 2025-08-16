package com.ohana.api.household.models

import com.ohana.domain.household.HouseholdInviteMemberHandler
import com.ohana.shared.Guid
import com.ohana.shared.enums.HouseholdMemberRole
import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException

data class HouseholdInviteMemberRequest(
    val memberId: String?,
    val role: String?,
    val tagIds: List<String>? = emptyList(),
) {
    fun toDomain(): HouseholdInviteMemberHandler.Request {
        validate(this)

        return HouseholdInviteMemberHandler.Request(
            memberId = memberId!!,
            role = HouseholdMemberRole.valueOf(role!!),
            tagIds = tagIds ?: emptyList(),
        )
    }

    companion object {
        private fun validate(request: HouseholdInviteMemberRequest) {
            val errors = mutableListOf<ValidationError>()

            if (request.memberId == null || !Guid.isValid(request.memberId)) {
                errors.add(
                    ValidationError(HouseholdInviteMemberHandler.Request::memberId.name, "Member ID is required and must be a valid GUID"),
                )
            }

            if (request.role == null) {
                errors.add(
                    ValidationError(
                        HouseholdInviteMemberHandler.Request::role.name,
                        "Role is required and must be one of: ${HouseholdMemberRole.values().joinToString(", ")}",
                    ),
                )
            } else {
                try {
                    HouseholdMemberRole.valueOf(request.role)
                } catch (e: IllegalArgumentException) {
                    errors.add(
                        ValidationError(
                            HouseholdInviteMemberHandler.Request::role.name,
                            "Role must be one of: ${HouseholdMemberRole.values().joinToString(", ")}",
                        ),
                    )
                }
            }

            if (request.tagIds?.any { tagId -> !Guid.isValid(tagId) } == true) {
                errors.add(ValidationError(HouseholdInviteMemberHandler.Request::tagIds.name, "All tag IDs must be valid GUIDs"))
            }

            if (errors.isNotEmpty()) {
                throw ValidationException("Validation failed", errors)
            }
        }
    }
}
