# ===============================
# Stage 1: Build the Spring Boot App
# ===============================
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# ===============================
# Stage 2: Run the Application
# ===============================
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy built JAR from previous stage
COPY --from=build /app/target/*.jar app.jar

# Copy keystore into container
COPY src/main/resources/keystore/streetmed-keystore.p12 /app/keystore/streetmed-keystore.p12

EXPOSE 8443

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=local"]
