package com.ohana.domain.validators

import com.ohana.data.unitOfWork.UnitOfWorkContext
import com.ohana.shared.Guid
import com.ohana.shared.enums.HouseholdMemberStatus
import com.ohana.shared.exceptions.AuthorizationException
import com.ohana.shared.exceptions.NotFoundException

class HouseholdMemberValidator {
    fun validate(
        context: UnitOfWorkContext,
        householdId: String,
        userId: String,
    ): String {
        if (!Guid.isValid(householdId) || !Guid.isValid(userId)) {
            throw IllegalArgumentException("Household ID and user ID must be valid GUIDs")
        }

        context.households.findById(householdId)
            ?: throw NotFoundException("Household not found")

        val member =
            context.households.findMemberById(householdId, userId)
                ?: throw AuthorizationException("User is not a member of the household")

        if (member.status != HouseholdMemberStatus.ACTIVE) {
            throw AuthorizationException("User is not an active member of the household")
        }

        return member.id
    }
}
