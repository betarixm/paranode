package kr.ac.postech.paranode.rpc

import java.util.logging.Logger

import io.grpc.{Server, ServerBuilder}
import kr.ac.postech.paranode.rpc.exchange.{
  ExchangeGrpc,
  GetMyRecordsRequest,
  GetMyRecordsReply
}

import scala.concurrent.{ExecutionContext, Future}

object ExchangeServer {
  private val logger = Logger.getLogger(classOf[ExchangeServer].getName)

  def main(args: Array[String]): Unit = {
    val server = new ExchangeServer(ExecutionContext.global)
    server.start()
    server.blockUntilShutdown()
  }

  private val port = 30050
}

class ExchangeServer(executionContext: ExecutionContext) { self =>
  private[this] var server: Server = null

  private def start(): Unit = {
    server = ServerBuilder
      .forPort(ExchangeServer.port)
      .addService(ExchangeGrpc.bindService(new ExchangeImpl, executionContext))
      .build
      .start

    ExchangeServer.logger.info(
      "Server started, listening on " + ExchangeServer.port
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

  private class ExchangeImpl extends ExchangeGrpc.Exchange {
    override def saveRecords(request: GetMyRecordsRequest) = {
      // TODO
      val reply = GetMyRecordsReply(isNice = true)
      Future.successful(reply)
    }
  }

}
