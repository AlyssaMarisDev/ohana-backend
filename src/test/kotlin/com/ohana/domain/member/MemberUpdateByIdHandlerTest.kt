package com.ohana.domain.member

import com.ohana.TestUtils
import com.ohana.data.member.MemberRepository
import com.ohana.data.unitOfWork.*
import com.ohana.shared.exceptions.AuthorizationException
import com.ohana.shared.exceptions.NotFoundException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.UUID

class MemberUpdateByIdHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var memberRepository: MemberRepository
    private lateinit var handler: MemberUpdateByIdHandler

    @BeforeEach
    fun setUp() {
        memberRepository = mock()
        context = mock { on { members } doReturn memberRepository }
        unitOfWork = mock()
        handler = MemberUpdateByIdHandler(unitOfWork)
    }

    @Test
    fun `handle should update own member`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val userId = UUID.randomUUID().toString()
            val existing = TestUtils.getMember(id = userId)
            val updated = existing.copy(name = "New Name", age = 30, gender = "female")
            whenever(memberRepository.findById(userId)).thenReturn(existing)
            whenever(memberRepository.update(any())).thenReturn(updated)

            val res = handler.handle(userId, userId, MemberUpdateByIdHandler.Request(name = updated.name, age = updated.age, gender = updated.gender))
            assertEquals(updated.name, res.name)
            assertEquals(updated.age, res.age)
            assertEquals(updated.gender, res.gender)
        }

    @Test
    fun `handle should forbid updating other member`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val userId = UUID.randomUUID().toString()
            val otherId = UUID.randomUUID().toString()

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(userId, otherId, MemberUpdateByIdHandler.Request("x", null, null))
                }
            assertEquals("You can only update your own member information", ex.message)
        }

    @Test
    fun `handle should throw NotFound when member missing`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val userId = UUID.randomUUID().toString()
            whenever(memberRepository.findById(userId)).thenReturn(null)

            val ex =
                assertThrows<NotFoundException> {
                    handler.handle(userId, userId, MemberUpdateByIdHandler.Request("x", null, null))
                }
            assertEquals("Member not found", ex.message)
        }
}


