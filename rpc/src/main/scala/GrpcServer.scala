package kr.ac.postech.paranode.rpc

import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.ServerServiceDefinition
import org.apache.logging.log4j.scala.Logging

import java.util.concurrent.TimeUnit

class GrpcServer[T <: ServerBuilder[T]](
    service: ServerServiceDefinition,
    port: Int
) extends Logging {
  private val server: Server =
    ServerBuilder
      .forPort(port)
      .addService(service)
      .asInstanceOf[ServerBuilder[T]]
      .build()

  def start(): Unit = {
    server.start()

    logger.info(
      "[GrpcServer] \n" +
        s"service: $service\n" +
        s"port: $port\n"
    )

    sys.addShutdownHook {
      logger.error(
        "[GrpcServer] shutting down gRPC server since JVM is shutting down"
      )
      stop()
      logger.error("[GrpcServer] server shut down")
    }
  }

  def stop(): Unit = {
    if (server != null) {
      server.shutdown().awaitTermination(5, TimeUnit.SECONDS)

    }
  }

  def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

}
