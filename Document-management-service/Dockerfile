# Dockerfile for document-management-service - CORRECTED AND SECURED
FROM openjdk:17-jdk-slim

# *** THE FIX IS THIS LINE ***
# Apply security patches to the base OS to fix vulnerabilities.
RUN apt-get update && apt-get upgrade -y && rm -rf /var/lib/apt/lists/*

# Create a non-root user and group for security
RUN addgroup --system appgroup && adduser --system --group appuser

# Set the working directory
WORKDIR /app

# Copy the application JAR file into the container
COPY target/document-administratif-service-0.0.1-SNAPSHOT.jar app.jar

# Create the directory for document storage
RUN mkdir -p /app/documents

# Change ownership of the entire app directory to the non-root user
RUN chown -R appuser:appgroup /app

# Switch to the non-root user
USER appuser

# Define the entry point to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]