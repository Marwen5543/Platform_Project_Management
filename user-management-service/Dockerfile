# Use a slim OpenJDK 17 base image for a smaller footprint
FROM eclipse-temurin:17-jdk-jammy
# More reliable than openjdk:17-jdk-slim

# Install curl for health checks (remove after if not needed)
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

# Create a non-root user and group for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy the JAR file (use a consistent name)
# --- THIS IS THE CORRECTED LINE ---
#COPY target/user-management-service-*.jar app.jar
# Copy the JAR file - Updated path for microservice structure
COPY user-management-service/target/user-management-service-*.jar app.jar

# Change ownership to appuser
RUN chown appuser:appuser app.jar

# Expose the port (best practice, but doesn't actually publish it)
EXPOSE 8085

# Set environment variables (use default values for flexibility)
ENV SPRING_PROFILES_ACTIVE=docker \
    SERVER_PORT=8085 \
    KEYCLOAK_REALM=Tunisys \
    KEYCLOAK_RESOURCE=demo-rest-api \
    KEYCLOAK_CREDENTIALS_SECRET=6ZWEg9yWkbERGGJK0ju0AKaqgTn7S0wI

# Health check (ensure Keycloak is reachable)
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${SERVER_PORT:-8085}/actuator/health || exit 1

# Run as non-root user
USER appuser

# Optimized JVM startup
#ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
ENTRYPOINT ["java", "-jar", "app.jar"]