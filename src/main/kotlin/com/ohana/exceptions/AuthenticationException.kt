package com.ohana.exceptions

class AuthenticationException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause),
    KnownError
