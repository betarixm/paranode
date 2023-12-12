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

FROM debian:bookworm-slim as generator

ARG NUMBER_OF_DISKS=2
ENV NUMBER_OF_DISKS=${NUMBER_OF_DISKS}

ARG NUMBER_OF_PARTITIONS=2
ENV NUMBER_OF_PARTITIONS=${NUMBER_OF_PARTITIONS}

ARG NUMBER_OF_RECORDS=340000
ENV NUMBER_OF_RECORDS=${NUMBER_OF_RECORDS}

WORKDIR /app

COPY bin/gensort .

RUN for diskIndex in $(seq 0 $((${NUMBER_OF_DISKS} - 1))); do\
    mkdir -p /data/${diskIndex};\
    for partitionIndex in $(seq 0 $((${NUMBER_OF_PARTITIONS} - 1))); do ./gensort -a$((${diskIndex} * ${NUMBER_OF_PARTITIONS} * ${NUMBER_OF_RECORDS} + ${partitionIndex} * ${NUMBER_OF_RECORDS})) ${NUMBER_OF_RECORDS}\
    ${partitionIndex};\
    mv ${partitionIndex} /data/${diskIndex}/${partitionIndex};\
    done;\
    done

FROM amazoncorretto:20 as runner

ARG WORKER_ID
ENV WORKER_ID=${WORKER_ID}

ARG MASTER_HOST
ENV MASTER_HOST=${MASTER_HOST}

ARG MASTER_PORT
ENV MASTER_PORT=${MASTER_PORT}

COPY --from=generator /data /data

WORKDIR /app

COPY --from=builder /app/build/worker .

ENTRYPOINT rm -f /output/* && time ./worker ${MASTER_HOST}:${MASTER_PORT} -I /data/0 /data/1 -O /output/
