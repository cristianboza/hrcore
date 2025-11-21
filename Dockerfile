FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy only the POM first to cache dependencies
COPY pom.xml ./

# Download dependencies (this layer is cached until pom.xml changes)
RUN mvn -B package -DskipTests -Dmaven.main.skip=true -Dmaven.test.skip=true || true

# Now copy source code and build
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/hrcore-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]