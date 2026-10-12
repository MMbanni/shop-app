FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src

RUN chmod +x mvnw && ./mvnw -B -ntp clean verify

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN groupadd --system shop \
    && useradd --system --gid shop shop

COPY --from=build /app/target/*.jar /app/app.jar

USER shop:shop

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60.0", "-jar", "/app/app.jar"]