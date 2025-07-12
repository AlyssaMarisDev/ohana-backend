package com.ohana.shared

import com.ohana.TestUtils
import com.ohana.exceptions.AuthorizationException
import com.ohana.exceptions.NotFoundException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.UUID

class HouseholdMemberValidatorTest {
    private lateinit var validator: HouseholdMemberValidator
    private lateinit var context: UnitOfWorkContext
    private lateinit var householdRepository: HouseholdRepository

    @BeforeEach
    fun setUp() {
        householdRepository = mock()
        context =
            mock {
                on { households } doReturn householdRepository
            }
        validator = HouseholdMemberValidator()
    }

    @Test
    fun `validate should pass when household exists and user is member`() {
        // Given
        val household = TestUtils.getHousehold()
        val householdMember = TestUtils.getHouseholdMember()

        whenever(householdRepository.findById(household.id)).thenReturn(household)
        whenever(householdRepository.findMemberById(household.id, householdMember.memberId)).thenReturn(householdMember)

        // When & Then
        assertDoesNotThrow {
            validator.validate(context, household.id, householdMember.memberId)
        }

        verify(householdRepository).findById(household.id)
        verify(householdRepository).findMemberById(household.id, householdMember.memberId)
        verifyNoMoreInteractions(householdRepository)
    }

    @Test
    fun `validate should throw NotFoundException when household does not exist`() {
        // Given
        val householdId = UUID.randomUUID().toString()
        whenever(householdRepository.findById(householdId)).thenReturn(null)

        // When & Then
        val ex =
            assertThrows<NotFoundException> {
                validator.validate(context, householdId, UUID.randomUUID().toString())
            }

        assertEquals("Household not found", ex.message)

        verify(householdRepository).findById(householdId)
        verifyNoMoreInteractions(householdRepository)
    }

    @Test
    fun `validate should throw AuthorizationException when household exists but user is not member`() {
        // Given
        val household = TestUtils.getHousehold()
        val userId = UUID.randomUUID().toString()

        whenever(householdRepository.findById(household.id)).thenReturn(household)
        whenever(householdRepository.findMemberById(household.id, userId)).thenReturn(null)

        // When & Then
        val ex =
            assertThrows<AuthorizationException> {
                validator.validate(context, household.id, userId)
            }

        assertEquals("User is not a member of the household", ex.message)

        verify(householdRepository).findById(household.id)
        verify(householdRepository).findMemberById(household.id, userId)
        verifyNoMoreInteractions(householdRepository)
    }

    @Test
    fun `validate should throw AuthorizationException when household exists but user is inactive member`() {
        // Given
        val household = TestUtils.getHousehold()
        val inactiveMember = TestUtils.getHouseholdMember(isActive = false)

        whenever(householdRepository.findById(household.id)).thenReturn(household)
        whenever(householdRepository.findMemberById(household.id, inactiveMember.memberId)).thenReturn(inactiveMember)

        // When & Then
        val ex =
            assertThrows<AuthorizationException> {
                validator.validate(context, household.id, inactiveMember.memberId)
            }

        assertEquals("User is not an active member of the household", ex.message)

        verify(householdRepository).findById(household.id)
        verify(householdRepository).findMemberById(household.id, inactiveMember.memberId)
        verifyNoMoreInteractions(householdRepository)
    }

    @Test
    fun `validate should work with different household and user IDs`() {
        // Given
        val differentHouseholdId = UUID.randomUUID().toString()
        val differentUserId = UUID.randomUUID().toString()
        val household = TestUtils.getHousehold(id = differentHouseholdId)
        val householdMember =
            TestUtils.getHouseholdMember(
                householdId = differentHouseholdId,
                memberId = differentUserId,
            )

        whenever(householdRepository.findById(household.id)).thenReturn(household)
        whenever(householdRepository.findMemberById(household.id, householdMember.memberId)).thenReturn(householdMember)

        // When & Then
        assertDoesNotThrow {
            validator.validate(context, differentHouseholdId, differentUserId)
        }

        verify(householdRepository).findById(differentHouseholdId)
        verify(householdRepository).findMemberById(differentHouseholdId, differentUserId)
        verifyNoMoreInteractions(householdRepository)
    }

    @Test
    fun `validate should not work with empty string IDs`() {
        // Given
        val emptyHouseholdId = ""
        val emptyUserId = ""

        // When & Then
        assertThrows<IllegalArgumentException> {
            validator.validate(context, emptyHouseholdId, emptyUserId)
        }
    }

    @Test
    fun `validate should not work with invalid GUID IDs`() {
        // Given
        val invalidHouseholdId = "invalid-guid"
        val invalidUserId = "invalid-guid"

        // When & Then
        assertThrows<IllegalArgumentException> {
            validator.validate(context, invalidHouseholdId, invalidUserId)
        }
    }

    @Test
    fun `validate should work with active members`() {
        // Given
        val household = TestUtils.getHousehold()
        val adminMember =
            TestUtils.getHouseholdMember(
                isActive = true,
            )

        whenever(householdRepository.findById(household.id)).thenReturn(household)
        whenever(householdRepository.findMemberById(household.id, adminMember.memberId)).thenReturn(adminMember)

        // When & Then
        assertDoesNotThrow {
            validator.validate(context, household.id, adminMember.memberId)
        }

        verify(householdRepository).findById(household.id)
        verify(householdRepository).findMemberById(household.id, adminMember.memberId)
        verifyNoMoreInteractions(householdRepository)
    }
}
