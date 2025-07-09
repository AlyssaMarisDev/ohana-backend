package com.ohana.household.handlers

import com.ohana.exceptions.DbException
import com.ohana.utils.DatabaseUtils.Companion.get
import com.ohana.utils.DatabaseUtils.Companion.insert
import com.ohana.utils.DatabaseUtils.Companion.transaction
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import java.util.UUID

class HouseholdInviteMemberHandler(
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
        val insertedRows = addHouseholdMember(handle, userId, householdId, request)

        if (insertedRows == 0) {
            throw DbException("Failed to add member to household")
        }
    }

    private fun addHouseholdMember(
        handle: Handle,
        userId: String,
        householdId: String,
        request: Request,
    ): Int {
        val insertQuery = """
            INSERT INTO household_members (id, householdId, memberId, role, invitedBy)
            VALUES (:id, :householdId, :memberId, :role, :invitedBy)
        """

        return insert(
            handle,
            insertQuery,
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
