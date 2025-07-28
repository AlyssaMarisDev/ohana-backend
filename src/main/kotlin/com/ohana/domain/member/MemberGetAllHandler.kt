package com.ohana.domain.member

import com.ohana.data.unitOfWork.*
import com.ohana.domain.validators.HouseholdMemberValidator

class MemberGetAllHandler(
    private val unitOfWork: UnitOfWork,
    private val householdMemberValidator: HouseholdMemberValidator,
) {
    data class Response(
        val id: String,
        val name: String,
        val age: Int?,
        val gender: String?,
        val email: String,
    )

    suspend fun handle(
        userId: String,
        householdId: String,
    ): List<Response> =
        unitOfWork.execute { context ->
            // Validate that the user is a member of the household
            householdMemberValidator.validate(context, householdId, userId)

            context.members.findByHouseholdId(householdId).map { member ->
                Response(
                    id = member.id,
                    name = member.name,
                    age = member.age,
                    gender = member.gender,
                    email = member.email,
                )
            }
        }
}
