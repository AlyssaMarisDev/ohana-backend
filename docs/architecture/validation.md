# Validation Patterns

## Overview

Ohana uses manual validation in API request models with `toDomain()` conversion for all request validation. This approach provides:

1. **Flexible validation logic** for complex business rules
2. **Data transformation** before passing to domain handlers
3. **Clear separation** between API concerns and domain concerns

## Validation Approach

### API Request Models with Manual Validation

For all request validation, use separate API request models with manual validation and `toDomain()` conversion.

#### API Request Model Structure

```kotlin
// src/main/kotlin/com/ohana/api/task/models/TaskUpdateRequest.kt
data class TaskUpdateRequest(
    val title: String?,
    val description: String?,
    val dueDate: Instant?,
    val status: String?,
) {
    fun toDomain(): TaskUpdateByIdHandler.Request {
        val errors = mutableListOf<ValidationError>()

        // Validate title
        if (title == null) {
            errors.add(ValidationError("title", "Title cannot be blank"))
        } else if (title.isBlank()) {
            errors.add(ValidationError("title", "Title cannot be blank"))
        } else if (title.length > 255) {
            errors.add(ValidationError("title", "Title must be at most 255 characters long"))
        }

        // Validate description
        if (description == null) {
            errors.add(ValidationError("description", "Description cannot be blank"))
        } else if (description.isBlank()) {
            errors.add(ValidationError("description", "Description cannot be blank"))
        } else if (description.length > 1000) {
            errors.add(ValidationError("description", "Description must be at most 1000 characters long"))
        }

        // Validate due date
        if (dueDate == null) {
            errors.add(ValidationError("dueDate", "Due date is required"))
        }

        // Validate status
        if (status == null) {
            errors.add(ValidationError("status", "Status is required"))
        } else {
            try {
                TaskStatus.valueOf(status)
            } catch (e: IllegalArgumentException) {
                errors.add(ValidationError("status", "Status must be one of: PENDING, IN_PROGRESS, COMPLETED"))
            }
        }

        if (errors.isNotEmpty()) {
            throw ValidationException("Validation failed", errors)
        }

        return TaskUpdateByIdHandler.Request(
            title = title!!,
            description = description!!,
            dueDate = dueDate!!,
            status = TaskStatus.valueOf(status!!),
        )
    }
}
```

#### Usage in Controllers

```kotlin
put("/{taskId}") {
    val userId = getUserId(call.principal<JWTPrincipal>())
    val id = call.parameters["taskId"]
        ?: throw ValidationException("Task ID is required", listOf(ValidationError("taskId", "Task ID is required")))

    // 1. Accept the new request type
    val request = call.receive<TaskUpdateRequest>()

    // 2. Call the validate method on the new request class (validation happens in toDomain)
    // 3. Calls .toDomain() on the new request type to get the TaskUpdateByIdHandler.Request before calling the handler
    val domainRequest = request.toDomain()

    val response = taskUpdateByIdHandler.handle(userId, id, domainRequest)
    call.respond(HttpStatusCode.OK, response)
}
```

#### Domain Handler Request

```kotlin
// Domain handler expects non-nullable fields after validation
data class Request(
    val title: String,
    val description: String,
    val dueDate: Instant,
    val status: TaskStatus,
)
```

## When to Use API Request Models

API request models with `toDomain()` validation should be used for:

- All request validation scenarios
- Partial updates (nullable fields)
- Complex validation logic
- Data transformation before domain processing
- Separation of API concerns from domain concerns
- Validation of string representations of enums
- Custom error messages for specific scenarios

## Error Handling

Validation errors are automatically caught and returned as HTTP 400 Bad Request responses:

```json
{
  "error": "Validation failed",
  "details": ["title: Title cannot be blank", "dueDate: Due date is required"]
}
```

## Testing

### Testing API Request Models

```kotlin
@Test
fun `toDomain should pass when all fields are valid`() = runTest {
    val request = TaskUpdateRequest(
        title = "Valid Title",
        description = "Valid description",
        dueDate = Instant.now().plusSeconds(3600),
        status = "PENDING"
    )

    val domainRequest = request.toDomain()

    assertEquals("Valid Title", domainRequest.title)
    assertEquals("Valid description", domainRequest.description)
    assertEquals(request.dueDate, domainRequest.dueDate)
    assertEquals(TaskStatus.PENDING, domainRequest.status)
}

@Test
fun `toDomain should throw ValidationException when title is null`() = runTest {
    val request = TaskUpdateRequest(
        title = null,
        description = "Valid description",
        dueDate = Instant.now().plusSeconds(3600),
        status = "PENDING"
    )

    val exception = assertThrows<ValidationException> {
        request.toDomain()
    }

    assertEquals("Validation failed", exception.message)
    assertEquals(1, exception.errors.size)
    assertEquals("title", exception.errors[0].field)
    assertEquals("Title cannot be blank", exception.errors[0].message)
}

@Test
fun `toDomain should throw ValidationException with multiple errors`() = runTest {
    val request = TaskUpdateRequest(
        title = "", // Blank title
        description = "A".repeat(1001), // Too long description
        dueDate = null, // Missing due date
        status = "INVALID_STATUS" // Invalid status
    )

    val exception = assertThrows<ValidationException> {
        request.toDomain()
    }

    assertEquals("Validation failed", exception.message)
    assertEquals(4, exception.errors.size)

    val errorFields = exception.errors.map { it.field }.toSet()
    assertEquals(setOf("title", "description", "dueDate", "status"), errorFields)
}

@Test
fun `toDomain should accept all valid status values`() = runTest {
    val validStatuses = listOf("PENDING", "IN_PROGRESS", "COMPLETED")

    validStatuses.forEach { status ->
        val request = TaskUpdateRequest(
            title = "Valid Title",
            description = "Valid description",
            dueDate = Instant.now().plusSeconds(3600),
            status = status
        )

        val domainRequest = request.toDomain()
        assertEquals(TaskStatus.valueOf(status), domainRequest.status)
    }
}
```

## Best Practices

### 1. Use Descriptive Error Messages

```kotlin
// Good
errors.add(ValidationError("title", "Title cannot be blank"))

// Bad
errors.add(ValidationError("title", "Required"))
```

### 2. Group Related Validations in API Models

```kotlin
data class TaskUpdateRequest(
    // Basic information
    val title: String?,
    val description: String?,

    // Scheduling
    val dueDate: Instant?,
    val status: String?,
) {
    fun toDomain(): TaskUpdateByIdHandler.Request {
        val errors = mutableListOf<ValidationError>()

        // Validate basic information
        if (title == null) {
            errors.add(ValidationError("title", "Title cannot be blank"))
        } else if (title.isBlank()) {
            errors.add(ValidationError("title", "Title cannot be blank"))
        } else if (title.length > 255) {
            errors.add(ValidationError("title", "Title must be at most 255 characters long"))
        }

        // Validate scheduling
        if (dueDate == null) {
            errors.add(ValidationError("dueDate", "Due date is required"))
        }

        // ... rest of validation
    }
}
```

### 3. Handle Enum Validation Properly

```kotlin
// Validate status
if (status == null) {
    errors.add(ValidationError("status", "Status is required"))
} else {
    try {
        TaskStatus.valueOf(status)
    } catch (e: IllegalArgumentException) {
        errors.add(ValidationError("status", "Status must be one of: PENDING, IN_PROGRESS, COMPLETED"))
    }
}
```

### 4. Validate Required vs Optional Fields

```kotlin
// Required fields
if (title == null) {
    errors.add(ValidationError("title", "Title cannot be blank"))
}

// Optional fields (only validate if provided)
if (description != null && description.length > 1000) {
    errors.add(ValidationError("description", "Description must be at most 1000 characters long"))
}
```

## Performance Considerations

- Manual validation in API models should be lightweight
- Avoid database calls in validators (use separate business logic validation)
- Keep validation logic focused on data format and basic business rules

## File Organization

### API Request Models

```
src/main/kotlin/com/ohana/api/task/models/
├── TaskUpdateRequest.kt
├── TaskCreationRequest.kt
└── TaskFilterRequest.kt
```

### Domain Request Models

```
src/main/kotlin/com/ohana/domain/task/
├── TaskUpdateByIdHandler.kt (contains Request data class)
├── TaskCreationHandler.kt (contains Request data class)
└── TaskGetAllHandler.kt (contains Request data class)
```

## Migration Guide

When creating new endpoints or updating existing ones:

1. **Create API request model** in `/api/{resource}/models/` directory
2. **Implement `toDomain()` method** with validation logic
3. **Update controller** to use `call.receive<ApiRequest>()` and `request.toDomain()`
4. **Write comprehensive tests** for the `toDomain()` method
