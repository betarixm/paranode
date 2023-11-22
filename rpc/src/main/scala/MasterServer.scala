package kr.ac.postech.paranode.rpc

import io.grpc.Server
import io.grpc.ServerBuilder

import java.io.File
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.util.logging.Logger
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise
import master.{MasterGrpc, RegisterReply, RegisterRequest}

import kr.ac.postech.paranode.rpc.MasterServer.port

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
  
  private var requestCount = 0

  def incrementRequestCount(): Unit = synchronized {
    requestCount += 1
  }

  def getRequestCount: Int = requestCount
  def getPort:String = port.toString

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
        try {
          val dirPath = Paths.get("worker_register")
          if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath)
          }
          val filePath =
            dirPath.resolve(request.worker.get.host + ".txt").toString

          val writer = new PrintWriter(new File(filePath), "UTF-8")
          try {
            writer.println(
              s"Worker Host: ${request.worker.get.host}, Worker Port: ${request.worker.get.port}"
            )
          } finally {
            self.incrementRequestCount()
            writer.close()
          }
          promise.success(new RegisterReply())
        } catch {
          case e: Exception =>
            MasterServer.logger.warning(
              "Failed to write to file: " + e.getMessage
            )
            promise.failure(e)
        }
      }(executionContext)

      promise.future
    }
  }
}
