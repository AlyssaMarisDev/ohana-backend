package com.ohana.domain.validators

import com.ohana.exceptions.ValidationError
import com.ohana.exceptions.ValidationException

class ObjectValidator {
    fun validate(request: Validatable) {
        val validationErrors = request.validate()
        if (validationErrors.isNotEmpty()) {
            throw ValidationException("Validation failed", validationErrors)
        }
    }
}

interface Validatable {
    fun validate(): List<ValidationError>
}
