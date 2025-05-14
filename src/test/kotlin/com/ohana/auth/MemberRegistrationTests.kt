package com.ohana.auth

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MemberRegistrationTests {
    private val objectMapper = jacksonObjectMapper()
    private val client = HttpClient(CIO)

    @Test
    fun whenGivenValidRegistrationRequest_thenReturnsCreatedStatus() =
        runBlocking {
            val request =
                object {
                    val email = "test@example.com"
                    val password = "password"
                    val name = "Test User"
                }
            val requestJson = objectMapper.writeValueAsString(request)

            val createResponse =
                client.post("http://localhost:4240/api/v1/register") {
                    contentType(ContentType.Application.Json)
                    setBody(requestJson)
                }

            assertEquals(HttpStatusCode.Created, createResponse.status)

            val responseJson: JsonNode = objectMapper.readTree(createResponse.bodyAsText())
            val id = responseJson["id"]?.asInt()
            val token = responseJson["token"]?.asText()

            val getResponse =
                client.get("http://localhost:4240/api/v1/members/$id") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            assertEquals(HttpStatusCode.OK, getResponse.status)

            val userJson: JsonNode = objectMapper.readTree(getResponse.bodyAsText())
            val userEmail = userJson["email"]?.asText()
            val userName = userJson["name"]?.asText()

            assertEquals("test@example.com", userEmail)
            assertEquals("Test User", userName)
        }

    @Test
    fun whenGivenInvalidRegistrationRequest_thenReturnsBadRequestStatus() =
        runBlocking {
            val request = object {
                val email = "invalid-email"
                val password = "short"
            }
            val requestJson = objectMapper.writeValueAsString(request)

            val createResponse = client.post("http://localhost:4240/api/v1/register") {
                contentType(ContentType.Application.Json)
                setBody(requestJson)
            }

            assertEquals(HttpStatusCode.BadRequest, createResponse.status)
        }
}
