package com.ohana.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.*
import kotlin.time.Duration.Companion.seconds

fun Application.configureRateLimit() {
    // Load rate limiting configuration from environment variables
    val rateLimit = System.getenv("RATE_LIMIT")?.toIntOrNull() ?: 100
    val refillPeriodSeconds = System.getenv("RATE_LIMIT_REFILL_PERIOD_SECONDS")?.toIntOrNull() ?: 60

    install(RateLimit) {
        global {
            rateLimiter(
                limit = rateLimit,
                refillPeriod = refillPeriodSeconds.seconds,
            )
            requestKey { call ->
                call.request.origin.remoteHost
                    .toString()
            }
        }
    }
}
