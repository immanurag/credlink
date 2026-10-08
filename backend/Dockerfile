# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and resolve dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and package application JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Run stage ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy built Spring Boot executable JAR
COPY --from=build /app/target/*.jar app.jar

# Expose default port (Render overrides PORT environment variable at runtime)
EXPOSE 8080

# Execute Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
