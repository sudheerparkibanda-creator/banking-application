# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src/ src/
RUN mvn package -DskipTests -B

# ---- Run Stage ----
FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=builder /app/target/service-registry-0.0.1-SNAPSHOT.jar app.jar
COPY docker-entrypoint.sh docker-entrypoint.sh
RUN chmod +x docker-entrypoint.sh
ENV CONFIG_SERVER_URL=http://config-server:8888
EXPOSE 8761
ENTRYPOINT ["/app/docker-entrypoint.sh"]
