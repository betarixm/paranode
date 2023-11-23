package kr.ac.postech.paranode.rpc

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kr.ac.postech.paranode.core.WorkerMetadata

import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import common.Node
import master.MasterGrpc.MasterStub
import master.{MasterGrpc, RegisterRequest}

object MasterClient {
  def apply(host: String, port: Int): MasterClient = {
    val channel =
      ManagedChannelBuilder.forAddress(host, port).usePlaintext().build
    val stub = MasterGrpc.stub(channel)
    new MasterClient(channel, stub)
  }

  def main(args: Array[String]): Unit = {
    val client = MasterClient("localhost", 50051)
    try {
      val workerMetadata = WorkerMetadata("1.2.3.4", 56, None)
      client.register(workerMetadata)
    } finally {
      client.shutdown()
    }
  }

}

class MasterClient private (
    private val channel: ManagedChannel,
    private val stub: MasterStub
) {
  Logger.getLogger(classOf[MasterClient].getName)

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  /** Say hello to server. */
  def register(
      workerMetadata: WorkerMetadata
  ): Unit = {
    val request = RegisterRequest(
      Some(Node(workerMetadata.host, workerMetadata.port))
    )

    stub.register(request)
  }
}
