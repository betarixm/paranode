FROM sbtscala/scala-sbt:eclipse-temurin-jammy-20.0.2_9_1.9.6_2.13.12

ARG MASTER_HOST
ARG MASTER_PORT

ENV MASTER_HOST=${MASTER_HOST}
ENV MASTER_PORT=${MASTER_PORT}

ENV SBT_OPTS="-Xmx2G -Xss2M"

RUN mkdir -p /app /data /output

COPY docker/worker/data /data

WORKDIR /app

COPY build.sbt log4j2.properties ./
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

ENTRYPOINT sbt --batch -v "worker/run ${MASTER_HOST}:${MASTER_PORT} -I /data/0 /data/1 /data/2 -O /output/"
