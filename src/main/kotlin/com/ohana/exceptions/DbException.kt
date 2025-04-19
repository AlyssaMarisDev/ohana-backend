package com.ohana.exceptions

class DbException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause),
    KnownError
