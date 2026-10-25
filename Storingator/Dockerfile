# ---- build stage -----------------------------------------------------------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x ./gradlew && ./gradlew bootJar --no-daemon -x test

# ---- runtime stage ---------------------------------------------------------
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd --system storingator && useradd --system --gid storingator --no-create-home storingator

COPY --from=build /app/build/libs/Storingator-0.0.1-SNAPSHOT.jar /app/app.jar

USER storingator
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
