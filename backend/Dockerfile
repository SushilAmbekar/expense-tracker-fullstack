# Build stage: compile and package the Spring Boot jar (tests run in CI, not here)
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q -ntp dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q -ntp package -DskipTests && cp target/expense-tracker-*.jar app.jar

# Runtime stage: a small JRE image running as a non-root user
FROM eclipse-temurin:17-jre
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=build /app/app.jar app.jar
USER app
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
