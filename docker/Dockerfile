# ============================================
# VulnBank — Dockerfile
# ⚠️  FOR EDUCATIONAL USE ONLY
# ============================================

# Build stage — official Maven image already includes JDK 17
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy pom first — lets Docker cache the dependency layer separately
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -q

# Runtime stage — slim JRE only
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]