package com.ohana.domain.member

import com.ohana.data.unitOfWork.*
import com.ohana.shared.exceptions.AuthorizationException
import com.ohana.shared.exceptions.NotFoundException

class MemberUpdateByIdHandler(
    private val unitOfWork: UnitOfWork,
) {
    data class Request(
        val name: String,
        val age: Int?,
        val gender: String?,
    )

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
        request: Request,
    ): Response =
        unitOfWork.execute { context ->
            // Check that the user is trying to update their own member information
            if (id != userId) {
                throw AuthorizationException("You can only update your own member information")
            }

            // Get existing member
            val existingMember = context.members.findById(id) ?: throw NotFoundException("Member not found")

            // Update member
            val updatedMember =
                context.members.update(
                    existingMember.copy(
                        name = request.name,
                        age = request.age,
                        gender = request.gender,
                    ),
                )

            // Return response
            Response(
                id = updatedMember.id,
                name = updatedMember.name,
                age = updatedMember.age,
                gender = updatedMember.gender,
                email = updatedMember.email,
            )
        }
}
