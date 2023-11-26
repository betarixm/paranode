FROM sbtscala/scala-sbt:eclipse-temurin-jammy-20.0.2_9_1.9.6_2.13.12

ARG MASTER_HOST
ARG MASTER_PORT

ENV MASTER_HOST=${MASTER_HOST}
ENV MASTER_PORT=${MASTER_PORT}

ENV SBT_OPTS="-Xmx2G -Xss2M"

RUN mkdir -p /app /data /output

COPY docker/worker/data /data

WORKDIR /app

COPY project/build.properties project/plugins.sbt project/scalapb.sbt /app/project/

COPY . .

RUN sbt --batch compile

ENTRYPOINT sbt --batch -v "worker/run ${MASTER_HOST}:${MASTER_PORT} -I /data/0 data/1 data/2 -O /output/"
