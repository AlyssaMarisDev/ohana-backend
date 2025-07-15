package com.ohana.exceptions

data class ValidationError(
    val field: String,
    val message: String,
)

class ValidationException(
    message: String,
    val errors: List<ValidationError>,
    cause: Throwable? = null,
) : RuntimeException(message, cause),
    KnownError
