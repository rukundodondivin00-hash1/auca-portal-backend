# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Environment variables
ENV PORT=8080
ENV DATABASE_URL=postgresql://localhost:5432/auca_db
ENV AUCA_FINANCE_BASE_URL=https://auca-ims.onrender.com
ENV JWT_SECRET=urubuto-webhook-secret-key-2024-auca-portal
ENV WEBHOOK_USERNAME=urubuto_webhook
ENV WEBHOOK_PASSWORD=webhook_password_2024

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]