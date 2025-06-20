# Use a slim OpenJDK 17 base image
FROM openjdk:17-jdk-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create a non-root user
RUN useradd -m appuser

# Set working directory
WORKDIR /app

# Copy the built JAR file
COPY target/leave-management-service-0.0.1-SNAPSHOT.jar app.jar

# Create directory for document storage
RUN mkdir -p /app/documents && chown -R appuser:appuser /app/documents

# Expose the port
EXPOSE ${SERVER_PORT:-8088}

# Set environment variables for Docker
ENV KEYCLOAK_AUTH_SERVER_URL=http://keycloak:8080
ENV KEYCLOAK_REALM=Tunisys
ENV KEYCLOAK_RESOURCE=demo-rest-api
ENV KEYCLOAK_CREDENTIALS_SECRET=6ZWEg9yWkbERGGJK0ju0AKaqgTn7S0wI
ENV EUREKA_SERVER_URL=http://eureka:8761/eureka/

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${SERVER_PORT:-8088}/actuator/health || exit 1

# Run as non-root user
USER appuser

# Run the application
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]