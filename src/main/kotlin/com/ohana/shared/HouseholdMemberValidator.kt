package com.ohana.shared

import com.ohana.exceptions.AuthorizationException
import com.ohana.exceptions.NotFoundException

class HouseholdMemberValidator {
    fun validate(
        context: UnitOfWorkContext,
        householdId: String,
        userId: String,
    ) {
        context.households.findById(householdId)
            ?: throw NotFoundException("Household not found")

        context.households.findMemberById(householdId, userId)
            ?: throw AuthorizationException("User is not a member of the household")
    }
}
