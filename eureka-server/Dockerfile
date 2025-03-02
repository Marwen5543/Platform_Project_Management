FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/eureka-server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8761
# Add health check for better Docker orchestration
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8761/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]