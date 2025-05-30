# Use a slim OpenJDK 17 base image for a smaller footprint
FROM openjdk:17-jdk-slim

# Install curl for health checks (not included in slim images)
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create a non-root user for security
RUN useradd -m appuser

# Set working directory
WORKDIR /app

# Copy the built JAR file
COPY target/user-management-service-1.0.0.jar app.jar

# Expose the port specified in the application.properties (default 8085)
EXPOSE ${SERVER_PORT:-8085}

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=docker
ENV KEYCLOAK_REALM=Tunisys
ENV KEYCLOAK_RESOURCE=demo-rest-api
ENV KEYCLOAK_CREDENTIALS_SECRET=6ZWEg9yWkbERGGJK0ju0AKaqgTn7S0wI

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${SERVER_PORT:-8085}/actuator/health || exit 1

# Run as the non-root user
USER appuser

# Specify the command to run the application with optimized JVM options
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]