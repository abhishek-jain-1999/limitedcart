# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:21-jdk-alpine AS build

ARG MODULE=auth-service

WORKDIR /workspace

# Copy Maven wrapper first (rarely changes)
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw

# Copy only POMs first for dependency resolution (better caching)
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY ${MODULE}/pom.xml ${MODULE}/pom.xml

# Download dependencies with BuildKit cache mount
RUN --mount=type=cache,target=/root/.m2/repository \
    ./mvnw -B dependency:go-offline -pl common,${MODULE} -am

# Now copy source files
COPY common/src common/src
COPY ${MODULE}/src ${MODULE}/src

# Build with cached dependencies
RUN --mount=type=cache,target=/root/.m2/repository \
    ./mvnw -B -pl common,${MODULE} package -DskipTests -am

# Extract the JAR
RUN BOOT_JAR=$(find ${MODULE}/target -maxdepth 1 -type f -name '*.jar' ! -name '*original*' -print -quit) && \
    cp "${BOOT_JAR}" /workspace/app.jar

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /workspace/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
