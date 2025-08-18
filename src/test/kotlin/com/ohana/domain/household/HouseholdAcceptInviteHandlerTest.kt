package com.ohana.domain.household

import com.ohana.TestUtils
import com.ohana.data.household.HouseholdRepository
import com.ohana.data.unitOfWork.*
import com.ohana.shared.enums.HouseholdMemberStatus
import com.ohana.shared.exceptions.AuthorizationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.Instant
import java.util.UUID

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
    fun `handle should activate invitation and set joinedAt`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val invitedMember =
                TestUtils.getHouseholdMember(
                    householdId = householdId,
                    memberId = userId,
                    status = HouseholdMemberStatus.INVITED,
                )

            whenever(householdRepository.findMemberById(householdId, userId)).thenReturn(invitedMember)
            whenever(householdRepository.updateMember(any())).thenAnswer { it.getArgument(0) }

            handler.handle(userId, householdId)

            verify(householdRepository).updateMember(
                argThat { updated ->
                    updated.id == invitedMember.id &&
                        updated.status == HouseholdMemberStatus.ACTIVE &&
                        updated.joinedAt != null &&
                        updated.joinedAt!!.isAfter(Instant.now().minusSeconds(5))
                },
            )
        }

    @Test
    fun `handle should throw when user not invited`() =
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
            verify(householdRepository, never()).updateMember(any())
        }
}


