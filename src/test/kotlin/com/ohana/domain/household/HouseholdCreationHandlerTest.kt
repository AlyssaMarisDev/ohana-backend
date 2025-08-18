package com.ohana.domain.household

import com.ohana.TestUtils
import com.ohana.data.household.HouseholdRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.permissions.TagPermissionManager
import com.ohana.domain.tags.DefaultTagService
import com.ohana.shared.enums.HouseholdMemberRole
import com.ohana.shared.enums.HouseholdMemberStatus
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.Instant
import java.util.UUID

class HouseholdCreationHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var householdRepository: HouseholdRepository
    private lateinit var tagPermissionManager: TagPermissionManager
    private lateinit var defaultTagService: DefaultTagService
    private lateinit var handler: HouseholdCreationHandler

    @BeforeEach
    fun setUp() {
        householdRepository = mock()
        tagPermissionManager = mock()
        defaultTagService = mock()
        context =
            mock {
                on { households } doReturn householdRepository
            }
        unitOfWork = mock()
        handler = HouseholdCreationHandler(unitOfWork, tagPermissionManager, defaultTagService)
    }

    @Test
    fun `handle should create household and return response when valid request provided`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val name = "Test Household"
            val description = "Test Description"

            val household =
                TestUtils.getHousehold(
                    id = householdId,
                    name = name,
                    description = description,
                    createdBy = userId,
                )

            val defaultTags =
                listOf(
                    TestUtils.getTag(id = UUID.randomUUID().toString(), householdId = householdId),
                    TestUtils.getTag(id = UUID.randomUUID().toString(), householdId = householdId),
                )

            whenever(householdRepository.create(any())).thenReturn(household)
            whenever(householdRepository.createMember(any())).thenAnswer { it.getArgument(0) }
            whenever(defaultTagService.createDefaultTags(context, householdId)).thenReturn(defaultTags)
            whenever(tagPermissionManager.createPermissionsWithTags(eq(context), any(), any())).thenReturn(TestUtils.getPermission())

            val request =
                HouseholdCreationHandler.Request(
                    id = householdId,
                    name = name,
                    description = description,
                )

            val response = handler.handle(userId, request)

            assertEquals(householdId, response.id)
            assertEquals(name, response.name)
            assertEquals(description, response.description)

            verify(householdRepository).create(
                argThat { createdHousehold ->
                    createdHousehold.id == householdId &&
                        createdHousehold.name == name &&
                        createdHousehold.description == description &&
                        createdHousehold.createdBy == userId
                },
            )
            verify(householdRepository).createMember(
                argThat { member ->
                    member.householdId == householdId &&
                        member.memberId == userId &&
                        member.role == HouseholdMemberRole.ADMIN &&
                        member.status == HouseholdMemberStatus.ACTIVE &&
                        member.isDefault == true &&
                        member.invitedBy == userId &&
                        member.joinedAt != null
                },
            )
            verify(defaultTagService).createDefaultTags(context, householdId)
            verify(tagPermissionManager).createPermissionsWithTags(eq(context), any(), eq(defaultTags.map { it.id }))
            verifyNoMoreInteractions(householdRepository, defaultTagService, tagPermissionManager)
        }

    @Test
    fun `handle should work with empty description`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val name = "Test Household"
            val description = ""

            val household =
                TestUtils.getHousehold(
                    id = householdId,
                    name = name,
                    description = description,
                    createdBy = userId,
                )

            val defaultTags = emptyList<com.ohana.data.tags.Tag>()

            whenever(householdRepository.create(any())).thenReturn(household)
            whenever(householdRepository.createMember(any())).thenAnswer { it.getArgument(0) }
            whenever(defaultTagService.createDefaultTags(context, householdId)).thenReturn(defaultTags)
            whenever(tagPermissionManager.createPermissionsWithTags(eq(context), any(), any())).thenReturn(TestUtils.getPermission())

            val request =
                HouseholdCreationHandler.Request(
                    id = householdId,
                    name = name,
                    description = description,
                )

            val response = handler.handle(userId, request)

            assertEquals(description, response.description)
        }

    @Test
    fun `handle should work with long description`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val name = "Test Household"
            val description = "A".repeat(1000) // Long description

            val household =
                TestUtils.getHousehold(
                    id = householdId,
                    name = name,
                    description = description,
                    createdBy = userId,
                )

            val defaultTags = emptyList<com.ohana.data.tags.Tag>()

            whenever(householdRepository.create(any())).thenReturn(household)
            whenever(householdRepository.createMember(any())).thenAnswer { it.getArgument(0) }
            whenever(defaultTagService.createDefaultTags(context, householdId)).thenReturn(defaultTags)
            whenever(tagPermissionManager.createPermissionsWithTags(eq(context), any(), any())).thenReturn(TestUtils.getPermission())

            val request =
                HouseholdCreationHandler.Request(
                    id = householdId,
                    name = name,
                    description = description,
                )

            val response = handler.handle(userId, request)

            assertEquals(description, response.description)
        }

    @Test
    fun `handle should work with special characters in name and description`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val name = "O'Connor-Smith Household"
            val description = "A household with special characters: !@#\$%^&*()"

            val household =
                TestUtils.getHousehold(
                    id = householdId,
                    name = name,
                    description = description,
                    createdBy = userId,
                )

            val defaultTags = emptyList<com.ohana.data.tags.Tag>()

            whenever(householdRepository.create(any())).thenReturn(household)
            whenever(householdRepository.createMember(any())).thenAnswer { it.getArgument(0) }
            whenever(defaultTagService.createDefaultTags(context, householdId)).thenReturn(defaultTags)
            whenever(tagPermissionManager.createPermissionsWithTags(eq(context), any(), any())).thenReturn(TestUtils.getPermission())

            val request =
                HouseholdCreationHandler.Request(
                    id = householdId,
                    name = name,
                    description = description,
                )

            val response = handler.handle(userId, request)

            assertEquals(name, response.name)
            assertEquals(description, response.description)
        }

    @Test
    fun `handle should create household member with correct properties`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val name = "Test Household"
            val description = "Test Description"

            val household =
                TestUtils.getHousehold(
                    id = householdId,
                    name = name,
                    description = description,
                    createdBy = userId,
                )

            val defaultTags = emptyList<com.ohana.data.tags.Tag>()

            whenever(householdRepository.create(any())).thenReturn(household)
            whenever(householdRepository.createMember(any())).thenAnswer { it.getArgument(0) }
            whenever(defaultTagService.createDefaultTags(context, householdId)).thenReturn(defaultTags)
            whenever(tagPermissionManager.createPermissionsWithTags(eq(context), any(), any())).thenReturn(TestUtils.getPermission())

            val request =
                HouseholdCreationHandler.Request(
                    id = householdId,
                    name = name,
                    description = description,
                )

            handler.handle(userId, request)

            verify(householdRepository).createMember(
                argThat { member ->
                    member.householdId == householdId &&
                        member.memberId == userId &&
                        member.role == HouseholdMemberRole.ADMIN &&
                        member.status == HouseholdMemberStatus.ACTIVE &&
                        member.isDefault == true &&
                        member.invitedBy == userId &&
                        member.joinedAt != null &&
                        member.joinedAt.isAfter(Instant.now().minusSeconds(5)) // Should be recent
                },
            )
        }

    @Test
    fun `handle should create permissions for default tags`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val name = "Test Household"
            val description = "Test Description"

            val household =
                TestUtils.getHousehold(
                    id = householdId,
                    name = name,
                    description = description,
                    createdBy = userId,
                )

            val defaultTags =
                listOf(
                    TestUtils.getTag(id = UUID.randomUUID().toString(), householdId = householdId),
                    TestUtils.getTag(id = UUID.randomUUID().toString(), householdId = householdId),
                )

            whenever(householdRepository.create(any())).thenReturn(household)
            whenever(householdRepository.createMember(any())).thenAnswer { it.getArgument(0) }
            whenever(defaultTagService.createDefaultTags(context, householdId)).thenReturn(defaultTags)
            whenever(tagPermissionManager.createPermissionsWithTags(eq(context), any(), any())).thenReturn(TestUtils.getPermission())

            val request =
                HouseholdCreationHandler.Request(
                    id = householdId,
                    name = name,
                    description = description,
                )

            handler.handle(userId, request)

            verify(tagPermissionManager).createPermissionsWithTags(
                eq(context),
                any(),
                eq(defaultTags.map { it.id }),
            )
        }

    @Test
    fun `handle should propagate exception from household repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val name = "Test Household"
            val description = "Test Description"

            whenever(householdRepository.create(any())).thenThrow(RuntimeException("DB error"))

            val request =
                HouseholdCreationHandler.Request(
                    id = householdId,
                    name = name,
                    description = description,
                )

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(userId, request)
                }

            assertEquals("DB error", ex.message)
            verify(householdRepository).create(any())
            verify(householdRepository, never()).createMember(any())
            verify(defaultTagService, never()).createDefaultTags(any(), any())
            verify(tagPermissionManager, never()).createPermissionsWithTags(any(), any(), any())
        }

    @Test
    fun `handle should propagate exception from default tag service`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val name = "Test Household"
            val description = "Test Description"

            val household =
                TestUtils.getHousehold(
                    id = householdId,
                    name = name,
                    description = description,
                    createdBy = userId,
                )

            whenever(householdRepository.create(any())).thenReturn(household)
            whenever(householdRepository.createMember(any())).thenAnswer { it.getArgument(0) }
            whenever(defaultTagService.createDefaultTags(context, householdId))
                .thenThrow(RuntimeException("Tag creation error"))

            val request =
                HouseholdCreationHandler.Request(
                    id = householdId,
                    name = name,
                    description = description,
                )

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(userId, request)
                }

            assertEquals("Tag creation error", ex.message)
            verify(householdRepository).create(any())
            verify(householdRepository).createMember(any())
            verify(defaultTagService).createDefaultTags(context, householdId)
            verify(tagPermissionManager, never()).createPermissionsWithTags(any(), any(), any())
        }

    @Test
    fun `handle should propagate exception from tag permission manager`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val name = "Test Household"
            val description = "Test Description"

            val household =
                TestUtils.getHousehold(
                    id = householdId,
                    name = name,
                    description = description,
                    createdBy = userId,
                )

            val defaultTags =
                listOf(
                    TestUtils.getTag(id = UUID.randomUUID().toString(), householdId = householdId),
                )

            whenever(householdRepository.create(any())).thenReturn(household)
            whenever(householdRepository.createMember(any())).thenAnswer { it.getArgument(0) }
            whenever(defaultTagService.createDefaultTags(context, householdId)).thenReturn(defaultTags)
            whenever(tagPermissionManager.createPermissionsWithTags(eq(context), any(), any()))
                .thenThrow(RuntimeException("Permission creation error"))

            val request =
                HouseholdCreationHandler.Request(
                    id = householdId,
                    name = name,
                    description = description,
                )

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(userId, request)
                }

            assertEquals("Permission creation error", ex.message)
            verify(householdRepository).create(any())
            verify(householdRepository).createMember(any())
            verify(defaultTagService).createDefaultTags(context, householdId)
            verify(tagPermissionManager).createPermissionsWithTags(eq(context), any(), eq(defaultTags.map { it.id }))
        }
}
