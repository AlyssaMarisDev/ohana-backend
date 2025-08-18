package com.ohana.domain.household

import com.ohana.TestUtils
import com.ohana.data.household.HouseholdRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.permissions.TagPermissionManager
import com.ohana.shared.enums.HouseholdMemberRole
import com.ohana.shared.enums.HouseholdMemberStatus
import com.ohana.shared.exceptions.AuthorizationException
import com.ohana.shared.exceptions.ConflictException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.UUID

class HouseholdInviteMemberHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var householdRepository: HouseholdRepository
    private lateinit var tagPermissionManager: TagPermissionManager
    private lateinit var handler: HouseholdInviteMemberHandler

    @BeforeEach
    fun setUp() {
        householdRepository = mock()
        tagPermissionManager = mock()
        context = mock { on { households } doReturn householdRepository }
        unitOfWork = mock()
        handler = HouseholdInviteMemberHandler(unitOfWork, tagPermissionManager)
    }

    @Test
    fun `handle should invite member and set permissions`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val adminId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val targetMemberId = UUID.randomUUID().toString()
            val adminMember =
                TestUtils.getHouseholdMember(
                    householdId = householdId,
                    memberId = adminId,
                    role = HouseholdMemberRole.ADMIN,
                    status = HouseholdMemberStatus.ACTIVE,
                )

            whenever(householdRepository.findMemberById(householdId, adminId)).thenReturn(adminMember)
            whenever(householdRepository.findMemberById(householdId, targetMemberId)).thenReturn(null)
            whenever(householdRepository.createMember(any())).thenAnswer { it.getArgument(0) }

            val request =
                HouseholdInviteMemberHandler.Request(
                    memberId = targetMemberId,
                    role = HouseholdMemberRole.MEMBER,
                    tagIds = listOf(UUID.randomUUID().toString()),
                )

            handler.handle(adminId, householdId, request)

            verify(householdRepository).createMember(
                argThat { m ->
                    m.householdId == householdId &&
                        m.memberId == targetMemberId &&
                        m.status == HouseholdMemberStatus.INVITED &&
                        m.invitedBy == adminId
                },
            )
            verify(tagPermissionManager).createPermissionsWithTags(eq(context), any(), eq(request.tagIds))
        }

    @Test
    fun `handle should forbid non-member`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            whenever(householdRepository.findMemberById(householdId, userId)).thenReturn(null)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(
                        userId,
                        householdId,
                        HouseholdInviteMemberHandler.Request(UUID.randomUUID().toString(), HouseholdMemberRole.MEMBER, emptyList()),
                    )
                }
            assertEquals("User is not a member of the household", ex.message)
        }

    @Test
    fun `handle should forbid non-admin`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val member =
                TestUtils.getHouseholdMember(
                    householdId = householdId,
                    memberId = userId,
                    role = HouseholdMemberRole.MEMBER,
                    status = HouseholdMemberStatus.ACTIVE,
                )

            whenever(householdRepository.findMemberById(householdId, userId)).thenReturn(member)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(
                        userId,
                        householdId,
                        HouseholdInviteMemberHandler.Request(UUID.randomUUID().toString(), HouseholdMemberRole.MEMBER, emptyList()),
                    )
                }
            assertEquals("User is not an admin of the household", ex.message)
        }

    @Test
    fun `handle should throw when invitee already member`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val member =
                TestUtils.getHouseholdMember(
                    householdId = householdId,
                    memberId = userId,
                    role = HouseholdMemberRole.ADMIN,
                    status = HouseholdMemberStatus.ACTIVE,
                )
            val target = TestUtils.getHouseholdMember(householdId = householdId)

            whenever(householdRepository.findMemberById(householdId, userId)).thenReturn(member)
            whenever(householdRepository.findMemberById(householdId, target.memberId)).thenReturn(target)

            val ex =
                assertThrows<ConflictException> {
                    handler.handle(
                        userId,
                        householdId,
                        HouseholdInviteMemberHandler.Request(target.memberId, HouseholdMemberRole.MEMBER, emptyList()),
                    )
                }
            assertEquals("User is already a member of the household", ex.message)
        }
}
