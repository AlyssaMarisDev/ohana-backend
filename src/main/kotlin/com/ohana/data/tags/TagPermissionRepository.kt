package com.ohana.data.tags

interface TagPermissionRepository {
    fun create(permission: TagPermission): TagPermission

    fun update(permission: TagPermission): TagPermission

    fun findById(id: String): TagPermission?

    fun findByHouseholdMemberId(householdMemberId: String): TagPermission?

    fun deleteById(id: String): Boolean

    fun deleteByHouseholdMemberId(householdMemberId: String): Boolean
}
