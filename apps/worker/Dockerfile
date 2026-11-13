# syntax=docker/dockerfile:1
FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :apps:worker:bootJar -x test --no-daemon \
 && cp apps/worker/build/libs/*-SNAPSHOT.jar /app.jar

FROM eclipse-temurin:17-jre
RUN useradd --system --create-home app
USER app
WORKDIR /app
COPY --from=build /app.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","app.jar"]
