FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/department-management-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8087
# Add health check for better Docker orchestration
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8087/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]