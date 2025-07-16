package com.ohana.shared.exceptions

class AuthorizationException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause),
    KnownError
