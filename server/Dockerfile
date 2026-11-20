# ==============================================================================
# Stage 1: Build stage using official Maven 3.9.9 + Eclipse Temurin JDK 21 Alpine
# ==============================================================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy POM first to cache runtime dependencies in CI layers without downloading Maven binary
COPY pom.xml ./
RUN for attempt in 1 2 3 4; do \
        mvn dependency:resolve -DincludeScope=runtime -B && break || { \
            if [ "$attempt" -eq 4 ]; then exit 1; fi; \
            backoff=$((attempt * 10)); \
            echo "Maven dependency resolve attempt $attempt failed; retrying in ${backoff}s..."; \
            sleep "$backoff"; \
        }; \
    done

# Copy application source code
COPY src/ ./src/

# Package and extract Spring Boot layers for incremental runtime image caching
RUN for attempt in 1 2 3; do \
        mvn clean package -DskipTests -B && break || { \
            if [ "$attempt" -eq 3 ]; then exit 1; fi; \
            backoff=$((attempt * 10)); \
            echo "Maven package attempt $attempt failed; retrying in ${backoff}s..."; \
            sleep "$backoff"; \
        }; \
    done && \
    java -Djarmode=tools -jar target/pdds-server-*.jar extract --layers --launcher --destination /build/extracted

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

# Copy extracted Spring Boot layers in order of change frequency
COPY --from=builder --chown=pdds:pdds /build/extracted/dependencies/ ./
COPY --from=builder --chown=pdds:pdds /build/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=pdds:pdds /build/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=pdds:pdds /build/extracted/application/ ./

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

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
