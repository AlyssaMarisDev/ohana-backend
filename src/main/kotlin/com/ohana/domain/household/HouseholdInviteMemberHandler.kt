package com.ohana.domain.household

import com.ohana.data.household.HouseholdMember
import com.ohana.data.unitOfWork.*
import com.ohana.shared.enums.HouseholdMemberRole
import com.ohana.shared.exceptions.AuthorizationException
import com.ohana.shared.exceptions.ConflictException
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import java.util.UUID

class HouseholdInviteMemberHandler(
    private val unitOfWork: UnitOfWork,
) {
    data class Request(
        @field:NotBlank(message = "Member ID is required")
        @field:Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "Member ID must be a valid GUID")
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
        if (actorHouseholdMember.role != HouseholdMemberRole.ADMIN) {
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
