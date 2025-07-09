package com.ohana.household.handlers

import com.ohana.exceptions.DbException
import com.ohana.utils.DatabaseUtils.Companion.get
import com.ohana.utils.DatabaseUtils.Companion.transaction
import com.ohana.utils.DatabaseUtils.Companion.update
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import java.util.UUID

class HouseholdAddMemberHandler(
    private val jdbi: Jdbi,
) {
    data class Request(
        val memberId: String,
        val role: String,
    )

    suspend fun handle(
        userId: String,
        householdId: String,
        request: Request,
    ) = transaction(jdbi) { handle ->
        val updatedRows = addHouseholdMember(handle, userId, householdId, request)

        if (updatedRows == 0) {
            throw DbException("Failed to add member to household")
        }
    }

    private fun addHouseholdMember(
        handle: Handle,
        userId: String,
        householdId: String,
        request: Request,
    ): Int {
        val updateQuery = """
            INSERT INTO household_members (id, householdId, memberId, role, invitedBy)
            VALUES (:id, :householdId, :memberId, :role, :invitedBy)
        """

        return update(
            handle,
            updateQuery,
            mapOf(
                "id" to UUID.randomUUID().toString(),
                "householdId" to householdId,
                "memberId" to request.memberId,
                "role" to request.role,
                "invitedBy" to userId,
            ),
        )
    }
}
