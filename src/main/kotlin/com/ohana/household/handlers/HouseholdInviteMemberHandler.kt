package com.ohana.household.handlers

import com.ohana.exceptions.AuthorizationException
import com.ohana.exceptions.ConflictException
import com.ohana.shared.HouseholdMember
import com.ohana.shared.HouseholdMemberRole
import com.ohana.shared.UnitOfWork
import java.util.UUID

class HouseholdInviteMemberHandler(
    private val unitOfWork: UnitOfWork,
) {
    data class Request(
        val memberId: String,
        val role: HouseholdMemberRole,
    )

    suspend fun handle(
        userId: String,
        householdId: String,
        request: Request,
    ) = unitOfWork.execute { context ->
        // Validate authorization
        val actorHouseholdMember = context.households.findMemberById(householdId, userId)
        if (actorHouseholdMember == null) {
            throw AuthorizationException("User is not a member of the household")
        }
        if (actorHouseholdMember.role != HouseholdMemberRole.admin) {
            throw AuthorizationException("User is not an admin of the household")
        }

        // Validate member doesn't already exist
        val memberHouseholdMember = context.households.findMemberById(householdId, request.memberId)
        if (memberHouseholdMember != null) {
            throw ConflictException("User is already a member of the household")
        }

        // Create household member
        context.households.createMember(
            HouseholdMember(
                id = UUID.randomUUID().toString(),
                householdId = householdId,
                memberId = request.memberId,
                role = request.role,
                isActive = false,
                invitedBy = userId,
            ),
        )
    }
}
