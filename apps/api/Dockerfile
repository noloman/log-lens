FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
EXPOSE 8080
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

# FROM eclipse-temurin:17-jre
  #WORKDIR /app
  #COPY build/libs/*.jar app.jar
  #EXPOSE 8080
  #ENTRYPOINT ["java","-jar","/app/app.jar"]