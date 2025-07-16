package com.ohana.shared.exceptions

class AuthenticationException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause),
    KnownError
