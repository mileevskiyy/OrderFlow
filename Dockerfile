FROM node:24-bookworm-slim AS web
WORKDIR /web
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /build
COPY backend/pom.xml ./pom.xml
RUN mvn -B dependency:go-offline
COPY backend/src ./src
COPY --from=web /web/dist ./src/main/resources/static
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-jammy
RUN groupadd --system orderflow && useradd --system --gid orderflow orderflow
WORKDIR /app
COPY --from=backend /build/target/orderflow-0.1.0.jar ./orderflow.jar
RUN mkdir -p /app/data && chown -R orderflow:orderflow /app
USER orderflow
ENV HOST=0.0.0.0
ENV PORT=3200
ENV JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=70.0
EXPOSE 3200
ENTRYPOINT ["java", "-jar", "orderflow.jar"]
