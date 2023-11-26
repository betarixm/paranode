package kr.ac.postech.paranode.rpc

import io.grpc.Server
import io.grpc.ServerBuilder
import kr.ac.postech.paranode.core.WorkerMetadata
import kr.ac.postech.paranode.utils.MutableState
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext

import master.MasterGrpc

class MasterServer(executionContext: ExecutionContext, port: Int = 50051)
    extends Logging {

  private[this] val workers: MutableState[List[WorkerMetadata]] =
    new MutableState(List.empty)

  val server: Server =
    ServerBuilder
      .forPort(port)
      .addService(
        MasterGrpc
          .bindService(
            new MasterService(executionContext, workers),
            executionContext
          )
      )
      .build()

  def start(): Unit = {
    server.start()

    logger.debug(
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

  def registeredWorkers: List[WorkerMetadata] = workers.get
}
