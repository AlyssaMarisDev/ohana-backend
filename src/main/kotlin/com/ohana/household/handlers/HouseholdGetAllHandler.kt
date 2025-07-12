package com.ohana.household.handlers

import com.ohana.shared.UnitOfWork

class HouseholdGetAllHandler(
    private val unitOfWork: UnitOfWork,
) {
    data class Response(
        val id: String,
        val name: String,
        val description: String,
        val createdBy: String,
    )

    suspend fun handle(): List<Response> =
        unitOfWork.execute { context ->
            context.households.findAll().map { household ->
                Response(
                    id = household.id,
                    name = household.name,
                    description = household.description,
                    createdBy = household.createdBy,
                )
            }
        }
}
