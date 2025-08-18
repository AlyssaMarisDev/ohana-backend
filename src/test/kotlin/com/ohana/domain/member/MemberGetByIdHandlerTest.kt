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

class MemberGetByIdHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var memberRepository: MemberRepository
    private lateinit var handler: MemberGetByIdHandler

    @BeforeEach
    fun setUp() {
        memberRepository = mock()
        context = mock { on { members } doReturn memberRepository }
        unitOfWork = mock()
        handler = MemberGetByIdHandler(unitOfWork)
    }

    @Test
    fun `handle should return own member`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val userId = UUID.randomUUID().toString()
            val m = TestUtils.getMember(id = userId)
            whenever(memberRepository.findById(userId)).thenReturn(m)

            val res = handler.handle(userId, userId)
            assertEquals(userId, res.id)
        }

    @Test
    fun `handle should forbid accessing other member`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val userId = UUID.randomUUID().toString()
            val otherId = UUID.randomUUID().toString()

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(userId, otherId)
                }
            assertEquals("You can only access your own member information", ex.message)
        }

    @Test
    fun `handle should throw NotFound when missing`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val userId = UUID.randomUUID().toString()
            whenever(memberRepository.findById(userId)).thenReturn(null)

            val ex =
                assertThrows<NotFoundException> {
                    handler.handle(userId, userId)
                }
            assertEquals("Member not found", ex.message)
        }
}
