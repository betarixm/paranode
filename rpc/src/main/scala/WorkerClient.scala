package kr.ac.postech.paranode.rpc

import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kr.ac.postech.paranode.core.KeyRange
import kr.ac.postech.paranode.core.WorkerMetadata

import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import worker._
import worker.WorkerGrpc.WorkerBlockingStub
import common.{
  Node,
  KeyRange => RpcKeyRange,
  WorkerMetadata => RpcWorkerMetadata
}

object WorkerClient {
  def apply(host: String, port: Int): WorkerClient = {
    val channel = ManagedChannelBuilder
      .forAddress(host, port)
      .usePlaintext()
      .build
    val blockingStub = WorkerGrpc.blockingStub(channel)
    new WorkerClient(channel, blockingStub)
  }
}

class WorkerClient private (
    private val channel: ManagedChannel,
    private val blockingStub: WorkerBlockingStub
) {
  Logger.getLogger(classOf[WorkerClient].getName)

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  def sample(numberOfKeys: Int): SampleReply = {
    val request = SampleRequest(numberOfKeys)
    val response = blockingStub.sample(request)

    response
  }

  def partition(
      workers: List[(WorkerMetadata, KeyRange)]
  ): PartitionReply = {
    val request = PartitionRequest(workers.map({ case (worker, keyRange) =>
      RpcWorkerMetadata(
        Some(Node(worker.host, worker.port)),
        Some(
          RpcKeyRange(
            ByteString.copyFrom(keyRange.from.underlying),
            ByteString.copyFrom(keyRange.to.underlying)
          )
        )
      )
    }))

    blockingStub.partition(request)
  }

  def exchange(workers: List[(WorkerMetadata, KeyRange)]): ExchangeReply = {
    val request = ExchangeRequest(workers.map({ case (worker, keyRange) =>
      RpcWorkerMetadata(
        Some(Node(worker.host, worker.port)),
        Some(
          RpcKeyRange(
            ByteString.copyFrom(keyRange.from.underlying),
            ByteString.copyFrom(keyRange.to.underlying)
          )
        )
      )
    }))

    blockingStub.exchange(request)
  }

  def merge(): MergeReply = {
    val request = MergeRequest()
    blockingStub.merge(request)
  }
}
