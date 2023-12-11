package kr.ac.postech.paranode.rpc
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kr.ac.postech.paranode.core.Block
import kr.ac.postech.paranode.core.WorkerMetadata
import kr.ac.postech.paranode.utils.GenericBuildFrom

import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import worker._
import worker.WorkerGrpc.WorkerStub
import Implicit._

object WorkerClient {

  implicit class WorkerClients(val clients: List[WorkerClient]) {
    def sample(
        numberOfKeys: Int
    )(implicit executionContext: ExecutionContext): List[SampleReply] =
      Await.result(
        Future.traverse(clients)(_.sample(numberOfKeys))(
          GenericBuildFrom[WorkerClient, SampleReply],
          executionContext
        ),
        scala.concurrent.duration.Duration.Inf
      )

    def sort()(implicit executionContext: ExecutionContext): List[SortReply] =
      Await.result(
        Future.traverse(clients)(_.sort())(
          GenericBuildFrom[WorkerClient, SortReply],
          executionContext
        ),
        scala.concurrent.duration.Duration.Inf
      )

    def partition(
        keyRanges: List[WorkerMetadata]
    )(implicit executionContext: ExecutionContext): List[PartitionReply] =
      Await.result(
        Future.traverse(clients)(_.partition(keyRanges))(
          GenericBuildFrom[WorkerClient, PartitionReply],
          executionContext
        ),
        scala.concurrent.duration.Duration.Inf
      )

    def exchange(
        keyRanges: List[WorkerMetadata]
    )(implicit executionContext: ExecutionContext): List[ExchangeReply] =
      Await.result(
        Future.traverse(clients)(_.exchange(keyRanges))(
          GenericBuildFrom[WorkerClient, ExchangeReply],
          executionContext
        ),
        scala.concurrent.duration.Duration.Inf
      )

    def merge()(implicit executionContext: ExecutionContext): List[MergeReply] =
      Await.result(
        Future.traverse(clients)(_.merge())(
          GenericBuildFrom[WorkerClient, MergeReply],
          executionContext
        ),
        scala.concurrent.duration.Duration.Inf
      )

    def terminate()(implicit
        executionContext: ExecutionContext
    ): List[TerminateReply] =
      Await.result(
        Future.traverse(clients)(_.terminate())(
          GenericBuildFrom[WorkerClient, TerminateReply],
          executionContext
        ),
        scala.concurrent.duration.Duration.Inf
      )
  }

  def apply(host: String, port: Int): WorkerClient = {
    val channel = ManagedChannelBuilder
      .forAddress(host, port)
      .usePlaintext()
      .build
    val stub = WorkerGrpc.stub(channel)
    new WorkerClient(channel, stub)
  }

}

class WorkerClient private (
    private val channel: ManagedChannel,
    private val stub: WorkerStub
) {
  Logger.getLogger(classOf[WorkerClient].getName)

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  def shutdownNow(): Unit = {
    channel.shutdownNow()
  }

  def sample(numberOfKeys: Int): Future[SampleReply] = {
    val request = SampleRequest(numberOfKeys)
    val response = stub.sample(request)

    response
  }

  def sort(): Future[SortReply] = {
    val request = SortRequest()
    val response = stub.sort(request)

    response
  }

  def partition(
      workers: List[WorkerMetadata]
  ): Future[PartitionReply] = {
    val request = PartitionRequest(workers)

    stub.partition(request)
  }

  def exchange(
      workers: List[WorkerMetadata]
  ): Future[ExchangeReply] = {
    val request = ExchangeRequest(workers)

    stub.exchange(request)
  }

  def saveBlock(
      block: Block
  ): Future[SaveBlockReply] = {
    val request = SaveBlockRequest(block)

    stub.saveBlock(request)
  }

  def merge(): Future[MergeReply] = {
    val request = MergeRequest()
    stub.merge(request)
  }

  def terminate(): Future[TerminateReply] = {
    val request = TerminateRequest()
    stub.terminate(request)
  }
}
