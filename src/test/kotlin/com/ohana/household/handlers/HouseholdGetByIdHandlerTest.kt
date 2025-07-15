package com.ohana.household.handlers

import com.ohana.TestUtils
import com.ohana.exceptions.NotFoundException
import com.ohana.shared.HouseholdRepository
import com.ohana.shared.UnitOfWork
import com.ohana.shared.UnitOfWorkContext
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
    private lateinit var handler: HouseholdGetByIdHandler

    @BeforeEach
    fun setUp() {
        householdRepository = mock()
        context =
            mock {
                on { households } doReturn householdRepository
            }
        unitOfWork = mock()
        handler = HouseholdGetByIdHandler(unitOfWork)
    }

    @Test
    fun `handle should return household when found`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val household =
                TestUtils.getHousehold(
                    id = UUID.randomUUID().toString(),
                    name = "Test Household",
                    description = "Test household description",
                    createdBy = UUID.randomUUID().toString(),
                )

            whenever(householdRepository.findById(household.id)).thenReturn(household)

            val response = handler.handle(household.id)

            assertEquals(household.id, response.id)
            assertEquals(household.name, response.name)
            assertEquals(household.description, response.description)
            assertEquals(household.createdBy, response.createdBy)

            verify(householdRepository).findById(household.id)
            verifyNoMoreInteractions(householdRepository)
        }

    @Test
    fun `handle should throw NotFoundException when household does not exist`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val id = UUID.randomUUID().toString()
            whenever(householdRepository.findById(id)).thenReturn(null)

            val ex =
                assertThrows<NotFoundException> {
                    handler.handle(id)
                }
            assertEquals("Household not found", ex.message)
            verify(householdRepository).findById(id)
            verifyNoMoreInteractions(householdRepository)
        }

    @Test
    fun `handle should propagate exception from repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val id = UUID.randomUUID().toString()
            whenever(householdRepository.findById(id)).thenThrow(RuntimeException("DB error"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(id)
                }
            assertEquals("DB error", ex.message)
            verify(householdRepository).findById(id)
            verifyNoMoreInteractions(householdRepository)
        }
}
