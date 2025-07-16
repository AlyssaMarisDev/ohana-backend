package com.ohana.data.auth

interface AuthMemberRepository {
    fun findByEmail(email: String): AuthMember?

    fun create(member: AuthMember): AuthMember
}
