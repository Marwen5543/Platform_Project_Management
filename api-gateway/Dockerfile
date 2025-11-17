# Use a well-maintained base image from the Eclipse Foundation
FROM eclipse-temurin:17-jre-alpine

# Update the OS packages to the latest versions to fix vulnerabilities
# AND install curl for the healthcheck.
RUN apk update && apk upgrade && apk add curl

WORKDIR /app
COPY target/Gateway-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]