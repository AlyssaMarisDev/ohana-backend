package com.ohana.plugins

import io.ktor.server.application.*
import io.ktor.server.request.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.ohana.plugins.CallLogging")

fun Application.configureCallLogging() {
    intercept(ApplicationCallPipeline.Monitoring) {
        // Log request details
        val request = call.request
        logger.info("Request: ${request.httpMethod.value} - ${request.uri}")
        logger.info("Request Headers: ${request.headers.entries().map { "${it.key}: ${it.value}" }.joinToString(", ") { it }}")
    }

    intercept(ApplicationCallPipeline.Fallback) {
        // Log response details
        val response = call.response
        logger.info("Response Status: ${response.status()}")
    }
}
