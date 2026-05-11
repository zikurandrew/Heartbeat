FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY common/pom.xml common/
COPY client/pom.xml client/
COPY server/pom.xml server/

COPY common/src common/src
COPY server/src server/src

RUN mvn clean package -pl common,server -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN mkdir -p /data

COPY --from=build /app/server/target/*.jar app.jar

EXPOSE 5000

CMD ["java", "-jar", "app.jar"]