package com.ohana.domain.household

import com.ohana.data.household.*
import com.ohana.data.unitOfWork.*
import com.ohana.shared.enums.HouseholdMemberRole
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

class HouseholdCreationHandler(
    private val unitOfWork: UnitOfWork,
) {
    data class Request(
        @field:NotBlank(message = "Household ID is required")
        @field:Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "Household ID must be a valid GUID")
        val id: String,
        @field:NotBlank(message = "Household name is required")
        @field:Size(min = 1, max = 255, message = "Household name must be between 1 and 255 characters long")
        val name: String,
        @field:NotBlank(message = "Household description is required")
        @field:Size(max = 1000, message = "Household description must be at most 1000 characters long")
        val description: String,
    )

    data class Response(
        val id: String,
        val name: String,
        val description: String,
    )

    suspend fun handle(
        userId: String,
        request: Request,
    ): Response =
        unitOfWork.execute { context ->
            // Create household
            val household =
                context.households.create(
                    Household(
                        id = request.id,
                        name = request.name,
                        description = request.description,
                        createdBy = userId,
                    ),
                )

            // Create household member (admin)
            context.households.createMember(
                HouseholdMember(
                    id = UUID.randomUUID().toString(),
                    householdId = request.id,
                    memberId = userId,
                    role = HouseholdMemberRole.admin,
                    isActive = true,
                    invitedBy = userId,
                    joinedAt = Instant.now(),
                ),
            )

            // Return response
            Response(
                id = household.id,
                name = household.name,
                description = household.description,
            )
        }
}
