package com.ohana.api.household.models

import com.ohana.shared.Guid
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
            val validMemberId = Guid.generate()
            val validTagIds = listOf(Guid.generate(), Guid.generate())
            val request =
                HouseholdInviteMemberRequest(
                    memberId = validMemberId,
                    role = "MEMBER",
                    tagIds = validTagIds,
                )

            val domainRequest = request.toDomain()

            assertEquals(validMemberId, domainRequest.memberId)
            assertEquals(HouseholdMemberRole.MEMBER, domainRequest.role)
            assertEquals(validTagIds, domainRequest.tagIds)
        }

    @Test
    fun `toDomain should pass when tagIds is null`() =
        runTest {
            val validMemberId = Guid.generate()
            val request =
                HouseholdInviteMemberRequest(
                    memberId = validMemberId,
                    role = "ADMIN",
                    tagIds = null,
                )

            val domainRequest = request.toDomain()

            assertEquals(validMemberId, domainRequest.memberId)
            assertEquals(HouseholdMemberRole.ADMIN, domainRequest.role)
            assertEquals(emptyList<String>(), domainRequest.tagIds)
        }

    @Test
    fun `toDomain should pass when tagIds is empty`() =
        runTest {
            val validMemberId = Guid.generate()
            val request =
                HouseholdInviteMemberRequest(
                    memberId = validMemberId,
                    role = "ADMIN",
                    tagIds = emptyList(),
                )

            val domainRequest = request.toDomain()

            assertEquals(validMemberId, domainRequest.memberId)
            assertEquals(HouseholdMemberRole.ADMIN, domainRequest.role)
            assertEquals(emptyList<String>(), domainRequest.tagIds)
        }

    @Test
    fun `toDomain should throw ValidationException when memberId is null`() =
        runTest {
            val request =
                HouseholdInviteMemberRequest(
                    memberId = null,
                    role = "MEMBER",
                    tagIds = emptyList(),
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("memberId", exception.errors!![0].field)
            assertEquals("Member ID is required and must be a valid GUID", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when memberId is invalid GUID`() =
        runTest {
            val request =
                HouseholdInviteMemberRequest(
                    memberId = "invalid-guid",
                    role = "MEMBER",
                    tagIds = emptyList(),
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("memberId", exception.errors!![0].field)
            assertEquals("Member ID is required and must be a valid GUID", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when role is null`() =
        runTest {
            val request =
                HouseholdInviteMemberRequest(
                    memberId = Guid.generate(),
                    role = null,
                    tagIds = emptyList(),
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("role", exception.errors!![0].field)
            assertEquals("Role is required and must be one of: ADMIN, MEMBER", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when role is invalid`() =
        runTest {
            val request =
                HouseholdInviteMemberRequest(
                    memberId = Guid.generate(),
                    role = "INVALID_ROLE",
                    tagIds = emptyList(),
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("role", exception.errors!![0].field)
            assertEquals("Role must be one of: ADMIN, MEMBER", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when tagIds contains invalid GUID`() =
        runTest {
            val validMemberId = Guid.generate()
            val invalidTagIds = listOf(Guid.generate(), "invalid-guid", Guid.generate())
            val request =
                HouseholdInviteMemberRequest(
                    memberId = validMemberId,
                    role = "MEMBER",
                    tagIds = invalidTagIds,
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("tagIds", exception.errors!![0].field)
            assertEquals("All tag IDs must be valid GUIDs", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException with multiple errors`() =
        runTest {
            val invalidTagIds = listOf("invalid-guid-1", "invalid-guid-2")
            val request =
                HouseholdInviteMemberRequest(
                    memberId = "invalid-member-guid",
                    role = "INVALID_ROLE",
                    tagIds = invalidTagIds,
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(3, exception.errors!!.size)

            val errorFields = exception.errors!!.map { it.field }.toSet()
            assertEquals(setOf("memberId", "role", "tagIds"), errorFields)
        }

    @Test
    fun `toDomain should accept all valid role values`() =
        runTest {
            val validRoles = listOf("ADMIN", "MEMBER")
            val validMemberId = Guid.generate()

            validRoles.forEach { role ->
                val request =
                    HouseholdInviteMemberRequest(
                        memberId = validMemberId,
                        role = role,
                        tagIds = emptyList(),
                    )

                val domainRequest = request.toDomain()
                assertEquals(HouseholdMemberRole.valueOf(role), domainRequest.role)
            }
        }

    @Test
    fun `toDomain should accept valid GUID formats for memberId`() =
        runTest {
            val validGuids = listOf(Guid.generate(), Guid.generate(), Guid.generate())

            validGuids.forEach { guid ->
                val request =
                    HouseholdInviteMemberRequest(
                        memberId = guid,
                        role = "MEMBER",
                        tagIds = emptyList(),
                    )

                val domainRequest = request.toDomain()
                assertEquals(guid, domainRequest.memberId)
            }
        }

    @Test
    fun `toDomain should accept valid GUID formats for tagIds`() =
        runTest {
            val validTagIds = listOf(Guid.generate(), Guid.generate(), Guid.generate())
            val request =
                HouseholdInviteMemberRequest(
                    memberId = Guid.generate(),
                    role = "MEMBER",
                    tagIds = validTagIds,
                )

            val domainRequest = request.toDomain()
            assertEquals(validTagIds, domainRequest.tagIds)
        }

    @Test
    fun `toDomain should handle case sensitivity for role values`() =
        runTest {
            val request =
                HouseholdInviteMemberRequest(
                    memberId = Guid.generate(),
                    role = "member", // lowercase
                    tagIds = emptyList(),
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("role", exception.errors!![0].field)
            assertEquals("Role must be one of: ADMIN, MEMBER", exception.errors!![0].message)
        }
}
