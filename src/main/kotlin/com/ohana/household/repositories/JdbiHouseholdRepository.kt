package com.ohana.household.repositories

import com.ohana.exceptions.DbException
import com.ohana.exceptions.NotFoundException
import com.ohana.shared.Household
import com.ohana.shared.HouseholdMember
import com.ohana.shared.HouseholdRepository
import com.ohana.utils.DatabaseUtils
import org.jdbi.v3.core.Handle

class JdbiHouseholdRepository(
    private val handle: Handle,
) : HouseholdRepository {
    override fun findById(id: String): Household? {
        val selectQuery = """
            SELECT id, name, description, created_by as createdBy
            FROM households
            WHERE id = :id
        """

        return DatabaseUtils
            .get(
                handle,
                selectQuery,
                mapOf("id" to id),
                Household::class,
            ).firstOrNull()
    }

    override fun findAll(): List<Household> {
        val selectQuery = """
            SELECT id, name, description, created_by as createdBy
            FROM households
        """

        return DatabaseUtils
            .get(
                handle,
                selectQuery,
                mapOf(),
                Household::class,
            )
    }

    override fun create(household: Household): Household {
        val insertQuery = """
            INSERT INTO households (id, name, description, created_by)
            VALUES (:id, :name, :description, :createdBy)
        """

        val insertedRows =
            DatabaseUtils.insert(
                handle,
                insertQuery,
                mapOf(
                    "id" to household.id,
                    "name" to household.name,
                    "description" to household.description,
                    "createdBy" to household.createdBy,
                ),
            )

        if (insertedRows == 0) throw DbException("Failed to create household")

        return findById(household.id) ?: throw NotFoundException("Household not found after creation")
    }

    override fun findMemberById(
        householdId: String,
        memberId: String,
    ): HouseholdMember? {
        val selectQuery = """
            SELECT id, household_id as householdId, member_id as memberId, role, is_active as isActive, invited_by as invitedBy, joined_at as joinedAt
            FROM household_members
            WHERE household_id = :householdId AND member_id = :memberId
        """

        return DatabaseUtils
            .get(
                handle,
                selectQuery,
                mapOf(
                    "householdId" to householdId,
                    "memberId" to memberId,
                ),
                HouseholdMember::class,
            ).firstOrNull()
    }

    override fun createMember(member: HouseholdMember): HouseholdMember {
        val insertQuery = """
            INSERT INTO household_members (id, household_id, member_id, role, is_active, invited_by, joined_at)
            VALUES (:id, :householdId, :memberId, :role, :isActive, :invitedBy, :joinedAt)
        """

        val insertedRows =
            DatabaseUtils.insert(
                handle,
                insertQuery,
                mapOf(
                    "id" to member.id,
                    "householdId" to member.householdId,
                    "memberId" to member.memberId,
                    "role" to member.role,
                    "isActive" to member.isActive,
                    "invitedBy" to member.invitedBy,
                    "joinedAt" to member.joinedAt,
                ),
            )

        if (insertedRows == 0) throw DbException("Failed to create household member")

        return findMemberById(member.householdId, member.memberId)
            ?: throw NotFoundException("Household member not found after creation")
    }

    override fun updateMember(member: HouseholdMember): HouseholdMember {
        val updateQuery = """
            UPDATE household_members
            SET is_active = :isActive,
                joined_at = :joinedAt
            WHERE household_id = :householdId AND member_id = :memberId
        """

        val updatedRows =
            DatabaseUtils.update(
                handle,
                updateQuery,
                mapOf(
                    "householdId" to member.householdId,
                    "memberId" to member.memberId,
                    "isActive" to member.isActive,
                    "joinedAt" to member.joinedAt,
                ),
            )

        if (updatedRows == 0) throw DbException("Failed to update household member")

        return findMemberById(member.householdId, member.memberId)
            ?: throw NotFoundException("Household member not found after update")
    }
}
