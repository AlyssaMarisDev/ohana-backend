package com.ohana.exceptions

class ConflictException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause),
    KnownError
