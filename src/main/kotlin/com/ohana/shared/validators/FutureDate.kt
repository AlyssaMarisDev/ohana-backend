package com.ohana.shared.validators

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import java.time.Instant
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [FutureDateValidator::class])
annotation class FutureDate(
    val message: String = "Date must be in the future",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class FutureDateValidator : ConstraintValidator<FutureDate, Instant> {
    override fun isValid(
        value: Instant?,
        context: ConstraintValidatorContext?,
    ): Boolean {
        if (value == null) return true // Let @NotNull handle null validation
        return value.isAfter(Instant.now())
    }
}
