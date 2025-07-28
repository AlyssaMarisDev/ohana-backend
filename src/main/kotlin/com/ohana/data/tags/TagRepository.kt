package com.ohana.data.tags

interface TagRepository {
    fun findById(id: String): Tag?

    fun findByIds(ids: List<String>): List<Tag>

    fun findByHouseholdId(householdId: String): List<Tag>

    fun create(tag: Tag): Tag

    fun update(tag: Tag): Tag

    fun deleteById(id: String): Boolean
}
