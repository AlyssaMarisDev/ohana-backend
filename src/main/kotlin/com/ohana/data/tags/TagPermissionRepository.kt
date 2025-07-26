package com.ohana.data.tags

interface TagPermissionRepository {
    fun findByHouseholdMemberId(householdMemberId: String): TagPermission?
}
