package com.ohana.domain.household

import com.ohana.TestUtils
import com.ohana.data.household.HouseholdRepository
import com.ohana.data.unitOfWork.*
import com.ohana.shared.enums.HouseholdMemberRole
import jakarta.validation.Validation
import jakarta.validation.Validator
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HouseholdCreationHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var householdRepository: HouseholdRepository
    private lateinit var handler: HouseholdCreationHandler
    private lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        householdRepository = mock()
        context =
            mock {
                on { households } doReturn householdRepository
            }
        unitOfWork = mock()
        handler = HouseholdCreationHandler(unitOfWork)
        validator = Validation.buildDefaultValidatorFactory().validator
    }

    @Test
    fun `handle should create household successfully`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val request =
                HouseholdCreationHandler.Request(
                    id = householdId,
                    name = "Test Household",
                    description = "A test household for testing purposes",
                )

            val expectedHousehold =
                TestUtils.getHousehold(
                    id = householdId,
                    name = request.name,
                    description = request.description,
                    createdBy = userId,
                )

            val expectedMember =
                TestUtils.getHouseholdMember(
                    id = UUID.randomUUID().toString(),
                    householdId = householdId,
                    memberId = userId,
                    role = HouseholdMemberRole.admin,
                )

            whenever(householdRepository.create(any())).thenReturn(expectedHousehold)
            whenever(householdRepository.createMember(any())).thenReturn(expectedMember)

            val response = handler.handle(userId, request)

            assertEquals(householdId, response.id)
            assertEquals(request.name, response.name)
            assertEquals(request.description, response.description)

            verify(householdRepository).create(
                argThat { household ->
                    household.id == householdId &&
                        household.name == request.name &&
                        household.description == request.description &&
                        household.createdBy == userId
                },
            )

            verify(householdRepository).createMember(
                argThat { member ->
                    member.householdId == householdId &&
                        member.memberId == userId &&
                        member.role == HouseholdMemberRole.admin &&
                        member.isActive &&
                        member.invitedBy == userId &&
                        member.joinedAt != null
                },
            )
        }

    @Test
    fun `handle should create household with minimal valid data`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val request =
                HouseholdCreationHandler.Request(
                    id = householdId,
                    name = "A", // Minimum length
                    description = "Minimal description",
                )

            val expectedHousehold =
                TestUtils.getHousehold(
                    id = householdId,
                    name = request.name,
                    description = request.description,
                    createdBy = userId,
                )

            val expectedMember =
                TestUtils.getHouseholdMember(
                    id = UUID.randomUUID().toString(),
                    householdId = householdId,
                    memberId = userId,
                    role = HouseholdMemberRole.admin,
                )

            whenever(householdRepository.create(any())).thenReturn(expectedHousehold)
            whenever(householdRepository.createMember(any())).thenReturn(expectedMember)

            val response = handler.handle(userId, request)

            assertEquals(householdId, response.id)
            assertEquals(request.name, response.name)
            assertEquals(request.description, response.description)
        }

    @Test
    fun `handle should create household with maximum valid data`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val longName = "A".repeat(255) // Maximum length
            val longDescription = "A".repeat(1000) // Maximum length
            val request =
                HouseholdCreationHandler.Request(
                    id = householdId,
                    name = longName,
                    description = longDescription,
                )

            val expectedHousehold =
                TestUtils.getHousehold(
                    id = householdId,
                    name = request.name,
                    description = request.description,
                    createdBy = userId,
                )

            val expectedMember =
                TestUtils.getHouseholdMember(
                    id = UUID.randomUUID().toString(),
                    householdId = householdId,
                    memberId = userId,
                    role = HouseholdMemberRole.admin,
                )

            whenever(householdRepository.create(any())).thenReturn(expectedHousehold)
            whenever(householdRepository.createMember(any())).thenReturn(expectedMember)

            val response = handler.handle(userId, request)

            assertEquals(householdId, response.id)
            assertEquals(longName, response.name)
            assertEquals(longDescription, response.description)
        }

    @Test
    fun `handle should throw ValidationException when id is empty`() =
        runTest {
            val request =
                HouseholdCreationHandler.Request(
                    id = "",
                    name = "Test Household",
                    description = "Test description",
                )
            val violations = validator.validate(request)
            val messages = violations.map { it.propertyPath.toString() to it.message }
            assertTrue(messages.contains("id" to "Household ID is required"))
        }

    @Test
    fun `handle should throw ValidationException when id is not valid GUID`() =
        runTest {
            val request =
                HouseholdCreationHandler.Request(
                    id = "invalid-guid",
                    name = "Test Household",
                    description = "Test description",
                )
            val violations = validator.validate(request)
            val messages = violations.map { it.propertyPath.toString() to it.message }
            assertTrue(messages.contains("id" to "Household ID must be a valid GUID"))
        }

    @Test
    fun `handle should throw ValidationException when name is empty`() =
        runTest {
            val request =
                HouseholdCreationHandler.Request(
                    id = UUID.randomUUID().toString(),
                    name = "",
                    description = "Test description",
                )
            val violations = validator.validate(request)
            val messages = violations.map { it.propertyPath.toString() to it.message }
            assertTrue(messages.contains("name" to "Household name is required"))
        }

    @Test
    fun `handle should throw ValidationException when name is too long`() =
        runTest {
            val request =
                HouseholdCreationHandler.Request(
                    id = UUID.randomUUID().toString(),
                    name = "A".repeat(256), // 256 characters, exceeds 255 limit
                    description = "Test description",
                )
            val violations = validator.validate(request)
            val messages = violations.map { it.propertyPath.toString() to it.message }
            assertTrue(messages.contains("name" to "Household name must be between 1 and 255 characters long"))
        }

    @Test
    fun `handle should throw ValidationException when description is empty`() =
        runTest {
            val request =
                HouseholdCreationHandler.Request(
                    id = UUID.randomUUID().toString(),
                    name = "Test Household",
                    description = "",
                )
            val violations = validator.validate(request)
            val messages = violations.map { it.propertyPath.toString() to it.message }
            assertTrue(messages.contains("description" to "Household description is required"))
        }

    @Test
    fun `handle should throw ValidationException when description is too long`() =
        runTest {
            val request =
                HouseholdCreationHandler.Request(
                    id = UUID.randomUUID().toString(),
                    name = "Test Household",
                    description = "A".repeat(1001), // 1001 characters, exceeds 1000 limit
                )
            val violations = validator.validate(request)
            val messages = violations.map { it.propertyPath.toString() to it.message }
            assertTrue(messages.contains("description" to "Household description must be at most 1000 characters long"))
        }

    @Test
    fun `handle should throw ValidationException with multiple errors`() =
        runTest {
            val request =
                HouseholdCreationHandler.Request(
                    id = "",
                    name = "",
                    description = "",
                )
            val violations = validator.validate(request)
            val fields = violations.map { it.propertyPath.toString() }.toSet()
            assertTrue(fields.contains("id"))
            assertTrue(fields.contains("name"))
            assertTrue(fields.contains("description"))
        }

    @Test
    fun `handle should propagate exception from household creation`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val request =
                HouseholdCreationHandler.Request(
                    id = UUID.randomUUID().toString(),
                    name = "Test Household",
                    description = "Test description",
                )

            whenever(householdRepository.create(any())).thenThrow(RuntimeException("DB error"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(userId, request)
                }

            assertEquals("DB error", ex.message)
            verify(householdRepository).create(any())
            verify(householdRepository, never()).createMember(any())
        }

    @Test
    fun `handle should propagate exception from member creation`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val request =
                HouseholdCreationHandler.Request(
                    id = UUID.randomUUID().toString(),
                    name = "Test Household",
                    description = "Test description",
                )

            val expectedHousehold =
                TestUtils.getHousehold(
                    id = request.id,
                    name = request.name,
                    description = request.description,
                    createdBy = userId,
                )

            whenever(householdRepository.create(any())).thenReturn(expectedHousehold)
            whenever(householdRepository.createMember(any())).thenThrow(RuntimeException("Member creation failed"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(userId, request)
                }

            assertEquals("Member creation failed", ex.message)
            verify(householdRepository).create(any())
            verify(householdRepository).createMember(any())
        }

    @Test
    fun `handle should create member with correct admin role and active status`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val request =
                HouseholdCreationHandler.Request(
                    id = householdId,
                    name = "Test Household",
                    description = "Test description",
                )

            val expectedHousehold =
                TestUtils.getHousehold(
                    id = householdId,
                    name = request.name,
                    description = request.description,
                    createdBy = userId,
                )

            val expectedMember =
                TestUtils.getHouseholdMember(
                    id = UUID.randomUUID().toString(),
                    householdId = householdId,
                    memberId = userId,
                    role = HouseholdMemberRole.admin,
                )

            whenever(householdRepository.create(any())).thenReturn(expectedHousehold)
            whenever(householdRepository.createMember(any())).thenReturn(expectedMember)

            handler.handle(userId, request)

            verify(householdRepository).createMember(
                argThat { member ->
                    member.role == HouseholdMemberRole.admin &&
                        member.isActive &&
                        member.invitedBy == userId &&
                        member.joinedAt != null &&
                        member.householdId == householdId &&
                        member.memberId == userId
                },
            )
        }

    @Test
    fun `handle should generate unique member ID`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val request =
                HouseholdCreationHandler.Request(
                    id = householdId,
                    name = "Test Household",
                    description = "Test description",
                )

            val expectedHousehold =
                TestUtils.getHousehold(
                    id = request.id,
                    name = request.name,
                    description = request.description,
                    createdBy = userId,
                )

            val expectedMember =
                TestUtils.getHouseholdMember(
                    id = UUID.randomUUID().toString(),
                    householdId = householdId,
                    memberId = userId,
                    role = HouseholdMemberRole.admin,
                )

            whenever(householdRepository.create(any())).thenReturn(expectedHousehold)
            whenever(householdRepository.createMember(any())).thenReturn(expectedMember)

            handler.handle(userId, request)

            verify(householdRepository).createMember(
                argThat { member ->
                    member.id.isNotEmpty() && member.id != request.id && member.id != userId
                },
            )
        }

    @Test
    fun `handle should set joinedAt to current timestamp`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val request =
                HouseholdCreationHandler.Request(
                    id = householdId,
                    name = "Test Household",
                    description = "Test description",
                )

            val expectedHousehold =
                TestUtils.getHousehold(
                    id = request.id,
                    name = request.name,
                    description = request.description,
                    createdBy = userId,
                )

            val expectedMember =
                TestUtils.getHouseholdMember(
                    id = UUID.randomUUID().toString(),
                    householdId = householdId,
                    memberId = userId,
                    role = HouseholdMemberRole.admin,
                )

            whenever(householdRepository.create(any())).thenReturn(expectedHousehold)
            whenever(householdRepository.createMember(any())).thenReturn(expectedMember)

            val beforeCall = Instant.now()
            handler.handle(userId, request)
            val afterCall = Instant.now()

            verify(householdRepository).createMember(
                argThat { member ->
                    member.joinedAt != null &&
                        member.joinedAt!! >= beforeCall &&
                        member.joinedAt!! <= afterCall
                },
            )
        }
}
