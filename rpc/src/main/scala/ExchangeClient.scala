package kr.ac.postech.paranode.rpc

import java.util.logging.Logger
import java.util.concurrent.TimeUnit
import kr.ac.postech.paranode.rpc.exchange.ExchangeGrpc
import kr.ac.postech.paranode.rpc.exchange.GetMyRecordsRequest
import kr.ac.postech.paranode.rpc.exchange.GetMyRecordsReply

import kr.ac.postech.paranode.rpc.exchange.ExchangeGrpc.ExchangeBlockingStub
import io.grpc.{ManagedChannel, ManagedChannelBuilder, StatusRuntimeException}

object ExchangeClient {
  def apply(host: String, port: Int): ExchangeClient = {
    val channel = ManagedChannelBuilder
      .forAddress(host, port)
      .usePlaintext()
      .build
    val blockingStub = ExchangeGrpc.blockingStub(channel)
    new ExchangeClient(channel, blockingStub)
  }

  def main(args: Array[String]): Unit = {
    val client = ExchangeClient("localhost", 30050)
    try {
      val response = client.SaveRecords()

      println("My records is: " + response.isNice)
    } finally {
      client.shutdown()
    }
  }
}

class ExchangeClient private (
    private val channel: ManagedChannel,
    private val blockingStub: ExchangeBlockingStub
) {
  private[this] val logger = Logger.getLogger(classOf[WorkerClient].getName)

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  def SaveRecords(): GetMyRecordsReply = {
    logger.info(
      "Try to save my records"
    )

    val request = GetMyRecordsRequest()
    val response = blockingStub.saveRecords(request)
    logger.info("My records is: " + response.isNice)

    response
  }
}
