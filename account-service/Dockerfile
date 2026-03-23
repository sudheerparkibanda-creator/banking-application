# Multi-stage build
FROM openjdk:17-jdk-slim AS builder
WORKDIR /build
COPY target/account-service-0.0.1-SNAPSHOT.jar app.jar
RUN jar xf app.jar

# Runtime stage
FROM openjdk:17-jdk-slim
WORKDIR /app

# Create non-root user
RUN useradd -m -u 1000 appuser

# Copy from builder
COPY --from=builder --chown=appuser:appuser /build/BOOT-INF/lib /app/lib
COPY --from=builder --chown=appuser:appuser /build/META-INF /app/META-INF
COPY --from=builder --chown=appuser:appuser /build/BOOT-INF/classes /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD java -cp /app:/app/lib/* -c "import java.net.*; new URL(\"http://localhost:8082/actuator/health\").openConnection().getResponseCode() == 200" || exit 1

# Run application with JVM flags for Spring Boot
ENTRYPOINT ["java", "-cp", "/app:/app/lib/*", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "org.springframework.boot.loader.JarLauncher"]
