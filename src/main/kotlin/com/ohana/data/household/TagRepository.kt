package com.ohana.data.household

interface TagRepository {
    fun findById(id: String): Tag?

    fun findByHouseholdId(householdId: String): List<Tag>

    fun create(tag: Tag): Tag

    fun update(tag: Tag): Tag

    fun deleteById(id: String): Boolean
}
