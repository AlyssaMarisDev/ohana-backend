package com.ohana.domain.household

import com.ohana.TestUtils
import com.ohana.data.household.HouseholdRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.validators.HouseholdMemberValidator
import com.ohana.shared.exceptions.NotFoundException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.UUID
import kotlin.test.assertEquals

class HouseholdGetByIdHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var householdRepository: HouseholdRepository
    private lateinit var householdMemberValidator: HouseholdMemberValidator
    private lateinit var handler: HouseholdGetByIdHandler

    @BeforeEach
    fun setUp() {
        householdRepository = mock()
        context =
            mock {
                on { households } doReturn householdRepository
            }
        unitOfWork = mock()
        householdMemberValidator = mock()
        handler = HouseholdGetByIdHandler(unitOfWork, householdMemberValidator)
    }

    @Test
    fun `handle should return household when found`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val household =
                TestUtils.getHousehold(
                    id = UUID.randomUUID().toString(),
                    name = "Test Household",
                    description = "Test household description",
                    createdBy = UUID.randomUUID().toString(),
                )

            whenever(householdRepository.findById(household.id)).thenReturn(household)

            val response = handler.handle(household.id, userId)

            assertEquals(household.id, response.id)
            assertEquals(household.name, response.name)
            assertEquals(household.description, response.description)
            assertEquals(household.createdBy, response.createdBy)

            verify(householdMemberValidator).validate(context, household.id, userId)
            verify(householdRepository).findById(household.id)
            verifyNoMoreInteractions(householdRepository)
        }

    @Test
    fun `handle should propagate exception from validator`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val id = UUID.randomUUID().toString()
            whenever(householdMemberValidator.validate(context, id, userId)).thenThrow(NotFoundException("Household not found"))

            val ex =
                assertThrows<NotFoundException> {
                    handler.handle(id, userId)
                }
            assertEquals("Household not found", ex.message)
            verify(householdMemberValidator).validate(context, id, userId)
            verifyNoMoreInteractions(householdRepository)
        }

    @Test
    fun `handle should propagate exception from repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val id = UUID.randomUUID().toString()
            whenever(householdRepository.findById(id)).thenThrow(RuntimeException("DB error"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(id, userId)
                }
            assertEquals("DB error", ex.message)
            verify(householdRepository).findById(id)
            verifyNoMoreInteractions(householdRepository)
        }
}
