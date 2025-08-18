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
    private lateinit var validator: HouseholdMemberValidator
    private lateinit var handler: HouseholdGetByIdHandler

    @BeforeEach
    fun setUp() {
        householdRepository = mock()
        validator = mock()
        context = mock { on { households } doReturn householdRepository }
        unitOfWork = mock()
        handler = HouseholdGetByIdHandler(unitOfWork, validator)
    }

    @Test
    fun `handle should return household when user is member`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val household = TestUtils.getHousehold(id = householdId)

            whenever(validator.validate(context, householdId, userId)).thenReturn(UUID.randomUUID().toString())
            whenever(householdRepository.findById(householdId)).thenReturn(household)

            val res = handler.handle(userId, householdId)

            assertEquals(householdId, res.id)
            assertEquals(household.name, res.name)
        }

    @Test
    fun `handle should throw NotFound when household missing`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            whenever(validator.validate(context, householdId, userId)).thenReturn(UUID.randomUUID().toString())
            whenever(householdRepository.findById(householdId)).thenReturn(null)

            val ex =
                assertThrows<NotFoundException> {
                    handler.handle(userId, householdId)
                }
            assertEquals("Household not found", ex.message)
        }
}


