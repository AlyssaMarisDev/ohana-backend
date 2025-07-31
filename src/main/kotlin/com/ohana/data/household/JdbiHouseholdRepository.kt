package com.ohana.data.household

import com.ohana.data.utils.DatabaseUtils
import com.ohana.shared.enums.HouseholdMemberStatus
import com.ohana.shared.exceptions.DbException
import com.ohana.shared.exceptions.NotFoundException
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet

class JdbiHouseholdRepository(
    private val handle: Handle,
) : HouseholdRepository {
    init {
        handle.registerRowMapper(HouseholdRowMapper())
        handle.registerRowMapper(HouseholdMemberRowMapper())
    }

    override fun findById(id: String): Household? {
        val selectQuery = """
            SELECT id, name, description, created_by
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
            SELECT id, name, description, created_by
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

    override fun findByMemberId(memberId: String): List<Household> {
        val selectQuery = """
            SELECT h.id, h.name, h.description, h.created_by
            FROM households h
            INNER JOIN household_members hm ON h.id = hm.household_id
            WHERE hm.member_id = :memberId AND hm.status = 'active'
        """

        return DatabaseUtils
            .get(
                handle,
                selectQuery,
                mapOf("memberId" to memberId),
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
            SELECT id, household_id, member_id, role, status, is_default, invited_by, joined_at
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

    override fun findHouseholdMemberById(householdMemberId: String): HouseholdMember? {
        val selectQuery = """
            SELECT id, household_id, member_id, role, status, is_default, invited_by, joined_at
            FROM household_members
            WHERE id = :householdMemberId
        """

        return DatabaseUtils
            .get(
                handle,
                selectQuery,
                mapOf("householdMemberId" to householdMemberId),
                HouseholdMember::class,
            ).firstOrNull()
    }

    override fun findMembersByHouseholdId(householdId: String): List<HouseholdMember> {
        val selectQuery = """
            SELECT id, household_id, member_id, role, status, is_default, invited_by, joined_at
            FROM household_members
            WHERE household_id = :householdId
        """

        return DatabaseUtils
            .get(
                handle,
                selectQuery,
                mapOf("householdId" to householdId),
                HouseholdMember::class,
            )
    }

    override fun createMember(member: HouseholdMember): HouseholdMember {
        val insertQuery = """
            INSERT INTO household_members (id, household_id, member_id, role, status, is_default, invited_by, joined_at)
            VALUES (:id, :householdId, :memberId, :role, :status, :isDefault, :invitedBy, :joinedAt)
        """

        val insertedRows =
            DatabaseUtils.insert(
                handle,
                insertQuery,
                mapOf(
                    "id" to member.id,
                    "householdId" to member.householdId,
                    "memberId" to member.memberId,
                    "role" to member.role.name,
                    "status" to member.status.name.lowercase(),
                    "isDefault" to member.isDefault,
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
            SET status = :status,
                is_default = :isDefault,
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
                    "status" to member.status.name.lowercase(),
                    "isDefault" to member.isDefault,
                    "joinedAt" to member.joinedAt,
                ),
            )

        if (updatedRows == 0) throw DbException("Failed to update household member")

        return findMemberById(member.householdId, member.memberId)
            ?: throw NotFoundException("Household member not found after update")
    }

    override fun findDefaultHouseholdByMemberId(memberId: String): HouseholdMember? {
        val selectQuery = """
            SELECT id, household_id, member_id, role, status, is_default, invited_by, joined_at
            FROM household_members
            WHERE member_id = :memberId AND is_default = true
        """

        return DatabaseUtils
            .get(
                handle,
                selectQuery,
                mapOf("memberId" to memberId),
                HouseholdMember::class,
            ).firstOrNull()
    }

    override fun setDefaultHousehold(
        memberId: String,
        householdId: String,
    ): HouseholdMember {
        // First, clear any existing default household for this member
        val clearDefaultQuery = """
            UPDATE household_members
            SET is_default = false
            WHERE member_id = :memberId AND is_default = true
        """

        DatabaseUtils.update(
            handle,
            clearDefaultQuery,
            mapOf("memberId" to memberId),
        )

        // Then set the new default household
        val setDefaultQuery = """
            UPDATE household_members
            SET is_default = true
            WHERE member_id = :memberId AND household_id = :householdId
        """

        val updatedRows =
            DatabaseUtils.update(
                handle,
                setDefaultQuery,
                mapOf(
                    "memberId" to memberId,
                    "householdId" to householdId,
                ),
            )

        if (updatedRows == 0) throw DbException("Failed to set default household")

        return findMemberById(householdId, memberId)
            ?: throw NotFoundException("Household member not found after setting default")
    }

    private class HouseholdRowMapper : RowMapper<Household> {
        override fun map(
            rs: ResultSet,
            ctx: StatementContext,
        ): Household =
            Household(
                id = rs.getString("id"),
                name = rs.getString("name"),
                description = rs.getString("description"),
                createdBy = rs.getString("created_by"),
            )
    }

    private class HouseholdMemberRowMapper : RowMapper<HouseholdMember> {
        override fun map(
            rs: ResultSet,
            ctx: StatementContext,
        ): HouseholdMember =
            HouseholdMember(
                id = rs.getString("id"),
                householdId = rs.getString("household_id"),
                memberId = rs.getString("member_id"),
                role =
                    com.ohana.shared.enums.HouseholdMemberRole
                        .valueOf(rs.getString("role").uppercase()),
                status = HouseholdMemberStatus.valueOf(rs.getString("status").uppercase()),
                isDefault = rs.getBoolean("is_default"),
                invitedBy = rs.getString("invited_by"),
                joinedAt = rs.getTimestamp("joined_at")?.toInstant(),
            )
    }
}
