FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
EXPOSE 8081
ARG JAR_FILE=apps/worker/build/libs/*-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","app.jar"]

# FROM eclipse-temurin:17-jre
  #WORKDIR /app
  #COPY build/libs/*.jar app.jar
  #EXPOSE 8081
  #ENTRYPOINT ["java","-jar","/app/app.jar"]