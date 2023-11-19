package kr.ac.postech.paranode.rpc

import io.grpc.Server
import io.grpc.ServerBuilder

import java.util.logging.Logger
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise

import worker._

object WorkerServer {
  private val logger = Logger.getLogger(classOf[WorkerServer].getName)

  def main(args: Array[String]): Unit = {
    val server = new WorkerServer(ExecutionContext.global)
    server.start()
    server.blockUntilShutdown()
  }

  private val port = 30040
}

class WorkerServer(executionContext: ExecutionContext) { self =>
  private[this] val server: Server = ServerBuilder
    .forPort(WorkerServer.port)
    .addService(WorkerGrpc.bindService(new WorkerImpl, executionContext))
    .build()

  private def start(): Unit = {
    server.start()

    WorkerServer.logger.info(
      "Server started, listening on " + WorkerServer.port
    )

    sys.addShutdownHook {
      System.err.println(
        "*** shutting down gRPC server since JVM is shutting down"
      )
      self.stop()
      System.err.println("*** server shut down")
    }
  }

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

  private class WorkerImpl extends WorkerGrpc.Worker {
    override def sample(request: SampleRequest): Future[SampleReply] = {
      val promise = Promise[SampleReply]

      Future {
        // TODO: Logic
        promise.success(new SampleReply())
      }(executionContext)

      promise.future
    }

    override def partition(
        request: PartitionRequest
    ): Future[PartitionReply] = {
      val promise = Promise[PartitionReply]

      Future {
        // TODO: Logic
        promise.success(new PartitionReply())
      }(executionContext)

      promise.future
    }

    override def exchange(request: ExchangeRequest): Future[ExchangeReply] = {
      val promise = Promise[ExchangeReply]

      Future {
        // TODO: Logic
        promise.success(new ExchangeReply())
      }(executionContext)

      promise.future
    }

    override def merge(request: MergeRequest): Future[MergeReply] = {
      val promise = Promise[MergeReply]

      Future {
        // TODO: Logic
        promise.success(new MergeReply())
      }(executionContext)

      promise.future
    }
  }

}
