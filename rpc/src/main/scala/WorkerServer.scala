package kr.ac.postech.paranode.rpc

import com.google.protobuf.ByteString
import io.grpc.Server
import io.grpc.ServerBuilder
import kr.ac.postech.paranode.core._
import org.apache.logging.log4j.scala.Logging

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.reflect.io.Directory
import scala.reflect.io.Path

import common.{WorkerMetadata => RpcWorkerMetadata}
import worker._

class WorkerServer(
    executionContext: ExecutionContext,
    port: Int,
    inputDirectories: Array[Directory],
    outputDirectory: Directory
) extends Logging { self =>
  private[this] val server: Server = ServerBuilder
    .forPort(port)
    .addService(WorkerGrpc.bindService(new WorkerImpl, executionContext))
    .build()

  private def inputFiles = inputDirectories.flatMap(_.files)

  private def outputFiles = outputDirectory.files.toList

  def start(): Unit = {
    server.start()

    logger.debug(
      "[WorkerServer] \n" +
        s"port: $port\n" +
        s"inputDirectories: ${inputDirectories.mkString(", ")}\n" +
        s"inputFiles: ${inputFiles.mkString(", ")}\n" +
        s"outputDirectory: $outputDirectory\n"
    )

    sys.addShutdownHook {
      logger.error(
        "[WorkerServer] shutting down gRPC server since JVM is shutting down"
      )
      self.stop()
      logger.error("[WorkerServer] server shut down")
    }
  }

  def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

  private class WorkerImpl extends WorkerGrpc.Worker {
    private def toWorkerMetadata(workers: Seq[RpcWorkerMetadata]) =
      workers.map(worker =>
        WorkerMetadata(
          worker.node.get.host,
          worker.node.get.port,
          worker.keyRange.map(keyRange =>
            KeyRange(
              Key.fromByteString(keyRange.from),
              Key.fromByteString(keyRange.to)
            )
          )
        )
      )

    override def sample(request: SampleRequest): Future[SampleReply] = {
      val promise = Promise[SampleReply]

      Future {
        logger.debug(s"[WorkerServer] Sample ($request)")

        val sampledKeys = inputFiles
          .map(f => Block.fromPath(f.path))
          .flatMap(_.sample(request.numberOfKeys))
          .map(key => ByteString.copyFrom(key.underlying))

        promise.success(SampleReply(sampledKeys))
      }(executionContext)

      promise.future
    }

    override def sort(request: SortRequest): Future[SortReply] = {
      val promise = Promise[SortReply]

      Future {
        logger.debug(s"[WorkerServer] Sort ($request)")

        inputFiles
          .foreach(path => {
            val block = Block.fromPath(path)

            val sortedBlock = block.sorted

            logger.debug(s"[WorkerServer] Writing sorted block to $path")

            sortedBlock.writeTo(path)

            logger.debug(s"[WorkerServer] Wrote sorted block to $path")
          })

        promise.success(new SortReply())
      }

      promise.future
    }

    override def partition(
        request: PartitionRequest
    ): Future[PartitionReply] = {
      val promise = Promise[PartitionReply]

      Future {
        logger.debug(s"[WorkerServer] Partition ($request)")

        val workers = toWorkerMetadata(request.workers)

        inputFiles
          .map(path => {
            val block = Block.fromPath(path)
            workers
              .map(_.keyRange.get)
              .map(block.partition)
              .map({ case (keyRange, partition) =>
                val partitionPath = Path(
                  s"$path.${keyRange.from.hex}-${keyRange.to.hex}"
                )

                logger.debug(
                  s"[WorkerServer] Writing partition to $partitionPath"
                )

                partition.writeTo(partitionPath)

                logger.debug(
                  s"[WorkerServer] Wrote partition to $partitionPath"
                )

                if (path.exists && path.isFile) {
                  val result = path.delete()
                  logger.debug(s"[WorkerServer] Deleted $path: $result")
                }
              })

          })

        promise.success(new PartitionReply())
      }(executionContext)

      promise.future
    }

    override def exchange(request: ExchangeRequest): Future[ExchangeReply] = {
      val promise = Promise[ExchangeReply]

      Future {
        logger.debug(s"[WorkerServer] Exchange ($request)")

        val workers = toWorkerMetadata(request.workers)

        inputFiles.foreach(path => {
          val block = Block.fromPath(path)
          val targetWorkers = workers
            .filter(_.keyRange.get.includes(block.records.head.key))

          logger.debug(s"[WorkerServer] Sending $block to $targetWorkers")

          Await.result(
            Future.sequence(
              targetWorkers
                .map(worker => WorkerClient(worker.host, worker.port))
                .map(_.saveBlock(block))
            ),
            scala.concurrent.duration.Duration.Inf
          )
        })

        promise.success(new ExchangeReply())
      }

      promise.future
    }

    override def saveBlock(
        request: SaveBlockRequest
    ): Future[SaveBlockReply] = {
      val promise = Promise[SaveBlockReply]

      Future {
        logger.debug(s"[WorkerServer] SaveBlock ($request)")

        val block = Block.fromBytes(
          LazyList.from(request.block.toByteArray)
        )

        val path = outputDirectory / UUID.randomUUID().toString

        logger.debug(s"[WorkerServer] Writing block to $path")

        block.writeTo(path)

        logger.debug(s"[WorkerServer] Wrote block to $path")

        promise.success(new SaveBlockReply())
      }

      promise.future
    }

    override def merge(request: MergeRequest): Future[MergeReply] = {
      val promise = Promise[MergeReply]

      Future {
        logger.debug(s"[WorkerServer] Merge ($request)")
        val targetFiles = outputFiles

        val blocks = targetFiles.map(path => Block.fromPath(path))

        logger.debug("[WorkerServer] Merging blocks")

        val mergedBlock = blocks.merged

        val results = mergedBlock.writeTo(outputDirectory / "result")

        targetFiles.foreach(file => {
          val result = file.delete()

          logger.debug(
            s"[WorkerServer] Deleted $file: $result"
          )
        })

        logger.debug(
          s"[WorkerServer] Merged blocks: $results"
        )

        promise.success(new MergeReply())
      }(executionContext)

      promise.future
    }

  }

}
