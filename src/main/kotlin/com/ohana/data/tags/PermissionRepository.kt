package com.ohana.data.tags

interface PermissionRepository {
    fun findByHouseholdMemberId(householdMemberId: String): Permission?
}
