# ==============================================================================
# Stage 1: Build stage using Eclipse Temurin JDK 21 on Alpine Linux
# ==============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Maven Wrapper and POM first to cache runtime/compile dependencies in CI layers
COPY .mvn/ ./.mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:resolve -DincludeScope=runtime -B

# Copy application source code
COPY src/ ./src/

# Package and extract Spring Boot layers for incremental runtime image caching
RUN ./mvnw clean package -DskipTests -B && \
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
