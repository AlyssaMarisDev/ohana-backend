# Configuration Guide

## Environment Variables

The Ohana backend uses environment variables for configuration. The configuration is loaded through the `AppConfig` class and its nested configuration classes.

### Server Configuration

| Variable      | Default       | Description                                    |
| ------------- | ------------- | ---------------------------------------------- |
| `PORT`        | `4242`        | Server port                                    |
| `HOST`        | `0.0.0.0`     | Server host                                    |
| `ENVIRONMENT` | `development` | Environment (development, staging, production) |

### Database Configuration

| Variable       | Default     | Description          |
| -------------- | ----------- | -------------------- |
| `DB_HOST`      | `localhost` | Database host        |
| `DB_PORT`      | `3306`      | Database port        |
| `DB_NAME`      | `ohana`     | Database name        |
| `DB_USER`      | `root`      | Database username    |
| `DB_PASSWORD`  | `root`      | Database password    |
| `DB_POOL_SIZE` | `10`        | Connection pool size |

### JWT Configuration

| Variable               | Default                                | Description               |
| ---------------------- | -------------------------------------- | ------------------------- |
| `JWT_SECRET`           | `your-secret-key-change-in-production` | JWT signing secret        |
| `JWT_ISSUER`           | `ohana-backend`                        | JWT issuer                |
| `JWT_AUDIENCE`         | `ohana-users`                          | JWT audience              |
| `JWT_EXPIRATION_HOURS` | `24`                                   | Token expiration in hours |

### Rate Limiting Configuration

| Variable                         | Default | Description                            |
| -------------------------------- | ------- | -------------------------------------- |
| `RATE_LIMIT_REQUESTS_PER_MINUTE` | `60`    | Maximum requests per minute per client |
| `RATE_LIMIT_BURST_CAPACITY`      | `10`    | Burst capacity for rate limiting       |

## Configuration Structure

The configuration is organized into nested data classes:

```kotlin
data class AppConfig(
    val server: ServerConfig,
    val database: DatabaseConfig,
    val jwt: JwtConfig,
    val rateLimit: RateLimitConfig,
)
```

### ServerConfig

```kotlin
data class ServerConfig(
    val port: Int,
    val host: String,
    val environment: String,
)
```

### DatabaseConfig

```kotlin
data class DatabaseConfig(
    val host: String,
    val port: String,
    val name: String,
    val user: String,
    val password: String,
    val connectionPoolSize: Int,
)
```

### JwtConfig

```kotlin
data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val expirationHours: Long,
)
```

### RateLimitConfig

```kotlin
data class RateLimitConfig(
    val requestsPerMinute: Int,
    val burstCapacity: Int,
)
```

## Example .env File

Create a `.env` file in your project root with the following content:

```bash
# Server Configuration
PORT=4242
HOST=0.0.0.0
ENVIRONMENT=development

# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=ohana
DB_USER=root
DB_PASSWORD=root
DB_POOL_SIZE=10

# JWT Configuration
JWT_SECRET=your-secret-key-change-in-production
JWT_ISSUER=ohana-backend
JWT_AUDIENCE=ohana-users
JWT_EXPIRATION_HOURS=24

# Rate Limiting Configuration
RATE_LIMIT_REQUESTS_PER_MINUTE=60
RATE_LIMIT_BURST_CAPACITY=10
```

## CORS Configuration

CORS is configured with hardcoded settings in `ConfigureCORS.kt`:

```kotlin
fun Application.configureCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)
        anyHost()
    }
}
```

**Note**: CORS is currently configured to allow any host (`anyHost()`). For production, you should modify this to restrict to specific domains.

## Production Configuration

For production, make sure to:

1. **Set a strong `JWT_SECRET`**: Use a cryptographically secure random string
2. **Use proper database credentials**: Never use default credentials in production
3. **Configure CORS properly**: Modify `ConfigureCORS.kt` to restrict allowed origins
4. **Set appropriate rate limits**: Adjust based on your expected traffic
5. **Use environment-specific database settings**: Use production database credentials
6. **Set `ENVIRONMENT=production`**: This may affect logging and error handling

## Configuration Usage

The configuration is automatically loaded when the application starts using `AppConfig.fromEnvironment()`. You can access it in your code through dependency injection:

```kotlin
// In your handler or service
class SomeHandler(private val config: AppConfig) {
    fun handle() {
        val dbHost = config.database.host
        val jwtSecret = config.jwt.secret
        val serverPort = config.server.port
        val rateLimit = config.rateLimit.requestsPerMinute
    }
}
```

### Accessing Specific Configuration Sections

```kotlin
// Server configuration
val port = config.server.port
val host = config.server.host
val environment = config.server.environment

// Database configuration
val dbHost = config.database.host
val dbName = config.database.name
val poolSize = config.database.connectionPoolSize

// JWT configuration
val secret = config.jwt.secret
val issuer = config.jwt.issuer
val expiration = config.jwt.expirationHours

// Rate limiting configuration
val requestsPerMinute = config.rateLimit.requestsPerMinute
val burstCapacity = config.rateLimit.burstCapacity
```

## Configuration Loading

The configuration is loaded in the main application entry point:

```kotlin
fun main() {
    val config = AppConfig.fromEnvironment()

    embeddedServer(
        Netty,
        port = config.server.port,
        host = config.server.host,
        module = Application::module,
    ).start(wait = true)
}
```

Each configuration section is loaded independently using its own `fromEnvironment()` method, making the configuration modular and easy to extend.
