package com.ohana.household.handlers

import com.ohana.exceptions.NotFoundException
import com.ohana.utils.DatabaseUtils.Companion.get
import com.ohana.utils.DatabaseUtils.Companion.query
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi

class HouseholdGetByIdHandler(
    private val jdbi: Jdbi,
) {
    data class Response(
        val id: String,
        val name: String,
        val description: String,
        val createdBy: String,
    )

    suspend fun handle(id: String): Response =
        query(jdbi) { handle ->
            getHouseholdById(handle, id) ?: throw NotFoundException("Household not found")
        }

    fun getHouseholdById(
        handle: Handle,
        id: String,
    ): Response? =
        get(
            handle,
            "SELECT id, name, description, createdBy FROM households WHERE id = :id",
            mapOf("id" to id),
            Response::class,
        ).firstOrNull()
}
