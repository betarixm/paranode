package kr.ac.postech.paranode.worker

import com.google.protobuf.ByteString
import io.grpc.ServerServiceDefinition
import kr.ac.postech.paranode.core.Block
import kr.ac.postech.paranode.core.WorkerMetadata
import kr.ac.postech.paranode.rpc.Implicit._
import kr.ac.postech.paranode.rpc.WorkerClient
import kr.ac.postech.paranode.rpc.worker._
import kr.ac.postech.paranode.utils.GenericBuildFrom
import org.apache.logging.log4j.scala.Logging

import java.util.UUID
import scala.concurrent._
import scala.reflect.io.Directory
import scala.reflect.io.File
import scala.reflect.io.Path

object WorkerService {
  def apply(
      inputDirectories: Array[Directory],
      outputDirectory: Directory
  )(implicit executionContext: ExecutionContext): ServerServiceDefinition =
    WorkerGrpc.bindService(
      new WorkerService(
        executionContext,
        inputDirectories,
        outputDirectory
      ),
      executionContext
    )
}

class WorkerService(
    executionContext: ExecutionContext,
    inputDirectories: Array[Directory],
    outputDirectory: Directory
) extends WorkerGrpc.Worker
    with Logging {

  private def inputFiles: Array[File] = inputDirectories.flatMap(_.files)

  private def outputFiles: List[File] = outputDirectory.files.toList

  override def sample(request: SampleRequest): Future[SampleReply] = {
    val promise = Promise[SampleReply]

    Future {
      try {
        logger.info(s"[WorkerServer] Sample ($request)")

        val sampledKeys = inputFiles
          .map(f => Block.fromPath(f.path))
          .flatMap(_.sample(request.numberOfKeys))
          .map(key => ByteString.copyFrom(key.underlying))

        promise.success(SampleReply(sampledKeys))
      } catch {
        case e: Exception =>
          logger.error(s"[WorkerServer] Sample ($request)\n$e", e)
          promise.failure(e)
      }
    }(executionContext)

    promise.future
  }

  override def sort(request: SortRequest): Future[SortReply] = {
    val promise = Promise[SortReply]

    implicit val executionContext: ExecutionContextExecutor =
      scala.concurrent.ExecutionContext.fromExecutor(
        java.util.concurrent.Executors
          .newCachedThreadPool()
      )

    def sorted(path: Path) = Future {
      logger.info(s"[WorkerServer] Sorting block $path")
      val result = Block.fromPath(path).sorted.writeTo(path)
      logger.info(s"[WorkerServer] Wrote sorted block $path")
      result
    }

    Future {
      try {
        logger.info(s"[WorkerServer] Sort ($request)")

        Await.result(
          Future.traverse(inputFiles.toList)(sorted)(
            GenericBuildFrom[File, File],
            executionContext
          ),
          scala.concurrent.duration.Duration.Inf
        )

        logger.info("[WorkerServer] Sorted")

        promise.success(new SortReply())
      } catch {
        case e: Exception =>
          logger.error(s"[WorkerServer] Sort ($request)\n$e", e)
          promise.failure(e)
      }
    }(executionContext)

    promise.future
  }

  override def partition(
      request: PartitionRequest
  ): Future[PartitionReply] = {
    val promise = Promise[PartitionReply]

    implicit val executionContext: ExecutionContextExecutor =
      scala.concurrent.ExecutionContext.fromExecutor(
        java.util.concurrent.Executors
          .newCachedThreadPool()
      )

    val workers: Seq[WorkerMetadata] = request.workers

    def partition(path: Path) = Future {
      val block = Block.fromPath(path)

      logger.info("[WorkerServer] Partitioning block")

      val partitions = workers
        .map(_.keyRange.get)
        .map(keyRange => {
          block.partition(keyRange)
        })

      logger.info("[WorkerServer] Partitioned block")

      path.delete()
      
      logger.info(s"[WorkerServer] Delete input file: ${path}")

      logger.info("[WorkerServer] Writing partitions")

      val result = partitions.map({ case (keyRange, partition) =>
        partition.writeTo(
          Path(
            s"$path.${keyRange.from.hex}-${keyRange.to.hex}"
          )
        )
      })

      logger.info("[WorkerServer] Wrote partitions")

      result
    }

    Future {
      try {
        logger.info(s"[WorkerServer] Partition ($request)")

        Await.result(
          Future.traverse(inputFiles.toList)(partition)(
            GenericBuildFrom[File, Seq[File]],
            executionContext
          ),
          scala.concurrent.duration.Duration.Inf
        )

        logger.info("[WorkerServer] Partitioned")

        promise.success(new PartitionReply())
      } catch {
        case e: Exception =>
          logger.error(s"[WorkerServer] Partition ($request)\n$e", e)
          promise.failure(e)
      }
    }(executionContext)

    promise.future
  }

  override def exchange(request: ExchangeRequest): Future[ExchangeReply] = {
    val promise = Promise[ExchangeReply]

    implicit val executionContext: ExecutionContextExecutor =
      scala.concurrent.ExecutionContext.fromExecutor(
        java.util.concurrent.Executors
          .newCachedThreadPool()
      )

    val workers: Seq[WorkerMetadata] = request.workers

    Future {
      try {
        logger.info(s"[WorkerServer] Exchange ($request)")

        val clients = workers.map(worker => {
          WorkerClient(worker.host, worker.port)
        })

        val workersWithClients = workers.zip(clients).toList

        inputFiles.foreach(path => {
          val block = Block.fromPath(path)

          val targetClients = workersWithClients
            .filter(
              _._1.keyRange.get.includes(block.records.head.key)
            )
            .map(_._2)

          logger.info(s"[WorkerServer] Sending $block to $targetClients")

          Await.result(
            Future.traverse(targetClients)(_.saveBlock(block))(
              GenericBuildFrom[WorkerClient, SaveBlockReply],
              executionContext
            ),
            scala.concurrent.duration.Duration.Inf
          )
        })

        clients.foreach(_.shutdown())

        logger.info("[WorkerServer] Sent blocks")

        promise.success(new ExchangeReply())
      } catch {
        case e: Exception =>
          logger.error(s"[WorkerServer] Exchange ($request)\n$e", e)
          promise.failure(e)
      }
    }(executionContext)

    promise.future
  }

  override def saveBlock(
      request: SaveBlockRequest
  ): Future[SaveBlockReply] = {
    val promise = Promise[SaveBlockReply]

    Future {
      try {
        logger.info(s"[WorkerServer] SaveBlock ($request)")

        val block: Block = request.block

        val path = outputDirectory / UUID.randomUUID().toString

        logger.info(s"[WorkerServer] Writing block to $path")

        block.writeTo(path)

        logger.info(s"[WorkerServer] Wrote block to $path")

        promise.success(new SaveBlockReply())
      } catch {
        case e: Exception =>
          logger.error(s"[WorkerServer] SaveBlock ($request)\n$e", e)
          promise.failure(e)
      }
    }(executionContext)

    promise.future
  }

  override def merge(request: MergeRequest): Future[MergeReply] = {
    val promise = Promise[MergeReply]

    Future {
      try {
        logger.info(s"[WorkerServer] Merge ($request)")
        val targetFiles = outputFiles

        val blocks = targetFiles.map(path => Block.fromPath(path))

        logger.info("[WorkerServer] Merging blocks")

        val mergedBlock = blocks.merged

        logger.info("[WorkerServer] Merged blocks")

        logger.info("[WorkerServer] Writing merged block")

        val results = mergedBlock.writeToDirectory(outputDirectory)

        logger.info("[WorkerServer] Wrote merged block")

        targetFiles.foreach(file => {
          val result = file.delete()

          logger.info(
            s"[WorkerServer] Deleted $file: $result"
          )
        })

        logger.info(
          s"[WorkerServer] Merged blocks: $results"
        )

        promise.success(new MergeReply())
      } catch {
        case e: Exception =>
          logger.error(s"[WorkerServer] Merge ($request)\n$e", e)
          promise.failure(e)
      }
    }(executionContext)

    promise.future
  }

}
