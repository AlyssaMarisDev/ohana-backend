# Refresh Token Implementation

## Overview

The Ohana backend now supports refresh tokens for enhanced security and better user experience. This implementation follows industry best practices for token-based authentication.

## Architecture

### Token Types

1. **Access Token**: Short-lived (1 hour by default) for API calls
2. **Refresh Token**: Long-lived (30 days by default) for obtaining new access tokens

### Security Features

- **Token Rotation**: Refresh tokens are rotated on each use
- **Token Revocation**: Refresh tokens can be revoked for logout
- **Database Tracking**: All refresh tokens are stored in the database for security
- **Separate Secrets**: Access and refresh tokens use different signing secrets

## API Endpoints

### Authentication Endpoints

#### POST `/api/v1/register`

Registers a new user and returns both access and refresh tokens.

**Request:**

```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

**Response:**

```json
{
  "id": "user-uuid",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### POST `/api/v1/login`

Authenticates a user and returns both access and refresh tokens.

**Request:**

```json
{
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

**Response:**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### POST `/api/v1/refresh`

Refreshes an access token using a valid refresh token.

**Request:**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### POST `/api/v1/logout`

Revokes a refresh token, effectively logging out the user.

**Request:**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:**

```json
{
  "message": "Successfully logged out"
}
```

## Configuration

### Environment Variables

```bash
# JWT Configuration
JWT_SECRET=your-secret-key-change-in-production
JWT_REFRESH_SECRET=your-refresh-secret-key-change-in-production
JWT_ISSUER=ohana-backend
JWT_AUDIENCE=ohana-users
JWT_EXPIRATION_HOURS=1
JWT_REFRESH_EXPIRATION_DAYS=30
```

### Database Schema

The `refresh_tokens` table stores all refresh tokens:

```sql
CREATE TABLE `refresh_tokens` (
  `id` char(36) NOT NULL COMMENT 'UUID for refresh token record',
  `token` text NOT NULL COMMENT 'The refresh token value',
  `user_id` char(36) NOT NULL COMMENT 'ID of the user this token belongs to',
  `expires_at` timestamp NOT NULL COMMENT 'When the refresh token expires',
  `is_revoked` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Whether the token has been revoked',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
  `revoked_at` timestamp NULL DEFAULT NULL COMMENT 'When the token was revoked',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`token`(255)),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_expires_at` (`expires_at`),
  KEY `idx_is_revoked` (`is_revoked`),
  KEY `idx_user_active_tokens` (`user_id`, `is_revoked`),
  CONSTRAINT `fk_refresh_tokens_user_id` FOREIGN KEY (`user_id`) REFERENCES `members` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Stores refresh tokens for authentication';
```

## Implementation Details

### Token Generation

```kotlin
// Generate both tokens
val tokenPair = JwtManager.generateTokenPair(userId)

// Or generate individually
val accessToken = JwtManager.generateAccessToken(userId)
val refreshToken = JwtManager.generateRefreshToken(userId)
```

### Token Validation

```kotlin
// Validate access token
val decodedAccessToken = JwtManager.validateAccessToken(token)

// Validate refresh token
val decodedRefreshToken = JwtManager.validateRefreshToken(token)

// Extract user ID
val userId = JwtManager.getUserIdFromToken(accessToken)
val userId = JwtManager.getUserIdFromRefreshToken(refreshToken)
```

### Database Operations

```kotlin
// Store refresh token
val refreshToken = RefreshToken(
    id = UUID.randomUUID().toString(),
    token = tokenPair.refreshToken,
    userId = userId,
    expiresAt = Instant.now().plus(30, ChronoUnit.DAYS)
)
context.refreshTokens.create(refreshToken)

// Revoke token
context.refreshTokens.revokeToken(refreshToken)

// Find token
val storedToken = context.refreshTokens.findByToken(token)
```

## Security Considerations

### Token Rotation

- Refresh tokens are rotated on each use
- Old refresh tokens are immediately revoked
- New refresh tokens are generated for each refresh

### Token Revocation

- Refresh tokens can be revoked for logout
- Revoked tokens are marked in the database
- Expired tokens are automatically cleaned up

### Token Storage

- Refresh tokens are stored in the database
- Tokens are hashed and secured
- Database indexes optimize token lookups

### Token Validation

- Tokens are validated against the database
- Expired tokens are rejected
- Revoked tokens are rejected
- User ID mismatch is detected

## Client Implementation

### Mobile App Flow

1. **Login**: Store both access and refresh tokens securely
2. **API Calls**: Use access token for all API requests
3. **Token Expiry**: When access token expires, use refresh token to get new tokens
4. **Logout**: Send refresh token to logout endpoint to revoke it

### Example Client Code

```kotlin
class TokenManager {
    private var accessToken: String? = null
    private var refreshToken: String? = null

    suspend fun makeAuthenticatedRequest(request: Request): Response {
        try {
            return apiService.makeRequest(request, accessToken)
        } catch (e: UnauthorizedException) {
            // Token expired, refresh it
            refreshTokens()
            return apiService.makeRequest(request, accessToken)
        }
    }

    private suspend fun refreshTokens() {
        val response = apiService.refreshToken(refreshToken)
        accessToken = response.accessToken
        refreshToken = response.refreshToken
        saveTokensSecurely()
    }
}
```

## Testing

The implementation includes comprehensive tests for:

- Token generation and validation
- Token refresh flow
- Token revocation
- Error handling
- Security edge cases

Run tests with:

```bash
./gradlew test
```

## Best Practices

1. **Secure Storage**: Store refresh tokens securely (Keychain on iOS, Keystore on Android)
2. **Token Rotation**: Always use the new refresh token after refresh
3. **Error Handling**: Handle token expiry gracefully
4. **Logout**: Always revoke refresh tokens on logout
5. **Network Issues**: Implement retry logic for failed refresh attempts
6. **Monitoring**: Monitor token usage for suspicious patterns

## Migration from Old System

If migrating from a single-token system:

1. Update client to handle token pairs
2. Implement refresh token storage
3. Add refresh logic to API calls
4. Update logout to use refresh token
5. Test thoroughly with new token flow
