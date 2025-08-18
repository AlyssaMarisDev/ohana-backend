package com.ohana.domain.tags

import com.ohana.TestUtils
import com.ohana.data.tags.TagRepository
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

class TagDeleteHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var tagRepository: TagRepository
    private lateinit var taskTagRepository: com.ohana.data.tags.TaskTagRepository
    private lateinit var validator: HouseholdMemberValidator
    private lateinit var handler: TagDeleteHandler

    @BeforeEach
    fun setUp() {
        tagRepository = mock()
        taskTagRepository = mock()
        validator = mock()
        context =
            mock {
                on { tags } doReturn tagRepository
                on { taskTags } doReturn taskTagRepository
            }
        unitOfWork = mock()
        handler = TagDeleteHandler(unitOfWork, validator)
    }

    @Test
    fun `handle should delete tag and cascading task tags`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val tag = TestUtils.getTag()
            whenever(validator.validate(context, householdId, userId)).thenReturn(UUID.randomUUID().toString())
            whenever(tagRepository.findById(tag.id)).thenReturn(tag)
            whenever(tagRepository.deleteById(tag.id)).thenReturn(true)

            val res = handler.handle(userId, householdId, tag.id)
            assertEquals(true, res.success)
            verify(taskTagRepository).deleteByTagId(tag.id)
            verify(tagRepository).deleteById(tag.id)
        }

    @Test
    fun `handle should throw NotFound when tag missing`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val tagId = UUID.randomUUID().toString()
            whenever(validator.validate(context, householdId, userId)).thenReturn(UUID.randomUUID().toString())
            whenever(tagRepository.findById(tagId)).thenReturn(null)

            val ex =
                assertThrows<NotFoundException> {
                    handler.handle(userId, householdId, tagId)
                }
            assertEquals("Tag not found in this household", ex.message)
        }
}
