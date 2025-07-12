package com.ohana.household.handlers

import com.ohana.exceptions.NotFoundException
import com.ohana.shared.UnitOfWork

class HouseholdGetByIdHandler(
    private val unitOfWork: UnitOfWork,
) {
    data class Response(
        val id: String,
        val name: String,
        val description: String,
        val createdBy: String,
    )

    suspend fun handle(id: String): Response =
        unitOfWork.execute { context ->
            val household = context.households.findById(id) ?: throw NotFoundException("Household not found")

            Response(
                id = household.id,
                name = household.name,
                description = household.description,
                createdBy = household.createdBy,
            )
        }
}
