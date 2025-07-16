package com.ohana.domain.household

import com.ohana.TestUtils
import com.ohana.data.household.HouseholdRepository
import com.ohana.data.unitOfWork.*
import com.ohana.shared.enums.HouseholdMemberRole
import com.ohana.shared.exceptions.AuthorizationException
import com.ohana.shared.exceptions.ConflictException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.UUID
import kotlin.test.assertEquals

class HouseholdInviteMemberHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var householdRepository: HouseholdRepository
    private lateinit var handler: HouseholdInviteMemberHandler

    @BeforeEach
    fun setUp() {
        householdRepository = mock()
        context =
            mock {
                on { households } doReturn householdRepository
            }
        unitOfWork = mock()
        handler = HouseholdInviteMemberHandler(unitOfWork)
    }

    @Test
    fun `handle should invite member successfully`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val memberId = UUID.randomUUID().toString()
            val request =
                HouseholdInviteMemberHandler.Request(
                    memberId = memberId,
                    role = HouseholdMemberRole.member,
                )
            val adminMember =
                TestUtils.getHouseholdMember(
                    householdId = householdId,
                    memberId = userId,
                    role = HouseholdMemberRole.admin,
                )

            whenever(householdRepository.findMemberById(householdId, userId)).thenReturn(adminMember)
            whenever(householdRepository.findMemberById(householdId, memberId)).thenReturn(null)
            whenever(householdRepository.createMember(any())).thenReturn(
                TestUtils.getHouseholdMember(
                    householdId = householdId,
                    memberId = memberId,
                    role = HouseholdMemberRole.member,
                    isActive = false,
                    invitedBy = userId,
                ),
            )

            handler.handle(userId, householdId, request)

            verify(householdRepository).findMemberById(householdId, userId)
            verify(householdRepository).findMemberById(householdId, memberId)
            verify(householdRepository).createMember(
                argThat {
                    this.householdId == householdId &&
                        this.memberId == memberId &&
                        this.role == HouseholdMemberRole.member &&
                        !this.isActive &&
                        this.invitedBy == userId
                },
            )
            verifyNoMoreInteractions(householdRepository)
        }

    @Test
    fun `handle should throw AuthorizationException if user is not a member`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val memberId = UUID.randomUUID().toString()
            val request =
                HouseholdInviteMemberHandler.Request(
                    memberId = memberId,
                    role = HouseholdMemberRole.member,
                )

            whenever(householdRepository.findMemberById(householdId, userId)).thenReturn(null)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(userId, householdId, request)
                }
            assertEquals("User is not a member of the household", ex.message)
            verify(householdRepository).findMemberById(householdId, userId)
            verifyNoMoreInteractions(householdRepository)
        }

    @Test
    fun `handle should throw AuthorizationException if user is not an admin`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val memberId = UUID.randomUUID().toString()
            val request =
                HouseholdInviteMemberHandler.Request(
                    memberId = memberId,
                    role = HouseholdMemberRole.member,
                )
            val nonAdminMember =
                TestUtils.getHouseholdMember(
                    householdId = householdId,
                    memberId = userId,
                    role = HouseholdMemberRole.member,
                )

            whenever(householdRepository.findMemberById(householdId, userId)).thenReturn(nonAdminMember)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(userId, householdId, request)
                }
            assertEquals("User is not an admin of the household", ex.message)
            verify(householdRepository).findMemberById(householdId, userId)
            verifyNoMoreInteractions(householdRepository)
        }

    @Test
    fun `handle should throw ConflictException if user is already a member`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val memberId = UUID.randomUUID().toString()
            val request =
                HouseholdInviteMemberHandler.Request(
                    memberId = memberId,
                    role = HouseholdMemberRole.member,
                )
            val adminMember =
                TestUtils.getHouseholdMember(
                    householdId = householdId,
                    memberId = userId,
                    role = HouseholdMemberRole.admin,
                )
            val alreadyMember =
                TestUtils.getHouseholdMember(
                    householdId = householdId,
                    memberId = memberId,
                    role = HouseholdMemberRole.member,
                )

            whenever(householdRepository.findMemberById(householdId, userId)).thenReturn(adminMember)
            whenever(householdRepository.findMemberById(householdId, memberId)).thenReturn(alreadyMember)

            val ex =
                assertThrows<ConflictException> {
                    handler.handle(userId, householdId, request)
                }
            assertEquals("User is already a member of the household", ex.message)
            verify(householdRepository).findMemberById(householdId, userId)
            verify(householdRepository).findMemberById(householdId, memberId)
            verifyNoMoreInteractions(householdRepository)
        }

    @Test
    fun `handle should propagate exception from repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val memberId = UUID.randomUUID().toString()
            val request =
                HouseholdInviteMemberHandler.Request(
                    memberId = memberId,
                    role = HouseholdMemberRole.member,
                )
            val adminMember =
                TestUtils.getHouseholdMember(
                    householdId = householdId,
                    memberId = userId,
                    role = HouseholdMemberRole.admin,
                )

            whenever(householdRepository.findMemberById(householdId, userId)).thenReturn(adminMember)
            whenever(householdRepository.findMemberById(householdId, memberId)).thenReturn(null)
            whenever(householdRepository.createMember(any())).thenThrow(RuntimeException("DB error"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(userId, householdId, request)
                }
            assertEquals("DB error", ex.message)
            verify(householdRepository).findMemberById(householdId, userId)
            verify(householdRepository).findMemberById(householdId, memberId)
            verify(householdRepository).createMember(any())
            verifyNoMoreInteractions(householdRepository)
        }
}
