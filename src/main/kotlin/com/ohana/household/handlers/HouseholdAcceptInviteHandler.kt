package com.ohana.household.handlers

import com.ohana.exceptions.AuthorizationException
import com.ohana.exceptions.DbException
import com.ohana.utils.DatabaseUtils.Companion.get
import com.ohana.utils.DatabaseUtils.Companion.transaction
import com.ohana.utils.DatabaseUtils.Companion.update
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi

class HouseholdAcceptInviteHandler(
    private val jdbi: Jdbi,
) {
    suspend fun handle(
        userId: String,
        householdId: String,
    ) = transaction(jdbi) { handle ->
        validateHouseholdMember(handle, userId, householdId)
        updateHouseholdMember(handle, userId, householdId)
    }

    private fun validateHouseholdMember(
        handle: Handle,
        userId: String,
        householdId: String,
    ) {
        val householdMember = getHouseholdMember(handle, userId, householdId)

        if (householdMember == null) {
            throw AuthorizationException("User has not been invited to the household")
        }
    }

    private fun getHouseholdMember(
        handle: Handle,
        userId: String,
        householdId: String,
    ): HouseholdMember? {
        val selectQuery = """
            SELECT * FROM household_members
            WHERE householdId = :householdId
            AND memberId = :userId
        """

        return get(
            handle,
            selectQuery,
            mapOf(
                "householdId" to householdId,
                "userId" to userId,
            ),
            HouseholdMember::class,
        ).firstOrNull()
    }

    private fun updateHouseholdMember(
        handle: Handle,
        userId: String,
        householdId: String,
    ) {
        val updateQuery = """
            UPDATE household_members
            SET isActive = true,
                joinedAt = NOW()
            WHERE householdId = :householdId
              AND memberId = :userId
        """

        val updatedRows =
            update(
                handle,
                updateQuery,
                mapOf(
                    "householdId" to householdId,
                    "userId" to userId,
                ),
            )

        if (updatedRows == 0) throw DbException("Failed to update household member")
    }

    data class HouseholdMember(
        val id: String,
    )
}
