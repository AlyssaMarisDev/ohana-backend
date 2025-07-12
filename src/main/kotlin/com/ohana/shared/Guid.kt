package com.ohana.shared

import java.util.UUID

object Guid {
    fun isValid(guid: String): Boolean =
        try {
            UUID.fromString(guid)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
}
