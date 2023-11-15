package kr.ac.postech.paranode.rpc

import java.util.logging.Logger

import io.grpc.{Server, ServerBuilder}
import kr.ac.postech.paranode.rpc.worker.{
  WorkerGrpc,
  SampleRequest,
  SampleReply,
  PartitionRequest,
  PartitionReply,
  ExchangeRequest,
  ExchangeReply,
  MergeRequest,
  MergeReply
}

import scala.concurrent.{ExecutionContext, Future}

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
  private[this] var server: Server = null

  private def start(): Unit = {
    server = ServerBuilder
      .forPort(WorkerServer.port)
      .addService(WorkerGrpc.bindService(new WorkerImpl, executionContext))
      .build
      .start

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
    override def sampleKeys(request: SampleRequest) = {

      // TODO: Implement the logic to sample keys from the input directory.
      // val sampledKeys = blockOfWorker.sample(block).map(_.toString).toList
      val sampledKeys = List("0x1", "0x2", "0x3")
      // TODO: Implement the logic to check whether the sampled keys are nice

      val reply = SampleReply(sampledKeys, isNice = true)
      Future.successful(reply)
    }
    override def makePartitions(request: PartitionRequest) = {
      // TODO
      val reply = PartitionReply(isNice = true)
      Future.successful(reply)
    }
    override def exchangeWithOtherWorker(request: ExchangeRequest) = {
      // TODO
      val reply = ExchangeReply(isNice = true)
      Future.successful(reply)
    }
    override def merge(request: MergeRequest) = {
      // TODO
      val reply = MergeReply(isNice = true)
      Future.successful(reply)
    }

  }

}
