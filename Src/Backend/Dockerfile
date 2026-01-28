# ===============================
# Cloud Run Production Dockerfile
# ===============================
# Cloud Run handles TLS termination

# Stage 1: Build the Spring Boot App
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the Application
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy built JAR from previous stage
COPY --from=build /app/target/*.jar app.jar

# Cloud Run expects port 8080
EXPOSE 8080

# Cloud Run will set PORT environment variable
ENV PORT=8080

# Use production profile and bind to all interfaces
ENTRYPOINT ["sh", "-c", "exec java -Dserver.port=${PORT} -Dserver.address=0.0.0.0 -Dspring.profiles.active=prod -jar /app/app.jar"]