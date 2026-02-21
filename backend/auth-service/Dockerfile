# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy module sources and shared code
COPY pom.xml ./
COPY common ./common
COPY auth-service ./auth-service
COPY task-service ./task-service
COPY reporting-service ./reporting-service

# Build only the auth-service module (and its dependencies)
RUN mvn -pl auth-service -am -DskipTests package

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=prod

# Copy the packaged jar
COPY --from=build /workspace/auth-service/target/auth-service-*.jar /app/app.jar

EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
