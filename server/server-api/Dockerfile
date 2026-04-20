FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /build

COPY server/server-api/pom.xml ./pom.xml
RUN mvn -B -DskipTests dependency:go-offline

COPY server/server-api/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --create-home app

COPY --from=build /build/target/server-api-0.0.1-SNAPSHOT.jar /app/app.jar

RUN mkdir -p /app/uploads /app/logs && chown -R app:app /app

USER app

ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080
ENV APP_UPLOAD_PATH=/app/uploads
ENV JAVA_OPTS="-Xms256m -Xmx768m"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
