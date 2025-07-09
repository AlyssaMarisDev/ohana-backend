package com.ohana.household.handlers

import com.ohana.exceptions.NotFoundException
import com.ohana.shared.HouseholdMemberRole
import com.ohana.utils.DatabaseUtils.Companion.get
import com.ohana.utils.DatabaseUtils.Companion.insert
import com.ohana.utils.DatabaseUtils.Companion.transaction
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class HouseholdCreationHandler(
    private val jdbi: Jdbi,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    data class Request(
        val id: String,
        val name: String,
        val description: String,
    )

    data class Response(
        val id: String,
        val name: String,
        val description: String,
    )

    suspend fun handle(
        userId: String,
        request: Request,
    ): Response =
        transaction(jdbi) { handle ->
            val insertedRows = insertHousehold(handle, userId, request)

            if (insertedRows == 0) {
                throw Exception("Failed to create household")
            }

            insertHouseholdMember(handle, userId, request)

            if (insertedRows == 0) {
                throw Exception("Failed to add household member")
            }

            getHouseholdById(handle, request.id)
        }

    private fun insertHousehold(
        handle: Handle,
        userId: String,
        request: Request,
    ): Int {
        val insertQuery = """
            INSERT INTO households (id, name, description, createdBy)
            VALUES (:id, :name, :description, :createdBy)
        """

        return insert(
            handle,
            insertQuery,
            mapOf(
                "id" to request.id,
                "name" to request.name,
                "description" to request.description,
                "createdBy" to userId,
            ),
        )
    }

    private fun insertHouseholdMember(
        handle: Handle,
        userId: String,
        request: Request,
    ): Int {
        val insertQuery = """
            INSERT INTO household_members (id, householdId, memberId, role, isActive, invitedBy, joinedAt)
            VALUES (:id, :householdId, :memberId, :role, :isActive, :invitedBy, :joinedAt)
        """

        return insert(
            handle,
            insertQuery,
            mapOf(
                "id" to UUID.randomUUID().toString(),
                "householdId" to request.id,
                "memberId" to userId,
                "role" to HouseholdMemberRole.admin.name,
                "isActive" to true,
                "invitedBy" to userId,
                "joinedAt" to Instant.now(),
            ),
        )
    }

    private fun getHouseholdById(
        handle: Handle,
        id: String,
    ): Response {
        val selectQuery = """
            SELECT id, name, description, createdBy
            FROM households
            WHERE id = :id
        """

        return get(
            handle,
            selectQuery,
            mapOf("id" to id),
            Response::class,
        ).firstOrNull() ?: throw NotFoundException("Household not found")
    }
}
