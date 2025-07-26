package com.ohana.data.tags

interface TagPermissionRepository {
    fun findByPermissionId(permissionId: String): List<TagPermission>
}
