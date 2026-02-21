# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY common ./common
COPY auth-service ./auth-service
COPY task-service ./task-service
COPY reporting-service ./reporting-service

RUN mvn -pl reporting-service -am -DskipTests package

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=prod

COPY --from=build /workspace/reporting-service/target/reporting-service-*.jar /app/app.jar

EXPOSE 8083
ENTRYPOINT ["java","-jar","/app/app.jar"]
