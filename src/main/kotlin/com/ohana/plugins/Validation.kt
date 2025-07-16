package com.ohana.plugins

import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.AttributeKey
import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator

class ValidationPlugin {
    private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

    fun validate(obj: Any) {
        val violations: Set<ConstraintViolation<Any>> = validator.validate(obj)

        if (violations.isNotEmpty()) {
            val errors =
                violations.map { violation ->
                    ValidationError(
                        field = violation.propertyPath.toString(),
                        message = violation.message,
                    )
                }
            throw ValidationException("Validation failed", errors)
        }
    }
}

fun Application.configureValidation() {
    val validationPlugin = ValidationPlugin()

    // Store the validation plugin in application attributes for easy access
    attributes.put(ValidationPluginKey, validationPlugin)
}

val ValidationPluginKey = AttributeKey<ValidationPlugin>("ValidationPlugin")

// Extension function to get the validation plugin
fun Application.validationPlugin(): ValidationPlugin = attributes[ValidationPluginKey]

// Extension function to validate request bodies
suspend inline fun <reified T : Any> ApplicationCall.validateAndReceive(): T {
    val request = receive<T>()
    application.validationPlugin().validate(request)
    return request
}
