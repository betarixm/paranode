package kr.ac.postech.paranode.rpc

import io.grpc.Server
import io.grpc.ServerBuilder

import java.util.logging.Logger
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise

import master.{MasterGrpc, RegisterReply, RegisterRequest}

object MasterServer {
  private val logger = Logger.getLogger(classOf[MasterServer].getName)

  def main(args: Array[String]): Unit = {
    val server = new MasterServer(ExecutionContext.global)
    server.start()
    server.blockUntilShutdown()
  }

  private val port = 50051
}

class MasterServer(executionContext: ExecutionContext) { self =>
  private[this] val server: Server = ServerBuilder
    .forPort(MasterServer.port)
    .addService(MasterGrpc.bindService(new MasterImpl, executionContext))
    .build()

  private def start(): Unit = {
    server.start()

    MasterServer.logger.info(
      "Server started, listening on " + MasterServer.port
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

  private class MasterImpl extends MasterGrpc.Master {
    override def register(request: RegisterRequest): Future[RegisterReply] = {
      val promise = Promise[RegisterReply]

      Future {
        // TODO: Logic
        promise.success(new RegisterReply())
      }(executionContext)

      promise.future
    }
  }
}
