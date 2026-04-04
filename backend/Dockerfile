FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/finresearch-api-0.1.0-SNAPSHOT.jar app.jar
ENV DATA_DIR=/app/data
RUN mkdir -p /app/data
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
