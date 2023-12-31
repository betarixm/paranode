package kr.ac.postech.paranode.rpc

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kr.ac.postech.paranode.core.WorkerMetadata

import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import scala.concurrent.Future

import common.Node
import master.MasterGrpc.MasterStub
import master.{MasterGrpc, RegisterReply, RegisterRequest}

object MasterClient {
  def apply(host: String, port: Int): MasterClient = {
    val channel =
      ManagedChannelBuilder.forAddress(host, port).usePlaintext().build
    val stub = MasterGrpc.stub(channel)
    new MasterClient(channel, stub)
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

  def shutdownNow(): Unit = {
    channel.shutdownNow()
  }

  def register(
      workerMetadata: WorkerMetadata
  ): Future[RegisterReply] = {
    val request = RegisterRequest(
      Some(Node(workerMetadata.host, workerMetadata.port))
    )

    stub.register(request)
  }
}
