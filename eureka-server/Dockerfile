# Use a slim OpenJDK 17 base image for a smaller footprint
FROM openjdk:17-jdk-slim

# Install curl for health checks (not included in slim images)
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy the built JAR file
COPY target/eureka-server-0.0.1-SNAPSHOT.jar app.jar

# Expose the port specified in the application.properties (default 8761)
EXPOSE ${SERVER_PORT:-8761}

# Add health check for Docker orchestration, using the actuator health endpoint
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${SERVER_PORT:-8761}/actuator/health || exit 1

# Run as a non-root user for security
RUN useradd -m appuser
USER appuser

# Allow environment variable override for JVM options
ENTRYPOINT ["java", "-jar", "app.jar"]