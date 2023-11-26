FROM sbtscala/scala-sbt:eclipse-temurin-jammy-20.0.2_9_1.9.6_2.13.12

ARG NUMBER_OF_WORKERS

ENV NUMBER_OF_WORKERS=${NUMBER_OF_WORKERS}

RUN mkdir -p /app

WORKDIR /app

COPY build.sbt ./
COPY core/build.sbt ./core/
COPY master/build.sbt ./master/
COPY rpc/build.sbt ./rpc/
COPY utils/build.sbt ./utils/
COPY worker/build.sbt ./worker/
COPY project/build.properties project/plugins.sbt project/scalapb.sbt ./project/

RUN sbt --batch compile

COPY rpc/src/main/protobuf  ./rpc/src/main/protobuf

RUN sbt --batch compile

COPY . .

RUN sbt --batch compile

ENTRYPOINT sbt --batch -v "master/run ${NUMBER_OF_WORKERS}"
