package kr.ac.postech.paranode.rpc

import com.google.protobuf.ByteString
import io.grpc.Server
import io.grpc.ServerBuilder
import kr.ac.postech.paranode.core._
import org.apache.logging.log4j.scala.Logging

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
              })

          })

        promise.success(new PartitionReply())
      }(executionContext)

      promise.future
    }

    override def exchange(request: ExchangeRequest): Future[ExchangeReply] = {
      val futures =
        request.workers.map(_ => Future {}(executionContext))

      Future.sequence(futures).map(_ => new ExchangeReply())
    }

    override def merge(request: MergeRequest): Future[MergeReply] = {
      val promise = Promise[MergeReply]

      Future {
        try {
          val host = Path("data/host")
          val port = Path("data/port")
          val blockPath = Path(s"data/partition/${host}:${port}")
          val mergedBlock = Block.fromPath(blockPath, 10, 90).sorted
          mergedBlock.writeTo(blockPath)

          promise.success(new MergeReply())
        } catch {
          case e: Exception =>
            println(e)
            promise.failure(e)
        }
      }(executionContext)

      promise.future
    }

  }

}
