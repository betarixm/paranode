package kr.ac.postech.paranode.rpc
import io.grpc.Server
import io.grpc.ServerBuilder
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext

import worker._

class WorkerServer(
    executionContext: ExecutionContext,
    service: WorkerGrpc.Worker,
    port: Int
) extends Logging { self =>
  private[this] val server: Server = ServerBuilder
    .forPort(port)
    .addService(
      WorkerGrpc.bindService(
        service,
        executionContext
      )
    )
    .build()

  def start(): Unit = {
    server.start()

    logger.info(
      "[WorkerServer] \n" +
        s"port: $port\n"
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
