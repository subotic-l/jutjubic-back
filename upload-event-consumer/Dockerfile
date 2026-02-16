# Multi-stage Docker build for upload-event-consumer
# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Kopiraj common i upload-event-consumer module
COPY common ./common
COPY upload-event-consumer/pom.xml ./upload-event-consumer/
COPY upload-event-consumer/src ./upload-event-consumer/src

# Kreiraj privremeni parent pom.xml samo za consumer build (bez jutjubic modula)
RUN echo '<?xml version="1.0" encoding="UTF-8"?>' > pom.xml && \
    echo '<project xmlns="http://maven.apache.org/POM/4.0.0"' >> pom.xml && \
    echo '         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"' >> pom.xml && \
    echo '         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">' >> pom.xml && \
    echo '    <modelVersion>4.0.0</modelVersion>' >> pom.xml && \
    echo '    <parent>' >> pom.xml && \
    echo '        <groupId>org.springframework.boot</groupId>' >> pom.xml && \
    echo '        <artifactId>spring-boot-starter-parent</artifactId>' >> pom.xml && \
    echo '        <version>3.2.2</version>' >> pom.xml && \
    echo '        <relativePath/>' >> pom.xml && \
    echo '    </parent>' >> pom.xml && \
    echo '    <groupId>com.example</groupId>' >> pom.xml && \
    echo '    <artifactId>jutjubic-parent</artifactId>' >> pom.xml && \
    echo '    <version>0.0.1-SNAPSHOT</version>' >> pom.xml && \
    echo '    <packaging>pom</packaging>' >> pom.xml && \
    echo '    <properties>' >> pom.xml && \
    echo '        <java.version>17</java.version>' >> pom.xml && \
    echo '        <protobuf.version>3.25.3</protobuf.version>' >> pom.xml && \
    echo '        <lombok.version>1.18.30</lombok.version>' >> pom.xml && \
    echo '    </properties>' >> pom.xml && \
    echo '    <modules>' >> pom.xml && \
    echo '        <module>common</module>' >> pom.xml && \
    echo '        <module>upload-event-consumer</module>' >> pom.xml && \
    echo '    </modules>' >> pom.xml && \
    echo '</project>' >> pom.xml

# Build common i upload-event-consumer module
RUN mvn clean install -DskipTests -B -pl common,upload-event-consumer -am

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install curl for health check
RUN apk add --no-cache curl

# Copy JAR from build stage
COPY --from=build /app/upload-event-consumer/target/upload-event-consumer-*.jar app.jar

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Start application
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]
