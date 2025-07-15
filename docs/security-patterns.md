# Security Patterns

## Overview

Ohana implements comprehensive security patterns including JWT authentication, password hashing, input validation, and authorization checks. Security is implemented at multiple layers to ensure data protection and proper access control.

## Authentication

### JWT Token Authentication

#### Token Structure

JWT tokens contain user identification and are signed with HMAC256:

```kotlin
// JWT payload structure
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "iat": 1640995200,
  "exp": 1641081600
}
```

#### Token Generation

```kotlin
object JwtCreator {
    fun generateToken(userId: String): String {
        val secret = System.getenv("JWT_SECRET") ?: "default-secret"

        return JWT.create()
            .withClaim("userId", userId)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)) // 24 hours
            .sign(Algorithm.HMAC256(secret))
    }
}
```

#### Token Validation

```kotlin
fun getUserId(principal: JWTPrincipal?): String {
    return principal?.getClaim("userId", String::class)
        ?: throw AuthenticationException("Invalid JWT token")
}
```

### Password Security

#### Password Hashing

Passwords are hashed using salted PBKDF2:

```kotlin
object Hasher {
    fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(32)
        random.nextBytes(salt)
        return salt
    }

    fun hashPassword(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, 65536, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.getEncoder().encodeToString(hash)
    }

    fun verifyPassword(password: String, salt: ByteArray, hashedPassword: String): Boolean {
        val hashToVerify = hashPassword(password, salt)
        return hashToVerify == hashedPassword
    }
}
```

#### Password Requirements

```kotlin
data class Request(
    val password: String,
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (password.isEmpty()) errors.add("Password is required")
        if (password.length < 8) errors.add("Password must be at least 8 characters long")
        if (!password.matches(Regex(".*[A-Z].*"))) errors.add("Password must contain at least one uppercase letter")
        if (!password.matches(Regex(".*[0-9].*"))) errors.add("Password must contain at least one number")
        if (!password.matches(Regex(".*[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\|,.<>\\/?].*"))) {
            errors.add("Password must contain at least one special character")
        }

        return errors
    }
}
```

## Authorization

### Household Membership Validation

#### HouseholdMemberValidator

Validates that a user is an active member of a household:

```kotlin
class HouseholdMemberValidator {
    fun validate(
        context: UnitOfWorkContext,
        householdId: String,
        userId: String,
    ) {
        // Validate GUID format
        if (!Guid.isValid(householdId) || !Guid.isValid(userId)) {
            throw IllegalArgumentException("Household ID and user ID must be valid GUIDs")
        }

        // Check household exists
        context.households.findById(householdId)
            ?: throw NotFoundException("Household not found")

        // Check user is member of household
        val member = context.households.findMemberById(householdId, userId)
            ?: throw AuthorizationException("User is not a member of the household")

        // Check member is active
        if (!member.isActive) {
            throw AuthorizationException("User is not an active member of the household")
        }
    }
}
```

#### Role-Based Authorization

```kotlin
enum class HouseholdMemberRole {
    admin,
    member,
}

// Check admin permissions
fun validateAdminAccess(member: HouseholdMember) {
    if (member.role != HouseholdMemberRole.admin) {
        throw AuthorizationException("User is not an admin of the household")
    }
}

// Check specific permissions
fun validateTaskPermission(member: HouseholdMember, task: Task) {
    // Users can only modify tasks they created or if they're admin
    if (member.role != HouseholdMemberRole.admin && member.memberId != task.createdBy) {
        throw AuthorizationException("User can only modify their own tasks")
    }
}
```

### Resource Ownership Validation

#### Task Ownership

```kotlin
class TaskUpdateHandler(
    private val unitOfWork: UnitOfWork,
    private val householdMemberValidator: HouseholdMemberValidator,
) {
    suspend fun handle(taskId: String, userId: String, request: Request): Response =
        unitOfWork.execute { context ->
            // Validate user is household member
            val task = context.tasks.findById(taskId)
                ?: throw NotFoundException("Task not found")

            householdMemberValidator.validate(context, task.householdId, userId)

            // Check ownership or admin role
            val member = context.households.findMemberById(task.householdId, userId)!!
            if (member.role != HouseholdMemberRole.admin && member.memberId != task.createdBy) {
                throw AuthorizationException("User can only modify their own tasks")
            }

            // Update task
            val updatedTask = context.tasks.update(task.copy(
                title = request.title,
                description = request.description,
                dueDate = request.dueDate,
                status = request.status,
            ))

            Response.from(updatedTask)
        }
}
```

## Input Validation

### Request Validation

All incoming requests are validated at multiple levels:

#### 1. Controller-Level Validation

```kotlin
post("") {
    val request = call.receive<TaskCreationHandler.Request>()

    val validationErrors = request.validate()
    if (validationErrors.isNotEmpty()) {
        throw ValidationException("Validation failed: ${validationErrors.joinToString(", ")}")
    }

    val response = handler.handle(request)
    call.respond(HttpStatusCode.Created, response)
}
```

#### 2. Handler-Level Validation

```kotlin
suspend fun handle(userId: String, request: Request): Response {
    // Additional business rule validation
    val validationErrors = request.validate()
    if (validationErrors.isNotEmpty()) {
        throw ValidationException("Validation failed: ${validationErrors.joinToString(", ")}")
    }

    return unitOfWork.execute { context ->
        // Business logic with additional validation
    }
}
```

### SQL Injection Prevention

#### Parameterized Queries

Always use parameterized queries to prevent SQL injection:

```kotlin
// ✅ Good - Parameterized query
val query = "SELECT * FROM tasks WHERE household_id = :household_id"
val params = mapOf("household_id" to householdId)
val result = DatabaseUtils.get(handle, query, params, Task::class)

// ❌ Bad - String concatenation (SQL injection risk)
val query = "SELECT * FROM tasks WHERE household_id = '$householdId'"
```

#### DatabaseUtils Pattern

All database operations use the `DatabaseUtils` class which enforces parameterized queries:

```kotlin
object DatabaseUtils {
    fun <T> get(handle: Handle, query: String, params: Map<String, Any?>, clazz: Class<T>): List<T> {
        return handle.createQuery(query)
            .bindMap(params)  // Safe parameter binding
            .mapTo(clazz)
            .list()
    }
}
```

### XSS Prevention

#### Input Sanitization

Validate and sanitize all user inputs:

```kotlin
data class Request(
    val title: String,
    val description: String,
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        // Length validation
        if (title.length > 255) errors.add("Title must be at most 255 characters")
        if (description.length > 1000) errors.add("Description must be at most 1000 characters")

        // Content validation (prevent script injection)
        if (title.contains("<script>") || title.contains("javascript:")) {
            errors.add("Title contains invalid content")
        }

        return errors
    }
}
```

## Environment Security

### Configuration Security

#### Environment Variables

Sensitive configuration is stored in environment variables:

```kotlin
// Database configuration
val dbHost = System.getenv("DB_HOST") ?: "localhost"
val dbPort = System.getenv("DB_PORT")?.toIntOrNull() ?: 3306
val dbName = System.getenv("DB_NAME") ?: "ohana"
val dbUser = System.getenv("DB_USER") ?: "root"
val dbPassword = System.getenv("DB_PASSWORD") ?: "root"

// JWT configuration
val jwtSecret = System.getenv("JWT_SECRET")
    ?: throw IllegalStateException("JWT_SECRET environment variable is required")
```

#### Secure Defaults

```kotlin
// ✅ Good - Secure defaults
val port = System.getenv("PORT")?.toIntOrNull() ?: 4242
val jwtSecret = System.getenv("JWT_SECRET")
    ?: throw IllegalStateException("JWT_SECRET is required")

// ❌ Bad - Insecure defaults
val jwtSecret = System.getenv("JWT_SECRET") ?: "default-secret"
```

## CORS Configuration

### Secure CORS Setup

```kotlin
fun Application.configureCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        anyHost()  // Configure specific hosts in production
    }
}
```

## Error Handling Security

### Secure Error Messages

Don't expose sensitive information in error messages:

```kotlin
// ✅ Good - Generic error messages
exception<AuthenticationException> { call, cause ->
    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Authentication failed"))
}

exception<DbException> { call, cause ->
    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Database error occurred"))
}

// ❌ Bad - Exposing sensitive information
exception<DbException> { call, cause ->
    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Database error: ${cause.message}"))
}
```

### Logging Security

```kotlin
// ✅ Good - Secure logging
logger.error("Database operation failed", exception)
logger.info("User ${userId} accessed resource ${resourceId}")

// ❌ Bad - Logging sensitive data
logger.error("Database error: ${exception.message}")
logger.info("User password: ${password}")
```

## Testing Security

### Security Testing

```kotlin
@Test
fun `should reject invalid JWT token`() = testApplication {
    client.get("/api/v1/tasks") {
        header(HttpHeaders.Authorization, "Bearer invalid-token")
    }.apply {
        assertEquals(HttpStatusCode.Unauthorized, status)
    }
}

@Test
fun `should reject SQL injection attempt`() = testApplication {
    val maliciousInput = "'; DROP TABLE tasks; --"

    client.get("/api/v1/tasks?householdId=$maliciousInput") {
        header(HttpHeaders.Authorization, "Bearer $validToken")
    }.apply {
        // Should not crash or expose data
        assertNotEquals(HttpStatusCode.InternalServerError, status)
    }
}

@Test
fun `should validate user permissions`() = testApplication {
    // Test that users can only access their own data
    client.get("/api/v1/tasks?householdId=${otherUserHouseholdId}") {
        header(HttpHeaders.Authorization, "Bearer $userToken")
    }.apply {
        assertEquals(HttpStatusCode.Forbidden, status)
    }
}
```

## Security Best Practices

### 1. Always Validate Input

```kotlin
// ✅ Good - Validate all inputs
val taskId = call.parameters["id"]
    ?: throw ValidationException("Task ID is required")

if (!Guid.isValid(taskId)) {
    throw ValidationException("Task ID must be a valid GUID")
}

// ❌ Bad - Trust user input
val taskId = call.parameters["id"] ?: ""
```

### 2. Use HTTPS in Production

```kotlin
// Configure SSL/TLS in production
fun Application.configureSSL() {
    if (environment.developmentMode) {
        // Development configuration
    } else {
        // Production SSL configuration
    }
}
```

### 3. Implement Rate Limiting

```kotlin
// Future enhancement
install(RateLimit) {
    register {
        rateLimiter(limit = 100, refillPeriod = 1.minutes)
    }
}
```

### 4. Regular Security Audits

- Review authentication flows
- Check authorization logic
- Validate input sanitization
- Test for common vulnerabilities
- Update dependencies regularly

### 5. Secure Session Management

```kotlin
// JWT tokens with appropriate expiration
fun generateToken(userId: String): String {
    return JWT.create()
        .withClaim("userId", userId)
        .withIssuedAt(Date())
        .withExpiresAt(Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)) // 24 hours
        .sign(Algorithm.HMAC256(jwtSecret))
}
```

## Summary

The security patterns provide:

- **Strong authentication** with JWT tokens
- **Secure password handling** with salted hashing
- **Comprehensive authorization** with role-based access control
- **Input validation** at multiple layers
- **SQL injection prevention** with parameterized queries
- **Secure error handling** without information leakage
- **Environment-based configuration** for sensitive data

These patterns ensure that the application is secure against common vulnerabilities while maintaining usability and performance.
