package com.ohana.domain.household

import com.ohana.exceptions.AuthorizationException
import com.ohana.shared.UnitOfWork
import java.time.Instant

class HouseholdAcceptInviteHandler(
    private val unitOfWork: UnitOfWork,
) {
    suspend fun handle(
        userId: String,
        householdId: String,
    ) = unitOfWork.execute { context ->
        // Validate household member exists
        val householdMember = context.households.findMemberById(householdId, userId)
        if (householdMember == null) {
            throw AuthorizationException("User has not been invited to the household")
        }

        // Update household member to active
        context.households.updateMember(
            householdMember.copy(
                isActive = true,
                joinedAt = Instant.now(),
            ),
        )
    }
}
