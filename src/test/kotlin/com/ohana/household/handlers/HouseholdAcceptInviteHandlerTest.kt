package com.ohana.household.handlers

import com.ohana.TestUtils
import com.ohana.exceptions.AuthorizationException
import com.ohana.shared.HouseholdMember
import com.ohana.shared.HouseholdMemberRole
import com.ohana.shared.HouseholdRepository
import com.ohana.shared.UnitOfWork
import com.ohana.shared.UnitOfWorkContext
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class HouseholdAcceptInviteHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var householdRepository: HouseholdRepository
    private lateinit var handler: HouseholdAcceptInviteHandler

    @BeforeEach
    fun setUp() {
        householdRepository = mock()
        context =
            mock {
                on { households } doReturn householdRepository
            }
        unitOfWork = mock()
        handler = HouseholdAcceptInviteHandler(unitOfWork)
    }

    @Test
    fun `handle should accept invite when user is invited`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            val invitedMember =
                TestUtils.getHouseholdMember(
                    householdId = householdId,
                    memberId = userId,
                    role = HouseholdMemberRole.member,
                    isActive = false, // Not yet active
                    invitedBy = UUID.randomUUID().toString(),
                )

            whenever(householdRepository.findMemberById(householdId, userId)).thenReturn(invitedMember)
            whenever(householdRepository.updateMember(any())).thenReturn(invitedMember.copy(isActive = true, joinedAt = any()))

            handler.handle(userId, householdId)

            verify(householdRepository).findMemberById(householdId, userId)
            verify(householdRepository).updateMember(any())
        }

    @Test
    fun `handle should accept invite for admin role`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            val invitedMember =
                TestUtils.getHouseholdMember(
                    householdId = householdId,
                    memberId = userId,
                    role = HouseholdMemberRole.admin,
                    isActive = false,
                    invitedBy = UUID.randomUUID().toString(),
                )

            whenever(householdRepository.findMemberById(householdId, userId)).thenReturn(invitedMember)
            whenever(householdRepository.updateMember(any())).thenReturn(invitedMember.copy(isActive = true, joinedAt = any()))

            handler.handle(userId, householdId)

            verify(householdRepository).findMemberById(householdId, userId)
            verify(householdRepository).updateMember(any())
        }

    @Test
    fun `handle should accept invite when already active member`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            val activeMember =
                TestUtils.getHouseholdMember(
                    householdId = householdId,
                    memberId = userId,
                    role = HouseholdMemberRole.member,
                    isActive = true, // Already active
                    invitedBy = UUID.randomUUID().toString(),
                    joinedAt = Instant.now().minusSeconds(3600), // Joined 1 hour ago
                )

            whenever(householdRepository.findMemberById(householdId, userId)).thenReturn(activeMember)
            whenever(householdRepository.updateMember(any())).thenReturn(activeMember.copy(joinedAt = any()))

            handler.handle(userId, householdId)

            verify(householdRepository).findMemberById(householdId, userId)
            verify(householdRepository).updateMember(any())
        }

    @Test
    fun `handle should throw AuthorizationException when user is not invited`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            whenever(householdRepository.findMemberById(householdId, userId)).thenReturn(null)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(userId, householdId)
                }

            assertEquals("User has not been invited to the household", ex.message)
            verify(householdRepository).findMemberById(householdId, userId)
            verify(householdRepository, never()).updateMember(any())
        }

    @Test
    fun `handle should propagate exception from repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            val invitedMember =
                TestUtils.getHouseholdMember(
                    householdId = householdId,
                    memberId = userId,
                    role = HouseholdMemberRole.member,
                    isActive = false,
                    invitedBy = UUID.randomUUID().toString(),
                )

            whenever(householdRepository.findMemberById(householdId, userId)).thenReturn(invitedMember)
            whenever(householdRepository.updateMember(any())).thenThrow(RuntimeException("DB error"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(userId, householdId)
                }

            assertEquals("DB error", ex.message)
            verify(householdRepository).findMemberById(householdId, userId)
            verify(householdRepository).updateMember(any())
        }

    @Test
    fun `handle should update member with current timestamp`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            val invitedMember =
                TestUtils.getHouseholdMember(
                    householdId = householdId,
                    memberId = userId,
                    role = HouseholdMemberRole.member,
                    isActive = false,
                    invitedBy = UUID.randomUUID().toString(),
                )

            whenever(householdRepository.findMemberById(householdId, userId)).thenReturn(invitedMember)
            whenever(householdRepository.updateMember(any())).thenAnswer { invocation ->
                val member = invocation.getArgument<HouseholdMember>(0)
                member
            }

            handler.handle(userId, householdId)

            verify(householdRepository).updateMember(
                argThat { member ->
                    member.isActive && member.joinedAt != null
                },
            )
        }
}
