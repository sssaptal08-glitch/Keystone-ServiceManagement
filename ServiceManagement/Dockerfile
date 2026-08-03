# --- Build stage ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Cache dependencies separately from source for faster rebuilds
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline

COPY src ./src
RUN ./mvnw -q clean package -DskipTests

# --- Run stage ---
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --create-home --shell /bin/false appuser
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/uploads && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
