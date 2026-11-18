FROM node:24-alpine AS frontend
WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app
COPY pom.xml ./
COPY src/ ./src/
COPY --from=frontend /frontend/dist/ ./src/main/resources/static/
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend /app/target/tutoringappointments-0.0.1-SNAPSHOT.jar app.jar
USER 10001
EXPOSE 8062
ENTRYPOINT ["java", "-jar", "app.jar"]
