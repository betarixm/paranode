package kr.ac.postech.paranode.rpc
import io.grpc.Server
import io.grpc.ServerBuilder
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext
import scala.reflect.io.Directory

import worker._

class WorkerServer(
    executionContext: ExecutionContext,
    port: Int,
    inputDirectories: Array[Directory],
    outputDirectory: Directory
) extends Logging { self =>
  private[this] val server: Server = ServerBuilder
    .forPort(port)
    .addService(
      WorkerGrpc.bindService(
        new WorkerService(executionContext, inputDirectories, outputDirectory),
        executionContext
      )
    )
    .build()

  def start(): Unit = {
    server.start()

    logger.debug(
      "[WorkerServer] \n" +
        s"port: $port\n" +
        s"inputDirectories: ${inputDirectories.mkString(", ")}\n" +
        s"outputDirectory: $outputDirectory\n"
    )

    sys.addShutdownHook {
      logger.error(
        "[WorkerServer] shutting down gRPC server since JVM is shutting down"
      )
      self.stop()
      logger.error("[WorkerServer] server shut down")
    }
  }

  def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

}
