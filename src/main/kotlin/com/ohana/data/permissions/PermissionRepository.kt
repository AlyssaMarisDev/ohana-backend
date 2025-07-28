package com.ohana.data.permissions

interface PermissionRepository {
    fun findByHouseholdMemberId(householdMemberId: String): Permission?

    fun create(permission: Permission): Permission
}
