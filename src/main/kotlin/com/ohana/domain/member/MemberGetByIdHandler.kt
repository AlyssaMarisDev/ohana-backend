package com.ohana.domain.member

import com.ohana.data.unitOfWork.*
import com.ohana.exceptions.NotFoundException

class MemberGetByIdHandler(
    private val unitOfWork: UnitOfWork,
) {
    data class Response(
        val id: String,
        val name: String,
        val age: Int?,
        val gender: String?,
        val email: String,
    )

    suspend fun handle(id: String): Response =
        unitOfWork.execute { context ->
            val member = context.members.findById(id) ?: throw NotFoundException("Member not found")

            Response(
                id = member.id,
                name = member.name,
                age = member.age,
                gender = member.gender,
                email = member.email,
            )
        }
}
