package com.ohana.household.handlers

import com.ohana.exceptions.ValidationError
import com.ohana.shared.Guid
import com.ohana.shared.Household
import com.ohana.shared.HouseholdMember
import com.ohana.shared.HouseholdMemberRole
import com.ohana.shared.UnitOfWork
import com.ohana.shared.Validatable
import java.time.Instant
import java.util.UUID

class HouseholdCreationHandler(
    private val unitOfWork: UnitOfWork,
) {
    data class Request(
        val id: String,
        val name: String,
        val description: String,
    ) : Validatable {
        override fun validate(): List<ValidationError> {
            val errors = mutableListOf<ValidationError>()

            if (id.isEmpty()) errors.add(ValidationError("id", "Household ID is required"))
            if (!Guid.isValid(id)) errors.add(ValidationError("id", "Household ID must be a valid GUID"))
            if (name.isEmpty()) errors.add(ValidationError("name", "Household name is required"))
            if (name.length < 1) errors.add(ValidationError("name", "Household name must be at least 1 character long"))
            if (name.length > 255) errors.add(ValidationError("name", "Household name must be at most 255 characters long"))
            if (description.isEmpty()) errors.add(ValidationError("description", "Household description is required"))
            if (description.length >
                1000
            ) {
                errors.add(ValidationError("description", "Household description must be at most 1000 characters long"))
            }

            return errors
        }
    }

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
