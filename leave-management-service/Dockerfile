# Dockerfile for leave-management-service - CORRECTED AND SECURED
FROM openjdk:17-jdk-slim

# ==============================================================================
# *** THE FIX IS HERE: Apply security patches to the base OS ***
# This will fix any potential OS vulnerabilities.
# ==============================================================================
RUN apt-get update && apt-get upgrade -y && rm -rf /var/lib/apt/lists/*

# Create a non-root user and group for security
RUN addgroup --system appgroup && adduser --system --group appuser

# Set the working directory
WORKDIR /app

# Copy the application JAR file into the container
COPY target/leave-management-service-*.jar app.jar

# Change ownership of the app directory to the non-root user
RUN chown -R appuser:appgroup /app
# *** NEW: Added Health Check for robustness ***
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8088/actuator/health || exit 1
# Switch to the non-root user
USER appuser

# Define the entry point to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]