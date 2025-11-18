FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the JAR file
COPY --from=build /app/target/*.jar /app/shiina.jar

# Copy required directories and files
COPY static /app/static
COPY templates /app/templates

# Create directories for volumes
RUN mkdir -p /app/logs
RUN mkdir -p /app/plugins
RUN mkdir -p /app/data
RUN mkdir -p /app/.cache

ENTRYPOINT ["java", "-jar", "/app/shiina.jar"]