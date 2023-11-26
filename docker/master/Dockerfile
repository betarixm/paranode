FROM sbtscala/scala-sbt:eclipse-temurin-jammy-20.0.2_9_1.9.6_2.13.12

RUN mkdir -p /app

WORKDIR /app

COPY project/build.properties project/plugins.sbt project/scalapb.sbt /app/project/

COPY . .

RUN sbt --batch compile

ENTRYPOINT sbt --batch -v "master/run 1"
