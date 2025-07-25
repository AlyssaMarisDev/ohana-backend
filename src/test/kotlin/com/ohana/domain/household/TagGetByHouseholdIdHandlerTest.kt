package com.ohana.domain.household

import com.ohana.TestUtils
import com.ohana.data.household.HouseholdRepository
import com.ohana.data.household.TagRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.validators.HouseholdMemberValidator
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.UUID
import kotlin.test.assertEquals

class TagGetByHouseholdIdHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var householdRepository: HouseholdRepository
    private lateinit var tagRepository: TagRepository
    private lateinit var validator: HouseholdMemberValidator
    private lateinit var handler: TagGetByHouseholdIdHandler

    @BeforeEach
    fun setUp() {
        householdRepository = mock()
        tagRepository = mock()
        validator = mock()
        context =
            mock {
                on { households } doReturn householdRepository
                on { tags } doReturn tagRepository
            }
        unitOfWork = mock()
        handler = TagGetByHouseholdIdHandler(unitOfWork, validator)
    }

    @Test
    fun `handle should return tags for household when user is authorized`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            val expectedTags =
                listOf(
                    TestUtils.getTag(
                        id = UUID.randomUUID().toString(),
                        name = "metas",
                        color = "#3B82F6",
                        householdId = householdId,
                    ),
                    TestUtils.getTag(
                        id = UUID.randomUUID().toString(),
                        name = "adult",
                        color = "#EF4444",
                        householdId = householdId,
                    ),
                )

            whenever(tagRepository.findByHouseholdId(householdId)).thenReturn(expectedTags)

            val response = handler.handle(userId, householdId)

            assertEquals(2, response.tags.size)
            assertEquals("metas", response.tags[0].name)
            assertEquals("#3B82F6", response.tags[0].color)
            assertEquals("adult", response.tags[1].name)
            assertEquals("#EF4444", response.tags[1].color)

            verify(validator).validate(context, householdId, userId)
            verify(tagRepository).findByHouseholdId(householdId)
        }

    @Test
    fun `handle should return empty list when no tags exist`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            whenever(tagRepository.findByHouseholdId(householdId)).thenReturn(emptyList())

            val response = handler.handle(userId, householdId)

            assertEquals(0, response.tags.size)

            verify(validator).validate(context, householdId, userId)
            verify(tagRepository).findByHouseholdId(householdId)
        }

    @Test
    fun `handle should propagate validation exception`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            whenever(validator.validate(context, householdId, userId))
                .thenThrow(RuntimeException("Validation failed"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(userId, householdId)
                }

            assertEquals("Validation failed", ex.message)
            verify(validator).validate(context, householdId, userId)
            verify(tagRepository, never()).findByHouseholdId(any())
        }

    @Test
    fun `handle should propagate repository exception`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            whenever(tagRepository.findByHouseholdId(householdId))
                .thenThrow(RuntimeException("Database error"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(userId, householdId)
                }

            assertEquals("Database error", ex.message)
            verify(validator).validate(context, householdId, userId)
            verify(tagRepository).findByHouseholdId(householdId)
        }
}
