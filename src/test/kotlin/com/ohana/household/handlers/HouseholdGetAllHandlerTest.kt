package com.ohana.household.handlers

import com.ohana.TestUtils
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
import kotlin.test.assertTrue

class HouseholdGetAllHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var householdRepository: HouseholdRepository
    private lateinit var handler: HouseholdGetAllHandler

    @BeforeEach
    fun setUp() {
        householdRepository = mock()
        context =
            mock {
                on { households } doReturn householdRepository
            }
        unitOfWork = mock()
        handler = HouseholdGetAllHandler(unitOfWork)
    }

    @Test
    fun `handle should return empty list when no households exist`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            whenever(householdRepository.findByMemberId(userId)).thenReturn(emptyList())

            val response = handler.handle(userId)

            assertTrue(response.isEmpty())
            verify(householdRepository).findByMemberId(userId)
            verifyNoMoreInteractions(householdRepository)
        }

    @Test
    fun `handle should return single household when one exists`() =
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

            whenever(householdRepository.findByMemberId(userId)).thenReturn(listOf(household))

            val response = handler.handle(userId)

            assertEquals(1, response.size)
            val responseItem = response.first()
            assertEquals(household.id, responseItem.id)
            assertEquals(household.name, responseItem.name)
            assertEquals(household.description, responseItem.description)
            assertEquals(household.createdBy, responseItem.createdBy)

            verify(householdRepository).findByMemberId(userId)
            verifyNoMoreInteractions(householdRepository)
        }

    @Test
    fun `handle should return multiple households when multiple exist`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val household1 =
                TestUtils.getHousehold(
                    id = UUID.randomUUID().toString(),
                    name = "First Household",
                    description = "First household description",
                    createdBy = UUID.randomUUID().toString(),
                )

            val household2 =
                TestUtils.getHousehold(
                    id = UUID.randomUUID().toString(),
                    name = "Second Household",
                    description = "Second household description",
                    createdBy = UUID.randomUUID().toString(),
                )

            val household3 =
                TestUtils.getHousehold(
                    id = UUID.randomUUID().toString(),
                    name = "Third Household",
                    description = "Third household description",
                    createdBy = UUID.randomUUID().toString(),
                )

            whenever(householdRepository.findByMemberId(userId)).thenReturn(listOf(household1, household2, household3))

            val response = handler.handle(userId)

            assertEquals(3, response.size)

            // Verify first household
            val response1 = response[0]
            assertEquals(household1.id, response1.id)
            assertEquals(household1.name, response1.name)
            assertEquals(household1.description, response1.description)
            assertEquals(household1.createdBy, response1.createdBy)

            // Verify second household
            val response2 = response[1]
            assertEquals(household2.id, response2.id)
            assertEquals(household2.name, response2.name)
            assertEquals(household2.description, response2.description)
            assertEquals(household2.createdBy, response2.createdBy)

            // Verify third household
            val response3 = response[2]
            assertEquals(household3.id, response3.id)
            assertEquals(household3.name, response3.name)
            assertEquals(household3.description, response3.description)
            assertEquals(household3.createdBy, response3.createdBy)

            verify(householdRepository).findByMemberId(userId)
            verifyNoMoreInteractions(householdRepository)
        }

    @Test
    fun `handle should return households with minimal data`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val household =
                TestUtils.getHousehold(
                    id = UUID.randomUUID().toString(),
                    name = "A", // Minimum length name
                    description = "Minimal description",
                    createdBy = UUID.randomUUID().toString(),
                )

            whenever(householdRepository.findByMemberId(userId)).thenReturn(listOf(household))

            val response = handler.handle(userId)

            assertEquals(1, response.size)
            val responseItem = response.first()
            assertEquals(household.id, responseItem.id)
            assertEquals(household.name, responseItem.name)
            assertEquals(household.description, responseItem.description)
            assertEquals(household.createdBy, responseItem.createdBy)

            verify(householdRepository).findByMemberId(userId)
            verifyNoMoreInteractions(householdRepository)
        }

    @Test
    fun `handle should return households with maximum length data`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val longName = "A".repeat(255) // Maximum length name
            val longDescription = "A".repeat(1000) // Maximum length description

            val household =
                TestUtils.getHousehold(
                    id = UUID.randomUUID().toString(),
                    name = longName,
                    description = longDescription,
                    createdBy = UUID.randomUUID().toString(),
                )

            whenever(householdRepository.findByMemberId(userId)).thenReturn(listOf(household))

            val response = handler.handle(userId)

            assertEquals(1, response.size)
            val responseItem = response.first()
            assertEquals(household.id, responseItem.id)
            assertEquals(longName, responseItem.name)
            assertEquals(longDescription, responseItem.description)
            assertEquals(household.createdBy, responseItem.createdBy)

            verify(householdRepository).findByMemberId(userId)
            verifyNoMoreInteractions(householdRepository)
        }

    @Test
    fun `handle should return households with special characters in name and description`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val household =
                TestUtils.getHousehold(
                    id = UUID.randomUUID().toString(),
                    name = "Household with special chars: !@#$%^&*()",
                    description = "Description with special chars: !@#$%^&*() and unicode: 🏠👨‍👩‍👧‍👦",
                    createdBy = UUID.randomUUID().toString(),
                )

            whenever(householdRepository.findByMemberId(userId)).thenReturn(listOf(household))

            val response = handler.handle(userId)

            assertEquals(1, response.size)
            val responseItem = response.first()
            assertEquals(household.id, responseItem.id)
            assertEquals(household.name, responseItem.name)
            assertEquals(household.description, responseItem.description)
            assertEquals(household.createdBy, responseItem.createdBy)

            verify(householdRepository).findByMemberId(userId)
            verifyNoMoreInteractions(householdRepository)
        }

    @Test
    fun `handle should return households with empty description`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val household =
                TestUtils.getHousehold(
                    id = UUID.randomUUID().toString(),
                    name = "Household with empty description",
                    description = "",
                    createdBy = UUID.randomUUID().toString(),
                )

            whenever(householdRepository.findByMemberId(userId)).thenReturn(listOf(household))

            val response = handler.handle(userId)

            assertEquals(1, response.size)
            val responseItem = response.first()
            assertEquals(household.id, responseItem.id)
            assertEquals(household.name, responseItem.name)
            assertEquals("", responseItem.description)
            assertEquals(household.createdBy, responseItem.createdBy)

            verify(householdRepository).findByMemberId(userId)
            verifyNoMoreInteractions(householdRepository)
        }

    @Test
    fun `handle should propagate exception from repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            whenever(householdRepository.findByMemberId(userId)).thenThrow(RuntimeException("Database connection failed"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(userId)
                }

            assertEquals("Database connection failed", ex.message)
            verify(householdRepository).findByMemberId(userId)
            verifyNoMoreInteractions(householdRepository)
        }

    @Test
    fun `handle should propagate database exception from repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            whenever(householdRepository.findByMemberId(userId)).thenThrow(RuntimeException("SQL syntax error"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(userId)
                }

            assertEquals("SQL syntax error", ex.message)
            verify(householdRepository).findByMemberId(userId)
            verifyNoMoreInteractions(householdRepository)
        }

    @Test
    fun `handle should maintain order of households from repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val household1 =
                TestUtils.getHousehold(
                    id = "1",
                    name = "First",
                    description = "First household",
                    createdBy = "user1",
                )

            val household2 =
                TestUtils.getHousehold(
                    id = "2",
                    name = "Second",
                    description = "Second household",
                    createdBy = "user2",
                )

            val household3 =
                TestUtils.getHousehold(
                    id = "3",
                    name = "Third",
                    description = "Third household",
                    createdBy = "user3",
                )

            val households = listOf(household1, household2, household3)
            whenever(householdRepository.findByMemberId(userId)).thenReturn(households)

            val response = handler.handle(userId)

            assertEquals(3, response.size)
            assertEquals("1", response[0].id)
            assertEquals("2", response[1].id)
            assertEquals("3", response[2].id)

            verify(householdRepository).findByMemberId(userId)
            verifyNoMoreInteractions(householdRepository)
        }

    @Test
    fun `handle should return correct response structure`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val household =
                TestUtils.getHousehold(
                    id = UUID.randomUUID().toString(),
                    name = "Test Household",
                    description = "Test Description",
                    createdBy = UUID.randomUUID().toString(),
                )

            whenever(householdRepository.findByMemberId(userId)).thenReturn(listOf(household))

            val response = handler.handle(userId)

            assertEquals(1, response.size)
            val responseItem = response.first()

            // Verify property values
            assertEquals(household.id, responseItem.id)
            assertEquals(household.name, responseItem.name)
            assertEquals(household.description, responseItem.description)
            assertEquals(household.createdBy, responseItem.createdBy)

            verify(householdRepository).findByMemberId(userId)
            verifyNoMoreInteractions(householdRepository)
        }
}
