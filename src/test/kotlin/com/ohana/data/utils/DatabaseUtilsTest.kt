package com.ohana.data.utils

import com.ohana.shared.exceptions.ConflictException
import com.ohana.shared.exceptions.DbException
import com.ohana.shared.exceptions.NotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.SQLException
import java.sql.SQLIntegrityConstraintViolationException

class DatabaseUtilsTest {
    @Test
    fun `passes through KnownError without translation`() {
        val thrown =
            assertThrows<NotFoundException> {
                translateDbErrors {
                    throw NotFoundException("Resource missing")
                }
            }
        assertEquals("Resource missing", thrown.message)
    }

    @Test
    fun `translates SQLIntegrityConstraintViolationException to ConflictException`() {
        val root = SQLIntegrityConstraintViolationException("Duplicate entry for key 'uq_name'")

        val thrown =
            assertThrows<ConflictException> {
                translateDbErrors {
                    throw root
                }
            }
        assertTrue(thrown.message?.contains("Duplicate entry") == true)
        assertTrue(thrown.cause === root)
    }

    @Test
    fun `translates SQLState 23xxx to ConflictException`() {
        val sql = SQLException("Integrity violation", "23000", 9999)

        val thrown =
            assertThrows<ConflictException> {
                translateDbErrors {
                    throw sql
                }
            }
        assertTrue(thrown.message?.contains("Integrity violation") == true)
    }

    @Test
    fun `translates non-integrity SQLExceptions to DbException`() {
        val sql = SQLException("Connection failure", "08001", 0)

        val thrown =
            assertThrows<DbException> {
                translateDbErrors {
                    throw sql
                }
            }
        assertTrue(thrown.message?.contains("Connection failure") == true)
    }

    @Test
    fun `finds nested SQL cause and translates appropriately`() {
        val nested = SQLException("Duplicate entry", "23000", 1062)
        val wrapper = RuntimeException("wrapped", nested)

        val thrown =
            assertThrows<ConflictException> {
                translateDbErrors {
                    throw wrapper
                }
            }
        assertTrue(thrown.message?.contains("Duplicate entry") == true)
        assertTrue(thrown.cause === wrapper)
    }
}
