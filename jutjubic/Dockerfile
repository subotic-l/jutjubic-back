# Multi-stage Docker build za Spring Boot aplikaciju
# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Kopiraj parent pom.xml i sve module
COPY pom.xml .
COPY common ./common
COPY jutjubic/pom.xml ./jutjubic/
COPY jutjubic/src ./jutjubic/src
COPY upload-event-consumer/pom.xml ./upload-event-consumer/
COPY upload-event-consumer/src ./upload-event-consumer/src

# Build samo common i jutjubic module
RUN mvn clean install -DskipTests -B -pl common,jutjubic

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Instaliraj curl za health check
RUN apk add --no-cache curl

# Kopiraj JAR iz build stage-a (sada je u jutjubic/target subfolderu)
COPY --from=build /app/jutjubic/target/jutjubic-*.jar app.jar

# Kreiraj folder za uploads (shared volume)
RUN mkdir -p /app/uploads/videos /app/uploads/thumbnails

# Expose port (environment variable)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Start aplikacije
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]
