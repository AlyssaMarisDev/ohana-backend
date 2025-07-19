package com.ohana.domain.household

import com.ohana.data.unitOfWork.*
import com.ohana.domain.validators.HouseholdMemberValidator
import com.ohana.shared.exceptions.NotFoundException

class HouseholdGetByIdHandler(
    private val unitOfWork: UnitOfWork,
    private val householdMemberValidator: HouseholdMemberValidator,
) {
    data class Response(
        val id: String,
        val name: String,
        val description: String,
        val createdBy: String,
    )

    suspend fun handle(
        userId: String,
        id: String,
    ): Response =
        unitOfWork.execute { context ->
            // Validate that the user is a member of the household
            householdMemberValidator.validate(context, id, userId)

            val household = context.households.findById(id) ?: throw NotFoundException("Household not found")

            Response(
                id = household.id,
                name = household.name,
                description = household.description,
                createdBy = household.createdBy,
            )
        }
}
