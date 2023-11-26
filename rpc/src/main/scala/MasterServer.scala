package kr.ac.postech.paranode.rpc

import io.grpc.Server
import io.grpc.ServerBuilder
import kr.ac.postech.paranode.core.WorkerMetadata
import kr.ac.postech.paranode.rpc.MasterServer.port

import java.util.logging.Logger
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.WrappedArray
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise
import master.{MasterGrpc, RegisterReply, RegisterRequest}

import org.apache.logging.log4j.scala.Logging

object MasterServer {
  private val port = 50051
}

class MasterServer(executionContext: ExecutionContext) extends Logging { self =>
  private[this] val server: Server = ServerBuilder
    .forPort(MasterServer.port)
    .addService(MasterGrpc.bindService(new MasterImpl, executionContext))
    .build()

  private val workerDetails: ListBuffer[WorkerMetadata] = ListBuffer()

  def addWorkerInfo(workerMetadata: WorkerMetadata): Unit = synchronized {
    workerDetails += workerMetadata
  }

  def getWorkerDetails: List[WorkerMetadata] = workerDetails.toList

  def getPort: String = port.toString

  private def start(): Unit = {
    server.start()

    logger.info(
      s"MasterServer listening on port $port"
    )

    sys.addShutdownHook {
      logger.error(
        "*** shutting down gRPC server since JVM is shutting down"
      )
      self.stop()
      logger.error("*** server shut down")
    }
  }

  def startServer(): Unit = this.start()
  def stopServer(): Unit = this.stop()

  private def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  private def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

  private class MasterImpl extends MasterGrpc.Master {
    override def register(request: RegisterRequest): Future[RegisterReply] = {
      val promise = Promise[RegisterReply]

      Future {
        logger.debug(s"Register request: $request")

        val workerMetadata =
          WorkerMetadata(request.worker.get.host, request.worker.get.port, None)
        addWorkerInfo(workerMetadata)
      }(executionContext)

      promise.future
    }
  }
}
