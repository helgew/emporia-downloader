FROM maven:3.8.7-eclipse-temurin-19 AS build
RUN rm -rf /home/app/dist
COPY src /home/app/src
RUN mv /home/app/src/main/resources/log4j2.xml.docker /home/app/src/main/resources/log4j2.xml
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package

FROM eclipse-temurin:19-jdk-alpine
RUN apk add -U bash
COPY --from=build /home/app/dist/emporia-downloader.*.jar /emporia-downloader.jar
COPY entrypoint.sh /
ENTRYPOINT ["/entrypoint.sh"]

