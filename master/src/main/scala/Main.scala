package kr.ac.postech.paranode.master

import kr.ac.postech.paranode.core.WorkerMetadata
import kr.ac.postech.paranode.rpc.MasterServer

import java.net._
import scala.util.Try

object Main {
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
    // TODO: save workerInfo and start WorkerClient

  }

}
