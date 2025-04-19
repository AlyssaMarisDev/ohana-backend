package com.ohana.exceptions

class NotFoundException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause),
    KnownError
