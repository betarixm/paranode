package kr.ac.postech.paranode.rpc

import io.grpc.Server
import io.grpc.ServerBuilder
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext

import master.MasterGrpc

class MasterServer(
    executionContext: ExecutionContext,
    service: MasterGrpc.Master,
    port: Int = 50051
) extends Logging {

  val server: Server =
    ServerBuilder
      .forPort(port)
      .addService(
        MasterGrpc
          .bindService(
            service,
            executionContext
          )
      )
      .build()

  def start(): Unit = {
    server.start()

    logger.info(
      "[MasterServer] \n" +
        s"port: $port\n"
    )

    sys.addShutdownHook {
      logger.error(
        "[MasterServer] shutting down gRPC server since JVM is shutting down"
      )
      stop()
      logger.error("[MasterServer] server shut down")
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
