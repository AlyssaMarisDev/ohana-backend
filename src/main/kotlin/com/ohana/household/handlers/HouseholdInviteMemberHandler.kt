package com.ohana.household.handlers

import com.ohana.exceptions.AuthorizationException
import com.ohana.exceptions.ConflictException
import com.ohana.exceptions.DbException
import com.ohana.shared.HouseholdMemberRole
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
        val role: HouseholdMemberRole,
    )

    suspend fun handle(
        userId: String,
        householdId: String,
        request: Request,
    ) = transaction(jdbi) { handle ->
        val actorHouseholdMember = getHouseholdMember(handle, userId, householdId)
        validateAuthorization(actorHouseholdMember)

        val memberHouseholdMember = getHouseholdMember(handle, request.memberId, householdId)
        validateMember(memberHouseholdMember)

        val insertedRows = addHouseholdMember(handle, userId, householdId, request)

        if (insertedRows == 0) {
            throw DbException("Failed to add member to household")
        }
    }

    private fun validateAuthorization(actorHouseholdMember: HouseholdMember?) {
        if (actorHouseholdMember == null) {
            throw AuthorizationException("User is not a member of the household")
        }

        if (actorHouseholdMember.role != HouseholdMemberRole.admin.name) {
            throw AuthorizationException("User is not an admin of the household")
        }
    }

    private fun validateMember(memberHouseholdMember: HouseholdMember?) {
        if (memberHouseholdMember != null) {
            throw ConflictException("User is already a member of the household")
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
                "role" to request.role.name,
                "invitedBy" to userId,
            ),
        )
    }

    private fun getHouseholdMember(
        handle: Handle,
        memberId: String,
        householdId: String,
    ): HouseholdMember? {
        val selectQuery = """
            SELECT id, role FROM household_members
            WHERE householdId = :householdId
            AND memberId = :memberId
        """

        return get(
            handle,
            selectQuery,
            mapOf(
                "householdId" to householdId,
                "memberId" to memberId,
            ),
            HouseholdMember::class,
        ).firstOrNull()
    }

    data class HouseholdMember(
        val id: String,
        val role: String,
    )
}
