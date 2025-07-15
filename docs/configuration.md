# Configuration Guide

## Environment Variables

The Ohana backend uses environment variables for configuration. Here are all the available options:

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

### CORS Configuration

| Variable               | Default                                       | Description                                  |
| ---------------------- | --------------------------------------------- | -------------------------------------------- |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:8080` | Comma-separated list of allowed origins      |
| `CORS_ALLOWED_METHODS` | `GET,POST,PUT,DELETE,OPTIONS`                 | Comma-separated list of allowed HTTP methods |
| `CORS_ALLOWED_HEADERS` | `Content-Type,Authorization`                  | Comma-separated list of allowed headers      |

### Rate Limiting Configuration

| Variable                         | Default | Description                            |
| -------------------------------- | ------- | -------------------------------------- |
| `RATE_LIMIT_REQUESTS_PER_MINUTE` | `60`    | Maximum requests per minute per client |
| `RATE_LIMIT_BURST_CAPACITY`      | `10`    | Burst capacity for rate limiting       |

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

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080
CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,OPTIONS
CORS_ALLOWED_HEADERS=Content-Type,Authorization

# Rate Limiting Configuration
RATE_LIMIT_REQUESTS_PER_MINUTE=60
RATE_LIMIT_BURST_CAPACITY=10
```

## Production Configuration

For production, make sure to:

1. Set a strong `JWT_SECRET`
2. Use proper database credentials
3. Configure CORS for your frontend domain
4. Set appropriate rate limits
5. Use environment-specific database settings

## Configuration Usage

The configuration is automatically loaded when the application starts. You can access it in your code through dependency injection:

```kotlin
class SomeHandler(private val config: AppConfig) {
    fun handle() {
        val dbHost = config.database.host
        val jwtSecret = config.jwt.secret
    }
}
```
