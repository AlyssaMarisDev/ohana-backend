package com.ohana.exceptions

class ValidationException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause),
    KnownError
