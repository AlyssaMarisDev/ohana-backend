package com.ohana.data.permissions

interface TagPermissionRepository {
    fun findByPermissionId(permissionId: String): List<TagPermission>

    fun create(tagPermission: TagPermission): TagPermission
}
