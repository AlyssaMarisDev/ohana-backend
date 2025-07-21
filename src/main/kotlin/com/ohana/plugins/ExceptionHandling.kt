package com.ohana.plugins

import com.ohana.shared.exceptions.AuthenticationException
import com.ohana.shared.exceptions.AuthorizationException
import com.ohana.shared.exceptions.ConflictException
import com.ohana.shared.exceptions.DbException
import com.ohana.shared.exceptions.NotFoundException
import com.ohana.shared.exceptions.ValidationException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.ohana.plugins.ExceptionHandling")

fun Application.configureExceptionHandling() {
    install(StatusPages) {
        exception<NotFoundException> { call, cause ->
            logger.info("Resource not found: ${cause.message}")
            call.respond(HttpStatusCode.NotFound, "Resource not found: ${cause.message}")
        }

        exception<DbException> { call, cause ->
            logger.error("Database error: ${cause.message}", cause)
            call.respond(HttpStatusCode.InternalServerError, "Database error: ${cause.message}")
        }

        exception<ValidationException> { call, cause ->
            logger.info("Validation error: ${cause.message}, ${cause.errors}")
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "error" to "Validation failed",
                    "details" to cause.errors.map { "${it.field}: ${it.message}" },
                ),
            )
        }

        exception<AuthenticationException> { call, cause ->
            logger.info("Authentication failed: ${cause.message}")
            call.respond(HttpStatusCode.Unauthorized, "Authentication failed: ${cause.message}")
        }

        exception<AuthorizationException> { call, cause ->
            logger.info("Authorization failed: ${cause.message}")
            call.respond(HttpStatusCode.Forbidden, "Authorization failed: ${cause.message}")
        }

        exception<ConflictException> { call, cause ->
            logger.info("Conflict: ${cause.message}")
            call.respond(HttpStatusCode.Conflict, "Conflict: ${cause.message}")
        }

        exception<BadRequestException> { call, cause ->
            logger.info("Bad request: ${cause.message}")
            call.respond(HttpStatusCode.BadRequest, "Bad request: ${cause.message}")
        }

        exception<Throwable> { call, cause ->
            logger.error("An unexpected error occurred: ${cause.message}", cause)
            call.respond(HttpStatusCode.InternalServerError, "An unexpected error occurred: ${cause.message}")
        }
    }
}
