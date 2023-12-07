FROM sbtscala/scala-sbt:eclipse-temurin-jammy-20.0.2_9_1.9.6_2.13.12 as builder

WORKDIR /app

COPY build.sbt log4j2.properties ./
COPY core/build.sbt ./core/
COPY master/build.sbt ./master/
COPY rpc/build.sbt ./rpc/
COPY utils/build.sbt ./utils/
COPY worker/build.sbt ./worker/
COPY project/build.properties project/plugins.sbt project/scalapb.sbt ./project/

RUN sbt --batch compile

COPY . .

RUN sbt --batch "compile; assembly"

FROM amazoncorretto:20 as runner

ARG NUMBER_OF_WORKERS
ENV NUMBER_OF_WORKERS=${NUMBER_OF_WORKERS}

WORKDIR /app

COPY --from=builder /app/master/target/scala-2.13/master.jar .

ENTRYPOINT java -jar master.jar ${NUMBER_OF_WORKERS}
