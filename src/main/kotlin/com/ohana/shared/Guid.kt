package com.ohana.shared

import java.util.UUID

object Guid {
    fun isValid(guid: String): Boolean {
        if (guid.isEmpty() || guid.length != 36) {
            return false
        }

        try {
            UUID.fromString(guid)
        } catch (e: IllegalArgumentException) {
            return false
        }

        return true
    }

    fun generate(): String = UUID.randomUUID().toString()
}
