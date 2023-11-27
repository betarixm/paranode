package kr.ac.postech.paranode.rpc

import com.google.protobuf.ByteString
import kr.ac.postech.paranode.core.Block
import kr.ac.postech.paranode.core.WorkerMetadata
import org.apache.logging.log4j.scala.Logging

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.reflect.io.Directory
import scala.reflect.io.File
import scala.reflect.io.Path

import worker.{
  ExchangeReply,
  ExchangeRequest,
  MergeReply,
  MergeRequest,
  PartitionReply,
  PartitionRequest,
  SampleReply,
  SampleRequest,
  SaveBlockReply,
  SaveBlockRequest,
  SortReply,
  SortRequest,
  WorkerGrpc
}
import Implicit._

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
      logger.info(s"[WorkerServer] Sample ($request)")

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
      logger.info(s"[WorkerServer] Sort ($request)")

      inputFiles
        .foreach(path => {
          val block = Block.fromPath(path)

          val sortedBlock = block.sorted

          logger.info(s"[WorkerServer] Writing sorted block to $path")

          sortedBlock.writeTo(path)

          logger.info(s"[WorkerServer] Wrote sorted block to $path")
        })

      promise.success(new SortReply())
    }(executionContext)

    promise.future
  }

  override def partition(
      request: PartitionRequest
  ): Future[PartitionReply] = {
    val promise = Promise[PartitionReply]

    Future {
      logger.info(s"[WorkerServer] Partition ($request)")

      val workers: Seq[WorkerMetadata] = request.workers

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

              logger.info(
                s"[WorkerServer] Writing partition to $partitionPath"
              )

              partition.writeTo(partitionPath)

              logger.info(
                s"[WorkerServer] Wrote partition to $partitionPath"
              )

              if (path.exists && path.isFile) {
                val result = path.delete()
                logger.info(s"[WorkerServer] Deleted $path: $result")
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
      logger.info(s"[WorkerServer] Exchange ($request)")

      val workers: Seq[WorkerMetadata] = request.workers

      inputFiles.foreach(path => {
        val block = Block.fromPath(path)
        val targetWorkers = workers
          .filter(_.keyRange.get.includes(block.records.head.key))

        logger.info(s"[WorkerServer] Sending $block to $targetWorkers")

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
    }(executionContext)

    promise.future
  }

  override def saveBlock(
      request: SaveBlockRequest
  ): Future[SaveBlockReply] = {
    val promise = Promise[SaveBlockReply]

    Future {
      logger.info(s"[WorkerServer] SaveBlock ($request)")

      val block: Block = request.block

      val path = outputDirectory / UUID.randomUUID().toString

      logger.info(s"[WorkerServer] Writing block to $path")

      block.writeTo(path)

      logger.info(s"[WorkerServer] Wrote block to $path")

      promise.success(new SaveBlockReply())
    }(executionContext)

    promise.future
  }

  override def merge(request: MergeRequest): Future[MergeReply] = {
    val promise = Promise[MergeReply]

    Future {
      logger.info(s"[WorkerServer] Merge ($request)")
      val targetFiles = outputFiles

      val blocks = targetFiles.map(path => Block.fromPath(path))

      logger.info("[WorkerServer] Merging blocks")

      val mergedBlock = blocks.merged

      val results = mergedBlock.writeTo(outputDirectory / "result")

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
    }(executionContext)

    promise.future
  }

}
