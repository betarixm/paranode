package kr.ac.postech.paranode.rpc

import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}

import kr.ac.postech.paranode.rpc.master.{
  MasterGrpc,
  MasterProto,
  RegisterRequest,
  RegisterRelpy
}

import kr.ac.postech.paranode.rpc.master.MasterGrpc.MasterBlockingStub

import io.grpc.{StatusRuntimeException, ManagedChannelBuilder, ManagedChannel}

object MasterClient {
  def apply(host: String, port: Int): MasterClient = {
    val channel =
      ManagedChannelBuilder.forAddress(host, port).usePlaintext().build
    val blockingStub = MasterGrpc.blockingStub(channel)
    new MasterClient(channel, blockingStub)
  }

  def main(args: Array[String]): Unit = {
    val client = MasterClient("localhost", 50051)
    try {
      val ipAddress = "1.2.3.4"
      val inputDirectory = List("a/a", "b/b")
      val outputDirectory = "c/c"
      client.register(ipAddress, inputDirectory, outputDirectory)
    } finally {
      client.shutdown()
    }
  }
}

class MasterClient private (
    private val channel: ManagedChannel,
    private val blockingStub: MasterBlockingStub
) {
  private[this] val logger = Logger.getLogger(classOf[MasterClient].getName)

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  /** Say hello to server. */
  def register(
      ipAddress: String,
      inputDirectory: List[String],
      outputDirectory: String
  ): Unit = {
    logger.info(
      "Try to register " + ipAddress + " | " + inputDirectory.mkString(
        ", "
      ) + " | " + outputDirectory + ", "
    )

    val request = RegisterRequest(
      ipAddress,
      inputDirectory,
      outputDirectory
    )
    try {
      val response = blockingStub.registerWorkerDirectory(request)
      logger.info("registering result: " + response.isRegistered)
    } catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
  }
}
