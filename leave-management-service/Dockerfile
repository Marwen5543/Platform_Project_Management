# Dockerfile - CORRECTED AND CLEANED
# Stage 1: Use a standard base image
FROM openjdk:17-jdk-slim

# Create a non-root user and group for security
RUN addgroup --system appgroup && adduser --system --group appuser

# Set the working directory
WORKDIR /app

# Copy the application JAR file into the container
# This assumes your build process creates a JAR in the 'target' directory
COPY target/leave-management-service-*.jar app.jar

# Change ownership of the app directory to the non-root user
RUN chown -R appuser:appgroup /app

# Switch to the non-root user
USER appuser

# Define the entry point to run the application
# All configuration will be passed in as environment variables by Kubernetes
ENTRYPOINT ["java", "-jar", "app.jar"]