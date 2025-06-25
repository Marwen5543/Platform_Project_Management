# Dockerfile for document-management-service - CORRECTED AND CLEANED
# Stage 1: Use a standard base image
FROM openjdk:17-jdk-slim

# Create a non-root user and group for security
RUN addgroup --system appgroup && adduser --system --group appuser

# Set the working directory
WORKDIR /app

# Copy the application JAR file into the container
# This line is specific to this service
COPY target/document-administratif-service-0.0.1-SNAPSHOT.jar app.jar

# Create the directory for document storage as you had before
RUN mkdir -p /app/documents

# Change ownership of the entire app directory to the non-root user
# This includes the JAR file and the documents directory
RUN chown -R appuser:appgroup /app

# Switch to the non-root user
USER appuser

# Define the entry point to run the application
# All configuration will be passed in as environment variables by Kubernetes
ENTRYPOINT ["java", "-jar", "app.jar"]