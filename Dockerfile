# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:21-jdk-alpine AS build

ARG MODULE

WORKDIR /workspace

# Copy Maven wrapper
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw

# Step 1: Copy only POMs first for dependency resolution
COPY pom.xml .

# Step 2: Install parent POM only (non-recursive)
RUN --mount=type=cache,target=/root/.m2/repository \
    ./mvnw -f pom.xml -N install -DskipTests

COPY common/pom.xml common/pom.xml

# Step 3: Download dependencies for common
RUN --mount=type=cache,target=/root/.m2/repository \
    ./mvnw -f common/pom.xml dependency:go-offline

# 1) Copy common sources and build+install common
COPY common/src common/src


RUN --mount=type=cache,target=/root/.m2/repository \
    ./mvnw -f common/pom.xml install -DskipTests

# 2) Copy module sources and pom
COPY ${MODULE}/pom.xml ${MODULE}/pom.xml
COPY ${MODULE}/src ${MODULE}/src

# Step 4: Now that common is installed, go-offline for the module
RUN --mount=type=cache,target=/root/.m2/repository \
    ./mvnw -f ${MODULE}/pom.xml dependency:go-offline

# Step 5: Build only the module
RUN --mount=type=cache,target=/root/.m2/repository \
    ./mvnw -f ${MODULE}/pom.xml package -DskipTests


# Extract JAR
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
