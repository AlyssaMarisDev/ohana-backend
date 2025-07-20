package com.ohana.api.household.models

import com.ohana.shared.enums.HouseholdMemberRole
import com.ohana.shared.exceptions.ValidationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class HouseholdInviteMemberRequestTest {
    @Test
    fun `toDomain should pass when all fields are valid`() =
        runTest {
            val request =
                HouseholdInviteMemberRequest(
                    memberId = "550e8400-e29b-41d4-a716-446655440000",
                    role = "ADMIN",
                )

            val domainRequest = request.toDomain()

            assertEquals("550e8400-e29b-41d4-a716-446655440000", domainRequest.memberId)
            assertEquals(HouseholdMemberRole.ADMIN, domainRequest.role)
        }

    @Test
    fun `toDomain should throw ValidationException when memberId is null`() =
        runTest {
            val request =
                HouseholdInviteMemberRequest(
                    memberId = null,
                    role = "ADMIN",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors.size)
            assertEquals("memberId", exception.errors[0].field)
            assertEquals("Member ID is required", exception.errors[0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when memberId is blank`() =
        runTest {
            val request =
                HouseholdInviteMemberRequest(
                    memberId = "",
                    role = "ADMIN",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors.size)
            assertEquals("memberId", exception.errors[0].field)
            assertEquals("Member ID cannot be blank", exception.errors[0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when memberId is not a valid GUID`() =
        runTest {
            val request =
                HouseholdInviteMemberRequest(
                    memberId = "invalid-guid",
                    role = "ADMIN",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors.size)
            assertEquals("memberId", exception.errors[0].field)
            assertEquals("Member ID must be a valid GUID", exception.errors[0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when role is null`() =
        runTest {
            val request =
                HouseholdInviteMemberRequest(
                    memberId = "550e8400-e29b-41d4-a716-446655440000",
                    role = null,
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors.size)
            assertEquals("role", exception.errors[0].field)
            assertEquals("Role is required", exception.errors[0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when role is invalid`() =
        runTest {
            val request =
                HouseholdInviteMemberRequest(
                    memberId = "550e8400-e29b-41d4-a716-446655440000",
                    role = "INVALID_ROLE",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors.size)
            assertEquals("role", exception.errors[0].field)
            assertEquals("Role must be one of: ADMIN, MEMBER", exception.errors[0].message)
        }

    @Test
    fun `toDomain should throw ValidationException with multiple errors`() =
        runTest {
            val request =
                HouseholdInviteMemberRequest(
                    memberId = "", // Blank memberId
                    role = "INVALID_ROLE", // Invalid role
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(2, exception.errors.size)

            val errorFields = exception.errors.map { it.field }.toSet()
            assertEquals(setOf("memberId", "role"), errorFields)
        }

    @Test
    fun `toDomain should accept all valid role values`() =
        runTest {
            val validRoles = listOf("ADMIN", "MEMBER")

            validRoles.forEach { role ->
                val request =
                    HouseholdInviteMemberRequest(
                        memberId = "550e8400-e29b-41d4-a716-446655440000",
                        role = role,
                    )

                val domainRequest = request.toDomain()
                assertEquals(HouseholdMemberRole.valueOf(role), domainRequest.role)
            }
        }

    @Test
    fun `toDomain should accept valid GUID formats`() =
        runTest {
            val validGuids =
                listOf(
                    "550e8400-e29b-41d4-a716-446655440000",
                    "550e8400-e29b-41d4-a716-446655440001",
                    "550e8400-e29b-41d4-a716-446655440002",
                )

            validGuids.forEach { guid ->
                val request =
                    HouseholdInviteMemberRequest(
                        memberId = guid,
                        role = "MEMBER",
                    )

                val domainRequest = request.toDomain()
                assertEquals(guid, domainRequest.memberId)
            }
        }
}
