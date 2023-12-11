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

ARG WORKER_ID

ARG MASTER_HOST
ARG MASTER_PORT

ENV MASTER_HOST=${MASTER_HOST}
ENV MASTER_PORT=${MASTER_PORT}

RUN mkdir -p /data /output

COPY docker/worker/data/${WORKER_ID} /data

WORKDIR /app

COPY --from=builder /app/build/worker .

ENTRYPOINT ./worker ${MASTER_HOST}:${MASTER_PORT} -I /data/0 /data/1 -O /output/
