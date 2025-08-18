package com.ohana.domain.tags

import com.ohana.TestUtils
import com.ohana.data.tags.TagRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.validators.HouseholdMemberValidator
import com.ohana.shared.exceptions.ConflictException
import com.ohana.shared.exceptions.NotFoundException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.UUID

class TagUpdateHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var tagRepository: TagRepository
    private lateinit var validator: HouseholdMemberValidator
    private lateinit var handler: TagUpdateHandler

    @BeforeEach
    fun setUp() {
        tagRepository = mock()
        validator = mock()
        context = mock { on { tags } doReturn tagRepository }
        unitOfWork = mock()
        handler = TagUpdateHandler(unitOfWork, validator)
    }

    @Test
    fun `handle should update tag`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val tag = TestUtils.getTag()
            val tagId = tag.id
            whenever(validator.validate(context, householdId, userId)).thenReturn(UUID.randomUUID().toString())
            whenever(tagRepository.findByHouseholdId(householdId)).thenReturn(listOf(tag))
            whenever(tagRepository.update(any())).thenAnswer { it.getArgument(0) }

            val req = TagUpdateHandler.Request(name = "New", color = "#ABCDEF")
            val res = handler.handle(userId, householdId, tagId, req)
            assertEquals("New", res.name)
            assertEquals("#ABCDEF", res.color)
        }

    @Test
    fun `handle should throw NotFound when tag not in household`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val tagId = UUID.randomUUID().toString()
            whenever(validator.validate(context, householdId, userId)).thenReturn(UUID.randomUUID().toString())
            whenever(tagRepository.findByHouseholdId(householdId)).thenReturn(emptyList())

            val ex =
                assertThrows<NotFoundException> {
                    handler.handle(userId, householdId, tagId, TagUpdateHandler.Request("n", "c"))
                }
            assertEquals("Tag not found in this household", ex.message)
        }

    @Test
    fun `handle should throw Conflict when name exists`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val existing1 = TestUtils.getTag(name = "A")
            val existing2 = TestUtils.getTag(name = "B")
            whenever(validator.validate(context, householdId, userId)).thenReturn(UUID.randomUUID().toString())
            whenever(tagRepository.findByHouseholdId(householdId)).thenReturn(listOf(existing1, existing2))

            val ex =
                assertThrows<ConflictException> {
                    handler.handle(userId, householdId, existing1.id, TagUpdateHandler.Request("B", "#111111"))
                }
            assertEquals("A tag with the name 'B' already exists in this household", ex.message)
        }
}
