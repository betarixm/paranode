package kr.ac.postech.paranode.master

import kr.ac.postech.paranode.core.Key
import kr.ac.postech.paranode.core.WorkerMetadata
import kr.ac.postech.paranode.rpc.MasterServer
import kr.ac.postech.paranode.rpc.WorkerClient
import org.apache.logging.log4j.scala.Logging

import java.net._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

object Master extends Logging {
  def main(args: Array[String]): Unit = {
    val numberOfWorker = Try(args(0).toInt).getOrElse {
      println("Invalid command")
      return
    }

    val server = new MasterServer(scala.concurrent.ExecutionContext.global)
    server.startServer()

    while (server.getWorkerDetails.size < numberOfWorker) {
      Thread.sleep(1000)
    }

    val workerInfo: List[WorkerMetadata] = server.getWorkerDetails

    assert(workerInfo.size == numberOfWorker)

    try {
      val ipAddress = InetAddress.getLocalHost.getHostAddress

      println(ipAddress + ":" + server.getPort)
      println(workerInfo.map(_.host).mkString(", "))
    } catch {
      case e: Exception => e.printStackTrace()
    }

    val clients = workerInfo.map { worker =>
      WorkerClient(worker.host, worker.port)
    }

    val sampledKeys = Await
      .result(
        Future.sequence(clients.map(_.sample(64))),
        scala.concurrent.duration.Duration.Inf
      )
      .flatMap(_.sampledKeys)
      .map(Key.fromByteString)

    logger.debug(s"Sampled keys: $sampledKeys")
  }

}
