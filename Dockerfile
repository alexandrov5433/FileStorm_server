# ---- Build stage ----
FROM eclipse-temurin:24-jdk AS build

# Set working directory inside container
WORKDIR /app

# Copy Maven files first to cache dependencies
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw dependency:go-offline

# Copy the source code
COPY src ./src

# Build the Spring Boot app
RUN ./mvnw clean package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:24-jdk AS runtime

# Set working directory for runtime container
WORKDIR /app

# Copy the JAR from the build container
COPY --from=build /app/target/*.jar FileStorm_server.jar

# Expose the port your app runs on
EXPOSE 8080

# Set default command
ENTRYPOINT ["java", "-jar", "FileStorm_server.jar"]
