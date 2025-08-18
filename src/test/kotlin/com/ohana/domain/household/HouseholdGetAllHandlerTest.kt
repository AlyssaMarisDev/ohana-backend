package com.ohana.domain.household

import com.ohana.TestUtils
import com.ohana.data.household.HouseholdRepository
import com.ohana.data.unitOfWork.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.UUID

class HouseholdGetAllHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var householdRepository: HouseholdRepository
    private lateinit var handler: HouseholdGetAllHandler

    @BeforeEach
    fun setUp() {
        householdRepository = mock()
        context = mock { on { households } doReturn householdRepository }
        unitOfWork = mock()
        handler = HouseholdGetAllHandler(unitOfWork)
    }

    @Test
    fun `handle should return households for member`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val h1 = TestUtils.getHousehold(createdBy = userId)
            val h2 = TestUtils.getHousehold(createdBy = userId)
            whenever(householdRepository.findByMemberId(userId)).thenReturn(listOf(h1, h2))

            val result = handler.handle(userId)

            assertEquals(2, result.size)
            assertEquals(h1.id, result[0].id)
            assertEquals(h2.id, result[1].id)
        }
}
