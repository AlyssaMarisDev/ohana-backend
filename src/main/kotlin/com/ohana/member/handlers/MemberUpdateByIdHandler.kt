package com.ohana.members.handlers

import com.ohana.exceptions.NotFoundException
import com.ohana.shared.UnitOfWork

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
        id: String,
        request: Request,
    ): Response =
        unitOfWork.execute { context ->
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
