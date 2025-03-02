FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/user-management-service-1.0.0.jar app.jar
EXPOSE 8085
# Optional: Add health check (requires actuator endpoints to be accessible)
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8085/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]