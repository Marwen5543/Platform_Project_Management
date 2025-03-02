FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/performance-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8089
# Add health check for better Docker orchestration
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8089/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]