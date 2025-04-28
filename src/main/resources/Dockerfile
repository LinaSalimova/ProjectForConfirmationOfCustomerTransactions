FROM maven:3.8.6-openjdk-11 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

FROM openjdk:11-jre-slim
COPY --from=build /app/target/*-jar-with-dependencies.jar /app/otp-service.jar
CMD ["java", "-jar", "/app/otp-service.jar"]
