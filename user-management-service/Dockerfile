# Use a secure and reliable OpenJDK 17 base image from the Eclipse Foundation
FROM eclipse-temurin:17-jdk-jammy

# ==============================================================================
# Best Practice: Apply security patches, install tools, and clean up in one layer.
# 1. apt-get update      - Refresh package lists
# 2. apt-get upgrade -y   - Apply all available OS and security patches
# 3. apt-get install -y  - Install 'curl' needed for the HEALTHCHECK
# 4. rm -rf /var/lib...  - Clean up the apt cache to keep the final image small
# ==============================================================================
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# ==============================================================================
# Best Practice: Run the application as a non-root user to limit potential damage
# in case of a container compromise.
# ==============================================================================
RUN groupadd -r appuser && useradd --no-log-init -r -g appuser appuser

# Set the working directory for the application
WORKDIR /app

# ==============================================================================
# Copy the application JAR into the container.
# The wildcard (*) is used to match the versioned JAR file produced by Maven.
# This path assumes the Docker build context is the root of the microservice
# (e.g., 'user-management-service/'), which is configured in your .gitlab-ci.yml.
# ==============================================================================
COPY target/user-management-service-*.jar app.jar

# Change ownership of the application file to the non-root user
RUN chown appuser:appuser app.jar

# Expose the port the application will run on. This is documentation for users.
EXPOSE 8085

# Set default environment variables. These can be overridden at runtime.
ENV SPRING_PROFILES_ACTIVE=docker \
    SERVER_PORT=8085 \
    KEYCLOAK_REALM=Tunisys \
    KEYCLOAK_RESOURCE=demo-rest-api \
    KEYCLOAK_CREDENTIALS_SECRET=6ZWEg9yWkbERGGJK0ju0AKaqgTn7S0wI

# ==============================================================================
# Best Practice: A health check tells the container orchestrator (like Docker
# or Kubernetes) if the application is running correctly.
# ==============================================================================
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${SERVER_PORT:-8085}/actuator/health || exit 1

# Switch to the non-root user before running the application
USER appuser

# ==============================================================================
# The command to start the application.
# For production, consider using the optimized JVM flags to control memory usage.
# ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
# ==============================================================================
ENTRYPOINT ["java", "-jar", "app.jar"]