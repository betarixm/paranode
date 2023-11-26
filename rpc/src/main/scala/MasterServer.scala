package kr.ac.postech.paranode.rpc

import io.grpc.Server
import io.grpc.ServerBuilder
import kr.ac.postech.paranode.core.WorkerMetadata
import org.apache.logging.log4j.scala.Logging

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise

import master.{MasterGrpc, RegisterReply, RegisterRequest}

class MasterServer(executionContext: ExecutionContext, val port: Int = 50051)
    extends Logging { self =>
  private[this] val server: Server = ServerBuilder
    .forPort(port)
    .addService(MasterGrpc.bindService(new MasterImpl, executionContext))
    .build()

  private val workerDetails: ListBuffer[WorkerMetadata] = ListBuffer()

  def addWorkerInfo(workerMetadata: WorkerMetadata): Unit = synchronized {
    workerDetails += workerMetadata
  }

  def getWorkerDetails: List[WorkerMetadata] = workerDetails.toList

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
      self.stop()
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

  private class MasterImpl extends MasterGrpc.Master {
    override def register(request: RegisterRequest): Future[RegisterReply] = {
      val promise = Promise[RegisterReply]

      Future {
        logger.debug(s"[MasterServer] Register ($request)")

        val workerMetadata =
          WorkerMetadata(request.worker.get.host, request.worker.get.port, None)
        addWorkerInfo(workerMetadata)
      }(executionContext)

      promise.future
    }
  }
}
