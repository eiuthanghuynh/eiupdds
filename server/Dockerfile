# ==============================================================================
# Stage 1: Build stage using Eclipse Temurin JDK 21 on Alpine Linux
# ==============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Install Maven and curl for dependable, fast container packaging
RUN apk add --no-cache maven curl

# Copy POM and application source code
COPY pom.xml ./
COPY src/ ./src/

# Compile and package using BuildKit Maven cache
# ponytail: avoid slow mvn dependency:go-offline pulling unused test/optional plugins
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B && \
    cp target/pdds-server-*.jar /build/app.jar

# ==============================================================================
# Stage 2: Hardened Runtime stage using Eclipse Temurin JRE 21 on Alpine Linux
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS runner

# Install curl for robust healthcheck probes
RUN apk add --no-cache curl

# Security hardening: Create dedicated non-root application user and group
RUN addgroup -g 1001 -S pdds && \
    adduser -S pdds -u 1001 -G pdds

WORKDIR /app

# Copy executable jar from builder stage with proper ownership
COPY --from=builder --chown=pdds:pdds /build/app.jar /app/app.jar

# Switch to non-root user
USER pdds:pdds

# Expose HTTP port
EXPOSE 8080

# Configure JVM container ergonomics and production profile
ENV SPRING_PROFILES_ACTIVE=docker \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Container healthcheck targeting Spring Boot Actuator liveness probe
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
