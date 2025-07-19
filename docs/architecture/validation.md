# Validation Patterns

## Overview

Ohana uses Bean Validation (JSR-303) annotations for request body validation. This provides a declarative, annotation-based approach to validation that is more maintainable and less error-prone than manual validation.

## Setup

### Dependencies

The following dependencies are required for annotation-based validation:

```kotlin
// Bean Validation (JSR-303)
implementation("jakarta.validation:jakarta.validation-api:3.0.2")
implementation("org.hibernate.validator:hibernate-validator:8.0.1.Final")
implementation("org.glassfish:jakarta.el:4.0.2")
```

### Validation Plugin

The `ValidationPlugin` automatically validates request bodies using Bean Validation annotations:

```kotlin
class ValidationPlugin {
    private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

    fun validate(obj: Any) {
        val violations: Set<ConstraintViolation<Any>> = validator.validate(obj)

        if (violations.isNotEmpty()) {
            val errors = violations.map { violation ->
                ValidationError(
                    field = violation.propertyPath.toString(),
                    message = violation.message
                )
            }
            throw ValidationException("Validation failed", errors)
        }
    }
}
```

## Usage in Controllers

### Basic Usage

Instead of manual validation, use the `validateAndReceive` extension function:

```kotlin
// Before (manual validation)
post("") {
    val request = call.receive<TaskCreationHandler.Request>()
    val validationErrors = request.validate()
    if (validationErrors.isNotEmpty()) {
        throw ValidationException("Validation failed", validationErrors)
    }
    // ... rest of handler
}

// After (annotation-based validation)
post("") {
    val request = call.validateAndReceive(TaskCreationHandler.Request::class.java)
    // ... rest of handler
}
```

### Example Controller

```kotlin
class TaskController(
    private val taskCreationHandler: TaskCreationHandler,
) {
    fun Route.registerTaskRoutes() {
        authenticate("auth-jwt") {
            route("/tasks") {
                post("") {
                    val userId = getUserId(call.principal<JWTPrincipal>())
                    val householdId = call.parameters["householdId"]
                        ?: throw ValidationException("Household ID is required")

                    // Automatic validation using annotations
                    val request = call.validateAndReceive(TaskCreationHandler.Request::class.java)

                    val response = taskCreationHandler.handle(userId, householdId, request)
                    call.respond(HttpStatusCode.Created, response)
                }
            }
        }
    }
}
```

## Request Object Annotations

### Standard Bean Validation Annotations

```kotlin
data class Request(
    @field:NotBlank(message = "Title is required")
    @field:Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    val title: String,

    @field:NotBlank(message = "Description is required")
    @field:Size(max = 1000, message = "Description must be at most 1000 characters")
    val description: String,

    @field:Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "ID must be a valid GUID")
    val id: String,

    @field:Min(value = 0, message = "Age must be non-negative")
    val age: Int?,

    @field:Email(message = "Invalid email format")
    val email: String,
)
```

### Custom Validators

Create custom validation annotations for domain-specific validation:

```kotlin
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [FutureDateValidator::class])
annotation class FutureDate(
    val message: String = "Date must be in the future",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class FutureDateValidator : ConstraintValidator<FutureDate, Instant> {
    override fun isValid(value: Instant?, context: ConstraintValidatorContext?): Boolean {
        if (value == null) return true // Let @NotNull handle null validation
        return value.isAfter(Instant.now())
    }
}
```

### Usage of Custom Validators

```kotlin
data class TaskRequest(
    @field:NotBlank(message = "Title is required")
    val title: String,

    @field:FutureDate(message = "Due date cannot be in the past")
    val dueDate: Instant,
)
```

## Available Annotations

### String Validation

- `@NotBlank` - String must not be null, empty, or whitespace
- `@NotEmpty` - String must not be null or empty
- `@Size(min, max)` - String length constraints
- `@Pattern(regexp)` - Regular expression validation
- `@Email` - Email format validation

### Numeric Validation

- `@Min(value)` - Minimum value
- `@Max(value)` - Maximum value
- `@Positive` - Must be positive
- `@Negative` - Must be negative
- `@PositiveOrZero` - Must be positive or zero
- `@NegativeOrZero` - Must be negative or zero

### Date/Time Validation

- `@Future` - Date must be in the future
- `@Past` - Date must be in the past
- `@FutureOrPresent` - Date must be in the future or present
- `@PastOrPresent` - Date must be in the past or present

### Collection Validation

- `@NotEmpty` - Collection must not be empty
- `@Size(min, max)` - Collection size constraints

### Null Validation

- `@NotNull` - Field must not be null
- `@Null` - Field must be null

## Error Handling

Validation errors are automatically caught and returned as HTTP 400 Bad Request responses:

```json
{
  "error": "Validation failed",
  "details": [
    "title: Title is required",
    "dueDate: Due date cannot be in the past"
  ]
}
```

## Migration from Manual Validation

### Before (Manual Validation)

```kotlin
data class Request(
    val title: String,
    val description: String,
) {
    fun validate(): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (title.isEmpty()) {
            errors.add(ValidationError("title", "Title is required"))
        }
        if (title.length > 255) {
            errors.add(ValidationError("title", "Title must be at most 255 characters"))
        }
        if (description.isEmpty()) {
            errors.add(ValidationError("description", "Description is required"))
        }

        return errors
    }
}
```

### After (Annotation-Based Validation)

```kotlin
data class Request(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 255, message = "Title must be at most 255 characters")
    val title: String,

    @field:NotBlank(message = "Description is required")
    val description: String,
)
```

## Best Practices

### 1. Use Descriptive Error Messages

```kotlin
// Good
@field:NotBlank(message = "User email is required for account creation")

// Bad
@field:NotBlank(message = "Required")
```

### 2. Group Related Validations

```kotlin
data class UserRegistrationRequest(
    // Personal information
    @field:NotBlank(message = "First name is required")
    @field:Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    val lastName: String,

    // Contact information
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,

    // Security
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters long")
    val password: String,
)
```

### 3. Create Custom Validators for Domain Logic

```kotlin
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidGUIDValidator::class])
annotation class ValidGUID(
    val message: String = "Must be a valid GUID",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class ValidGUIDValidator : ConstraintValidator<ValidGUID, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value == null) return true
        return Guid.isValid(value)
    }
}
```

### 4. Use Validation Groups for Conditional Validation

```kotlin
interface CreateGroup
interface UpdateGroup

data class TaskRequest(
    @field:NotBlank(message = "Title is required", groups = [CreateGroup::class, UpdateGroup::class])
    val title: String,

    @field:NotBlank(message = "ID is required", groups = [UpdateGroup::class])
    val id: String?,
)
```

## Testing

### Unit Testing Validators

```kotlin
@Test
fun `should validate valid request`() {
    val request = TaskCreationHandler.Request(
        id = UUID.randomUUID().toString(),
        title = "Valid Title",
        description = "Valid description",
        dueDate = Instant.now().plusSeconds(3600),
        status = TaskStatus.PENDING
    )

    val violations = validator.validate(request)
    assertTrue(violations.isEmpty())
}

@Test
fun `should fail validation with invalid data`() {
    val request = TaskCreationHandler.Request(
        id = "",
        title = "",
        description = "",
        dueDate = Instant.now().minusSeconds(3600),
        status = TaskStatus.PENDING
    )

    val violations = validator.validate(request)
    assertFalse(violations.isEmpty())
    assertEquals(4, violations.size)
}
```

## Performance Considerations

- Bean Validation is generally fast for most use cases
- Custom validators should be lightweight
- Avoid database calls in validators (use separate business logic validation)
- Consider caching validator instances for frequently used validators

## Integration with Existing Code

The annotation-based validation system is designed to work alongside existing manual validation where needed. You can gradually migrate request objects to use annotations while maintaining backward compatibility.
