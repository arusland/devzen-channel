FROM eclipse-temurin:17-jdk-alpine@sha256:ddd7a05cf8263989c29f2a9476dcfa25d0eaf8310d400f998ebd03c0d32feb72

RUN mkdir -p /app/logs
# "state" dir for config files, auth files, etc
# "files" for mp3 and html files
RUN mkdir -p /app/state/files

WORKDIR /app

COPY ../target/devzentg-jar-with-dependencies.jar /app/app.jar

CMD "java" "-jar" "app.jar"
