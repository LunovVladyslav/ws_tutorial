# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/p2p_server-0.0.1-SNAPSHOT.jar app.jar

# Render assigns a dynamic port via the PORT env variable
ENV PORT=8080
EXPOSE $PORT

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
