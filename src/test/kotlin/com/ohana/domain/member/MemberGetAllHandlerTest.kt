package com.ohana.domain.member

import com.ohana.TestUtils
import com.ohana.data.member.MemberRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.validators.HouseholdMemberValidator
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.UUID

class MemberGetAllHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var memberRepository: MemberRepository
    private lateinit var validator: HouseholdMemberValidator
    private lateinit var handler: MemberGetAllHandler

    @BeforeEach
    fun setUp() {
        memberRepository = mock()
        validator = mock()
        context = mock { on { members } doReturn memberRepository }
        unitOfWork = mock()
        handler = MemberGetAllHandler(unitOfWork, validator)
    }

    @Test
    fun `handle should return members for household`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            whenever(validator.validate(context, householdId, userId)).thenReturn(UUID.randomUUID().toString())
            val m1 = TestUtils.getMember()
            val m2 = TestUtils.getMember()
            whenever(memberRepository.findByHouseholdId(householdId)).thenReturn(listOf(m1, m2))

            val res = handler.handle(userId, householdId)
            assertEquals(2, res.size)
            assertEquals(m1.id, res[0].id)
            assertEquals(m2.email, res[1].email)
        }
}
