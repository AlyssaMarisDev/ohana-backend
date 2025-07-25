package com.ohana.shared.exceptions

data class ValidationError(
    val field: String,
    val message: String,
)

class ValidationException(
    message: String,
    val errors: List<ValidationError>? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause),
    KnownError
