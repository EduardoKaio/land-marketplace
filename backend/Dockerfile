# Build stage: full JDK and Maven, discarded after the jar is built
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Dependencies first: this layer is rebuilt only when pom.xml changes, not on every source edit
COPY pom.xml .
RUN mvn --batch-mode --quiet dependency:go-offline

COPY src ./src
# Tests run with ./mvnw verify: they need Docker (Testcontainers), which is not available inside an
# image build
RUN mvn --batch-mode --quiet package -DskipTests

# Runtime stage: JRE only. Alpine ships BusyBox wget, used by the Compose healthcheck
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/land-marketplace-api-*.jar app.jar
USER app

EXPOSE 8080
# Sizes the heap from the container memory limit instead of the host memory
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
