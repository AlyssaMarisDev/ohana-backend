package com.ohana.domain.member

import com.ohana.data.unitOfWork.*
import com.ohana.shared.exceptions.AuthorizationException
import com.ohana.shared.exceptions.NotFoundException

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

    suspend fun handle(
        userId: String,
        id: String,
    ): Response =
        unitOfWork.execute { context ->
            // Check that the user is trying to access their own member information
            if (id != userId) {
                throw AuthorizationException("You can only access your own member information")
            }

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
