package kr.ac.postech.paranode.rpc

import java.util.logging.Logger
import java.util.concurrent.TimeUnit
import kr.ac.postech.paranode.rpc.worker.ExchangeReply
import kr.ac.postech.paranode.rpc.worker.ExchangeRequest
import kr.ac.postech.paranode.rpc.worker.MergeReply
import kr.ac.postech.paranode.rpc.worker.MergeRequest
import kr.ac.postech.paranode.rpc.worker.PartitionReply
import kr.ac.postech.paranode.rpc.worker.PartitionRequest
import kr.ac.postech.paranode.rpc.worker.SampleReply
import kr.ac.postech.paranode.rpc.worker.SampleRequest
import kr.ac.postech.paranode.rpc.worker.WorkerGrpc

import kr.ac.postech.paranode.rpc.worker.WorkerGrpc.WorkerBlockingStub
import io.grpc.{ManagedChannel, ManagedChannelBuilder, StatusRuntimeException}
import kr.ac.postech.paranode.rpc.worker.PartitionRequest.WorkerPartition

object WorkerClient {
  def apply(host: String, port: Int): WorkerClient = {
    val channel = ManagedChannelBuilder
      .forAddress(host, port)
      .usePlaintext()
      .build
    val blockingStub = WorkerGrpc.blockingStub(channel)
    new WorkerClient(channel, blockingStub)
  }

  def main(args: Array[String]): Unit = {
    val client = WorkerClient("localhost", 30040)
    try {
      // TODO: later, numberOfKeys should be given by the master or we don't need this
      val numberOfKeys = 3
      val responseOfSample = client.sample(numberOfKeys)

      println(
        "Sampled keys: " + responseOfSample.sampledKeys.mkString(", ")
      )

      // TODO: Add more logic for link above sampled keys logic and below partition logic
      val workerPartitions = List(
        Tuple3("111.222.333.1", "startKey1", "endKey1"),
        Tuple3("111.222.333.2", "startKey2", "endKey2")
      )

      val responseOfPartition = client.partition(workerPartitions)

      println("Partitioned is: " + responseOfPartition.isNice)
    } finally {
      client.shutdown()
    }
  }
}

class WorkerClient private (
    private val channel: ManagedChannel,
    private val blockingStub: WorkerBlockingStub
) {
  private[this] val logger = Logger.getLogger(classOf[WorkerClient].getName)

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  def sample(numberOfKeys: Int): SampleReply = {
    logger.info(
      "Try to sample " + numberOfKeys + " keys"
    )

    val request = SampleRequest(numberOfKeys)
    val response = blockingStub.sampleKeys(request)
    logger.info("Sampled keys: " + response.sampledKeys.mkString(", "))

    response
  }

  def partition(
      workerPartitions: List[(String, String, String)]
  ): PartitionReply = {
    logger.info(
      "Try to partition"
    )
    val partitions = workerPartitions.map {
      case (ipAddress, startKey, endKey) =>
        WorkerPartition(ipAddress, startKey, endKey)
    }

    val request = PartitionRequest(partitions)
    val response = blockingStub.makePartitions(request)
    logger.info("Partitioned is: " + response.isNice)

    response
  }

  def exchange(): ExchangeReply = {
    logger.info(
      "Try to exchange"
    )

    val request = ExchangeRequest()
    val response = blockingStub.exchangeWithOtherWorker(request)
    logger.info("Exchanged is: " + response.isNice)

    response
  }

  def merge(): MergeReply = {
    logger.info(
      "Try to merge"
    )

    val request = MergeRequest()
    val response = blockingStub.merge(request)
    logger.info("Merged is: " + response.isNice)

    response
  }

}
