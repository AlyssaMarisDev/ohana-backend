# Multi-stage build for smaller production image
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app
COPY . .
RUN gradle build --no-daemon

# Production stage
FROM openjdk:17-jdk-slim

# Create app user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app
COPY --from=builder /app/build/libs/ohana-backend-0.0.1.jar app.jar

# Change ownership to app user
RUN chown appuser:appuser app.jar

# Switch to app user
USER appuser

# Expose the port
EXPOSE 4242

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:4242/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]