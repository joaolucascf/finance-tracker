# ---------- STAGE 1: BUILD ----------
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Cache de dependências
COPY pom.xml .
RUN mvn -B -q -e -DskipTests dependency:go-offline

# Copia código
COPY src ./src

# Build do jar
RUN mvn -B -q -DskipTests package

# ---------- STAGE 2: RUNTIME ----------
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copia só o jar final
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
