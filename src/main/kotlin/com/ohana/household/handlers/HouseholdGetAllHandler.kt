package com.ohana.household.handlers

import com.ohana.utils.DatabaseUtils.Companion.get
import com.ohana.utils.DatabaseUtils.Companion.query
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi

class HouseholdGetAllHandler(
    private val jdbi: Jdbi,
) {
    data class Response(
        val id: String,
        val name: String,
        val description: String,
        val createdBy: String,
    )

    suspend fun handle(): List<Response> =
        query(jdbi) { handle ->
            getHouseholds(handle)
        }

    fun getHouseholds(handle: Handle): List<Response> =
        get(
            handle,
            "SELECT id, name, description, createdBy FROM households",
            mapOf(),
            Response::class,
        )
}
