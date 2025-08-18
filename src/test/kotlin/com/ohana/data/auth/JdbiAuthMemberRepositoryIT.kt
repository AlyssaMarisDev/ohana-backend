package com.ohana.data.auth

import org.flywaydb.core.Flyway
import org.h2.jdbcx.JdbcDataSource
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class JdbiAuthMemberRepositoryIT {
    private lateinit var jdbi: Jdbi
    private lateinit var handle: Handle

    @BeforeEach
    fun setUp() {
        val jdbcUrl = "jdbc:h2:mem:ohana_it;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1"
        val dataSource =
            JdbcDataSource().apply {
                setURL(jdbcUrl)
                user = "sa"
                password = ""
            }

        val flyway =
            Flyway
                .configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .cleanDisabled(false)
                .cleanOnValidationError(true)
                .load()

        flyway.clean()
        flyway.migrate()

        jdbi = Jdbi.create(dataSource)
        handle = jdbi.open()
    }

    @AfterEach
    fun tearDown() {
        handle.close()
    }

    @Test
    fun create_insertsAndReturnsMember() {
        val repository = JdbiAuthMemberRepository(handle)

        val member =
            AuthMember(
                id = UUID.randomUUID().toString(),
                name = "Test User",
                email = "test@example.com",
                password = "hashed",
                salt = "salt".toByteArray(),
                age = 30,
                gender = "other",
            )

        val created = repository.create(member)

        assertNotNull(created)
        assertEquals(member, created)

        val fetched = repository.findByEmail(member.email)
        assertNotNull(fetched)
        assertEquals(member, fetched)
    }

    @Test
    fun findByEmail_returnsNullWhenNotFound() {
        val repository = JdbiAuthMemberRepository(handle)

        val result = repository.findByEmail("missing@example.com")
        assertNull(result)
    }

    @Test
    fun create_enforcesUniqueEmail() {
        val repository = JdbiAuthMemberRepository(handle)

        val email = "unique@example.com"
        val first =
            AuthMember(
                id = UUID.randomUUID().toString(),
                name = "First",
                email = email,
                password = "p1",
                salt = byteArrayOf(1, 2, 3),
                age = null,
                gender = null,
            )
        val second =
            AuthMember(
                id = UUID.randomUUID().toString(),
                name = "Second",
                email = email, // same email
                password = "p2",
                salt = byteArrayOf(4, 5, 6),
                age = null,
                gender = null,
            )

        repository.create(first)

        assertThrows(Exception::class.java) {
            repository.create(second)
        }
    }
}
