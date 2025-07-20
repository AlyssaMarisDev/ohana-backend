package com.ohana.api.household.models

import com.ohana.domain.household.HouseholdInviteMemberHandler
import com.ohana.shared.Guid
import com.ohana.shared.enums.HouseholdMemberRole
import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException

data class HouseholdInviteMemberRequest(
    val memberId: String?,
    val role: String?,
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

        if (errors.isNotEmpty()) {
            throw ValidationException("Validation failed", errors)
        }

        return HouseholdInviteMemberHandler.Request(
            memberId = memberId!!,
            role = HouseholdMemberRole.valueOf(role!!),
        )
    }
}
