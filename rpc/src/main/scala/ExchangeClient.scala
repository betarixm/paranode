package kr.ac.postech.paranode.rpc

import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kr.ac.postech.paranode.core.Record

import java.util.concurrent.TimeUnit

import exchange.ExchangeGrpc.ExchangeBlockingStub
import exchange.{ExchangeGrpc, SaveRecordsReply, SaveRecordsRequest}

object ExchangeClient {
  def apply(host: String, port: Int): ExchangeClient = {
    val channel = ManagedChannelBuilder
      .forAddress(host, port)
      .usePlaintext()
      .build()

    val blockingStub = ExchangeGrpc.blockingStub(channel)
    new ExchangeClient(channel, blockingStub)
  }
}

class ExchangeClient private (
    private val channel: ManagedChannel,
    private val blockingStub: ExchangeBlockingStub
) {
  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  def saveRecords(records: LazyList[Record]): SaveRecordsReply = {
    val request =
      SaveRecordsRequest(
        records.map(x => ByteString.copyFrom(x.toChars.map(_.toByte)))
      )

    // TODO

    blockingStub.saveRecords(request)
  }
}
