package kr.ac.postech.paranode.rpc

import java.util.logging.Logger

import io.grpc.Server
import io.grpc.ServerBuilder
import kr.ac.postech.paranode.rpc.master.MasterGrpc
import kr.ac.postech.paranode.rpc.master.RegisterRequest
import kr.ac.postech.paranode.rpc.master.RegisterReply


import scala.concurrent.{ExecutionContext, Future}

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
  private[this] var server: Server = null

  private def start(): Unit = {
    server = ServerBuilder
      .forPort(MasterServer.port)
      .addService(MasterGrpc.bindService(new MasterImpl, executionContext))
      .build
      .start
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
    override def registerWorkerDirectory(request: RegisterRequest): Future[RegisterReply] = {
      System.err.println("*** server side code working")
      System.err.println(
        s"*** Received registration request: ipAddress = ${request.ipAddress}"
      )
      System.err.println(
        s"*** Input Directories: ${request.inputDirectory.mkString(", ")}"
      )
      System.err.println(s"*** Output Directory: ${request.outputDirectory}")

      val reply = RegisterReply(isRegistered = true)
      Future.successful(reply)
    }
  }

}
