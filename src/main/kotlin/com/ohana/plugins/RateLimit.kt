package com.ohana.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.*
import kotlin.time.Duration.Companion.seconds

fun Application.configureRateLimit(rateLimitConfig: RateLimitConfig) {
    install(RateLimit) {
        global {
            rateLimiter(
                limit = rateLimitConfig.requestsPerMinute,
                refillPeriod = 60.seconds, // 1 minute refill period
            )
            requestKey { call ->
                call.request.origin.remoteHost
                    .toString()
            }
        }
    }
}
