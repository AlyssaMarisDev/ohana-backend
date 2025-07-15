# Configuration Management

## Overview

Ohana uses environment-based configuration to manage different deployment environments (development, staging, production). Configuration is centralized and follows security best practices.

## Environment Variables

### Required Environment Variables

#### Database Configuration

```bash
# Database connection
DB_HOST=localhost
DB_PORT=3306
DB_NAME=ohana
DB_USER=root
DB_PASSWORD=root

# JWT configuration
JWT_SECRET=your-super-secret-jwt-key-here

# Server configuration
PORT=4242
```

#### Optional Environment Variables

```bash
# Logging level
LOG_LEVEL=INFO

# CORS configuration (for production)
ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com

# Database connection pool
DB_POOL_SIZE=10
DB_CONNECTION_TIMEOUT=30000

# Rate limiting configuration
RATE_LIMIT=100
RATE_LIMIT_REFILL_PERIOD_SECONDS=60
```

### Environment Variable Loading

#### Application Configuration

```kotlin
fun Application.configureDatabase() {
    val dbHost = System.getenv("DB_HOST") ?: "localhost"
    val dbPort = System.getenv("DB_PORT")?.toIntOrNull() ?: 3306
    val dbName = System.getenv("DB_NAME") ?: "ohana"
    val dbUser = System.getenv("DB_USER") ?: "root"
    val dbPassword = System.getenv("DB_PASSWORD") ?: "root"

    // Database configuration
    val jdbi = Jdbi.create("jdbc:mysql://$dbHost:$dbPort/$dbName?useSSL=false&serverTimezone=UTC")
        .installPlugin(SqlObjectPlugin())
        .installPlugin(KotlinPlugin())
        .installPlugin(KotlinSqlObjectPlugin())

    // Register in DI
    single { jdbi }
}
```

#### JWT Configuration

```kotlin
fun Application.configureAuthentication() {
    val jwtSecret = System.getenv("JWT_SECRET")
        ?: throw IllegalStateException("JWT_SECRET environment variable is required")

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "Ohana API"
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer("ohana")
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("userId").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}
```

## Configuration Classes

### Database Configuration

```kotlin
data class DatabaseConfig(
    val host: String,
    val port: Int,
    val name: String,
    val user: String,
    val password: String,
    val poolSize: Int = 10,
    val connectionTimeout: Int = 30000,
) {
    val url: String
        get() = "jdbc:mysql://$host:$port/$name?useSSL=false&serverTimezone=UTC"

    companion object {
        fun fromEnvironment(): DatabaseConfig {
            return DatabaseConfig(
                host = System.getenv("DB_HOST") ?: "localhost",
                port = System.getenv("DB_PORT")?.toIntOrNull() ?: 3306,
                name = System.getenv("DB_NAME") ?: "ohana",
                user = System.getenv("DB_USER") ?: "root",
                password = System.getenv("DB_PASSWORD") ?: "root",
                poolSize = System.getenv("DB_POOL_SIZE")?.toIntOrNull() ?: 10,
                connectionTimeout = System.getenv("DB_CONNECTION_TIMEOUT")?.toIntOrNull() ?: 30000,
            )
        }
    }
}
```

### Application Configuration

```kotlin
data class AppConfig(
    val port: Int,
    val jwtSecret: String,
    val logLevel: String,
    val allowedOrigins: List<String>,
    val rateLimit: Int,
    val rateLimitRefillPeriodSeconds: Int,
) {
    companion object {
        fun fromEnvironment(): AppConfig {
            return AppConfig(
                port = System.getenv("PORT")?.toIntOrNull() ?: 4242,
                jwtSecret = System.getenv("JWT_SECRET")
                    ?: throw IllegalStateException("JWT_SECRET is required"),
                logLevel = System.getenv("LOG_LEVEL") ?: "INFO",
                allowedOrigins = System.getenv("ALLOWED_ORIGINS")?.split(",") ?: listOf("*"),
                rateLimit = System.getenv("RATE_LIMIT")?.toIntOrNull() ?: 100,
                rateLimitRefillPeriodSeconds = System.getenv("RATE_LIMIT_REFILL_PERIOD_SECONDS")?.toIntOrNull() ?: 60,
            )
        }
    }
}
```

## Configuration Loading

### Application Setup

```kotlin
fun main() {
    // Load configuration
    val appConfig = AppConfig.fromEnvironment()
    val dbConfig = DatabaseConfig.fromEnvironment()

    // Start application
    embeddedServer(Netty, port = appConfig.port) {
        configureSerialization()
        configureCORS(appConfig.allowedOrigins)
        configureAuthentication(appConfig.jwtSecret)
        configureDatabase(dbConfig)
        configureExceptionHandling()
        configureRouting()
    }.start(wait = true)
}
```

### Ktor Configuration

```kotlin
fun Application.configureCORS(allowedOrigins: List<String>) {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)

        if (allowedOrigins.contains("*")) {
            anyHost()
        } else {
            allowedOrigins.forEach { origin ->
                allowHost(origin)
            }
        }
    }
}
```

## Logging Configuration

### Logback Configuration

```xml
<!-- src/main/resources/logback.xml -->
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="${LOG_LEVEL:-INFO}">
        <appender-ref ref="STDOUT" />
    </root>

    <!-- Application-specific logging -->
    <logger name="com.ohana" level="${LOG_LEVEL:-INFO}" />

    <!-- Database logging -->
    <logger name="org.jdbi" level="WARN" />
</configuration>
```

### Logging Usage

```kotlin
class TaskCreationHandler(
    private val unitOfWork: UnitOfWork,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun handle(userId: String, request: Request): Response {
        logger.info("Creating task for user: $userId")

        return unitOfWork.execute { context ->
            try {
                val task = context.tasks.create(Task(...))
                logger.info("Task created successfully: ${task.id}")
                Response.from(task)
            } catch (e: Exception) {
                logger.error("Failed to create task for user: $userId", e)
                throw e
            }
        }
    }
}
```

## Database Configuration

### Connection Pool Configuration

```kotlin
fun Application.configureDatabase(config: DatabaseConfig) {
    val jdbi = Jdbi.create(config.url)
        .installPlugin(SqlObjectPlugin())
        .installPlugin(KotlinPlugin())
        .installPlugin(KotlinSqlObjectPlugin())

    // Configure connection pool
    jdbi.useHandle<Exception> { handle ->
        // Test connection
        handle.execute("SELECT 1")
    }

    // Register in DI
    single { jdbi }
    single { jdbi.open() }
}
```

### Database Migration

```kotlin
fun Application.configureDatabaseMigrations() {
    val jdbi = get<Jdbi>()

    jdbi.useHandle<Exception> { handle ->
        // Run database migrations
        val migrationScript = javaClass.getResource("/schema/database_schema.sql")?.readText()
            ?: throw IllegalStateException("Database schema not found")

        handle.execute(migrationScript)
    }
}
```

## Rate Limiting Configuration

### Global Rate Limiting

The application uses Ktor's RateLimit plugin to implement global rate limiting. This helps protect the API from abuse and ensures fair usage across all clients.

#### Configuration Parameters

- **RATE_LIMIT**: Maximum number of requests allowed in the time window (default: 100)
- **RATE_LIMIT_REFILL_PERIOD_SECONDS**: Time window in seconds for rate limiting (default: 60)

#### Rate Limiting Behavior

- **Token Bucket Algorithm**: Uses a token bucket algorithm for rate limiting
- **Global Scope**: All requests are rate limited globally (not per user/IP)
- **HTTP 429**: Returns HTTP 429 (Too Many Requests) when rate limit is exceeded
- **Automatic Refill**: Tokens are automatically refilled based on the configured period

#### Example Configuration

```bash
# Development - More lenient rate limiting
RATE_LIMIT=200
RATE_LIMIT_REFILL_PERIOD_SECONDS=60

# Production - Stricter rate limiting
RATE_LIMIT=100
RATE_LIMIT_REFILL_PERIOD_SECONDS=60
```

## Security Configuration

### Environment Variable Security

```kotlin
// ✅ Good - Secure configuration
val jwtSecret = System.getenv("JWT_SECRET")
    ?: throw IllegalStateException("JWT_SECRET environment variable is required")

// ❌ Bad - Insecure defaults
val jwtSecret = System.getenv("JWT_SECRET") ?: "default-secret"
```

### Configuration Validation

```kotlin
fun validateConfiguration(config: AppConfig) {
    require(config.jwtSecret.length >= 32) { "JWT_SECRET must be at least 32 characters long" }
    require(config.port in 1..65535) { "PORT must be between 1 and 65535" }
    require(config.allowedOrigins.isNotEmpty()) { "ALLOWED_ORIGINS cannot be empty" }
}
```

## Testing Configuration

### Test Environment

```kotlin
// src/test/resources/application-test.conf
ktor {
    deployment {
        port = 0  // Random port for testing
    }
    application {
        modules = [ com.ohana.ApplicationKt.module ]
    }
}
```

### Test Database Configuration

```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabaseTest {
    private lateinit var jdbi: Jdbi

    @BeforeAll
    fun setup() {
        // Use H2 in-memory database for tests
        jdbi = Jdbi.create("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
            .installPlugin(SqlObjectPlugin())
            .installPlugin(KotlinPlugin())
            .installPlugin(KotlinSqlObjectPlugin())

        // Run test schema
        jdbi.useHandle<Exception> { handle ->
            handle.execute("""
                CREATE TABLE tasks (
                    id VARCHAR(36) PRIMARY KEY,
                    title VARCHAR(255) NOT NULL,
                    description TEXT,
                    due_date TIMESTAMP,
                    status VARCHAR(50) NOT NULL,
                    created_by VARCHAR(36) NOT NULL,
                    household_id VARCHAR(36) NOT NULL
                )
            """)
        }
    }
}
```

## Docker Configuration

### Dockerfile

```dockerfile
FROM openjdk:17-jre-slim

WORKDIR /app

# Copy application
COPY build/libs/ohana-backend-*.jar app.jar

# Set environment variables
ENV PORT=8080
ENV LOG_LEVEL=INFO

# Expose port
EXPOSE 8080

# Run application
CMD ["java", "-jar", "app.jar"]
```

### Docker Compose

```yaml
# docker-compose.yml
version: "3.8"

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - DB_HOST=db
      - DB_PORT=3306
      - DB_NAME=ohana
      - DB_USER=ohana_user
      - DB_PASSWORD=ohana_password
      - JWT_SECRET=your-jwt-secret
      - PORT=8080
      - RATE_LIMIT=100
      - RATE_LIMIT_REFILL_PERIOD_SECONDS=60
    depends_on:
      - db

  db:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=root
      - MYSQL_DATABASE=ohana
      - MYSQL_USER=ohana_user
      - MYSQL_PASSWORD=ohana_password
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

volumes:
  mysql_data:
```

## Configuration Best Practices

### 1. Environment-Specific Files

```bash
# Use different .env files for different environments
.env.development
.env.staging
.env.production
```

### 2. Secure Defaults

```kotlin
// ✅ Good - Secure defaults
val jwtSecret = System.getenv("JWT_SECRET")
    ?: throw IllegalStateException("JWT_SECRET is required")

// ❌ Bad - Insecure defaults
val jwtSecret = System.getenv("JWT_SECRET") ?: "default-secret"
```

### 3. Configuration Validation

```kotlin
fun validateConfiguration() {
    val requiredVars = listOf("JWT_SECRET", "DB_PASSWORD")
    val missing = requiredVars.filter { System.getenv(it) == null }

    if (missing.isNotEmpty()) {
        throw IllegalStateException("Missing required environment variables: ${missing.joinToString(", ")}")
    }
}
```

### 4. Configuration Documentation

```kotlin
/**
 * Application configuration loaded from environment variables.
 *
 * Required environment variables:
 * - JWT_SECRET: Secret key for JWT token signing
 * - DB_PASSWORD: Database password
 *
 * Optional environment variables:
 * - PORT: Server port (default: 4242)
 * - LOG_LEVEL: Logging level (default: INFO)
 * - DB_HOST: Database host (default: localhost)
 */
data class AppConfig(
    val port: Int,
    val jwtSecret: String,
    val logLevel: String,
)
```

### 5. Configuration Testing

```kotlin
@Test
fun `should load configuration from environment`() {
    // Set environment variables
    System.setProperty("JWT_SECRET", "test-secret")
    System.setProperty("PORT", "8080")

    val config = AppConfig.fromEnvironment()

    assertEquals("test-secret", config.jwtSecret)
    assertEquals(8080, config.port)
}
```

## Summary

The configuration management provides:

- **Environment-based** configuration
- **Secure** handling of sensitive data
- **Flexible** deployment options
- **Comprehensive** logging configuration
- **Docker** support
- **Testing** configuration
- **Validation** of configuration values

This approach ensures that the application can be easily deployed to different environments while maintaining security and flexibility.
