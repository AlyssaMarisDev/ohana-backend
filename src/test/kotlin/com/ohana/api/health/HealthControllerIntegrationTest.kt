package com.ohana.api.health

import com.ohana.IntegrationTestUtils.configureMinimalTestApplication
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import kotlin.test.*

class HealthControllerIntegrationTest {
    @Test
    fun `health endpoint should return OK status and message`() =
        testApplication {
            application {
                configureMinimalTestApplication()
            }

            val response = client.get("/api/v1/health")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("OK", response.bodyAsText())
        }
}
