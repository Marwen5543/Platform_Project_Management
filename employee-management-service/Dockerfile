FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/employee-service-1.0.0.jar app.jar
EXPOSE 8086
# Add health check for better Docker orchestration
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8086/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]