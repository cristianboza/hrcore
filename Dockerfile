# Multi-stage Dockerfile: build with Maven, run with a lightweight JRE
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy only what's needed to build and take advantage of docker layer caching
COPY pom.xml ./
COPY src ./src

# Build the project (skip tests for faster builds by default)
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /workspace/target/hrcore-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
