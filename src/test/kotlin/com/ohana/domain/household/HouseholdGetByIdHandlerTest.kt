package com.ohana.domain.household

import com.ohana.TestUtils
import com.ohana.data.household.HouseholdRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.validators.HouseholdMemberValidator
import com.ohana.shared.exceptions.NotFoundException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.UUID

class HouseholdGetByIdHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var householdRepository: HouseholdRepository
    private lateinit var householdMemberValidator: HouseholdMemberValidator
    private lateinit var handler: HouseholdGetByIdHandler

    @BeforeEach
    fun setUp() {
        householdRepository = mock()
        householdMemberValidator = mock()
        context =
            mock {
                on { households } doReturn householdRepository
            }
        unitOfWork = mock()
        handler = HouseholdGetByIdHandler(unitOfWork, householdMemberValidator)
    }

    @Test
    fun `handle should return household when found and user is authorized`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val householdId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()

            val household =
                TestUtils.getHousehold(
                    id = householdId,
                    name = "Test Household",
                    description = "Test Description",
                    createdBy = userId,
                )

            whenever(householdRepository.findById(householdId)).thenReturn(household)

            val response = handler.handle(userId, householdId)

            assertEquals(household.id, response.id)
            assertEquals(household.name, response.name)
            assertEquals(household.description, response.description)
            assertEquals(household.createdBy, response.createdBy)

            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(householdRepository).findById(householdId)
            verifyNoMoreInteractions(householdRepository)
        }

    @Test
    fun `handle should throw AuthorizationException when user is not member of household`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val householdId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()

            whenever(
                householdMemberValidator.validate(context, householdId, userId),
            ).thenThrow(
                com.ohana.shared.exceptions
                    .AuthorizationException("User is not a member of the household"),
            )

            val ex =
                assertThrows<com.ohana.shared.exceptions.AuthorizationException> {
                    handler.handle(userId, householdId)
                }

            assertEquals("User is not a member of the household", ex.message)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(householdRepository, never()).findById(any())
        }

    @Test
    fun `handle should throw NotFoundException when household does not exist`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val householdId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()

            whenever(householdRepository.findById(householdId)).thenReturn(null)

            val ex =
                assertThrows<NotFoundException> {
                    handler.handle(userId, householdId)
                }

            assertEquals("Household not found", ex.message)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(householdRepository).findById(householdId)
        }

    @Test
    fun `handle should propagate exception from repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val householdId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()

            whenever(householdRepository.findById(householdId)).thenThrow(RuntimeException("DB error"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(userId, householdId)
                }

            assertEquals("DB error", ex.message)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(householdRepository).findById(householdId)
        }

    @Test
    fun `handle should work with household created by different user`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val householdId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()
            val householdCreatorId = UUID.randomUUID().toString() // Different user created the household

            val household =
                TestUtils.getHousehold(
                    id = householdId,
                    name = "Test Household",
                    description = "Test Description",
                    createdBy = householdCreatorId, // Different user
                )

            whenever(householdRepository.findById(householdId)).thenReturn(household)

            val response = handler.handle(userId, householdId)

            assertEquals(householdCreatorId, response.createdBy)
            assertEquals(householdId, response.id)
            assertEquals("Test Household", response.name)
            assertEquals("Test Description", response.description)
        }

    @Test
    fun `handle should work with household that has empty description`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val householdId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()

            val household =
                TestUtils.getHousehold(
                    id = householdId,
                    name = "Test Household",
                    description = "", // Empty description
                    createdBy = userId,
                )

            whenever(householdRepository.findById(householdId)).thenReturn(household)

            val response = handler.handle(userId, householdId)

            assertEquals("", response.description)
        }

    @Test
    fun `handle should work with household that has long description`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val householdId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()
            val longDescription = "A".repeat(1000) // Long description

            val household =
                TestUtils.getHousehold(
                    id = householdId,
                    name = "Test Household",
                    description = longDescription,
                    createdBy = userId,
                )

            whenever(householdRepository.findById(householdId)).thenReturn(household)

            val response = handler.handle(userId, householdId)

            assertEquals(longDescription, response.description)
        }
}
