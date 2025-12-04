# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:21-jdk-alpine AS build

ARG MODULE=inventory-service

WORKDIR /workspace
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY common common
COPY ${MODULE} ${MODULE}

RUN chmod +x mvnw
RUN ./mvnw -f pom.xml -N install -DskipTests
RUN ./mvnw -f common/pom.xml install -DskipTests
RUN ./mvnw -f ${MODULE}/pom.xml package -DskipTests

RUN BOOT_JAR=$(find ${MODULE}/target -maxdepth 1 -type f -name '*.jar' ! -name '*original*' -print -quit) && cp "${BOOT_JAR}" /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
