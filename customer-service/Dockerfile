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
COPY --from=builder /app/target/customer-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
