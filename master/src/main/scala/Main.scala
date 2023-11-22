package kr.ac.postech.paranode.master

import kr.ac.postech.paranode.core.WorkerMetadata
import kr.ac.postech.paranode.master.AuxFunction.getWorkerDetails
import kr.ac.postech.paranode.rpc.MasterServer

import java.net.URL
import scala.io.Source
import scala.util.Try

object Main {
  def main(args: Array[String]): Unit = {
    val requestLimit = Try(args(0).toInt).getOrElse {
      println("Invalid command")
      return
    }

    val server = new MasterServer(scala.concurrent.ExecutionContext.global)
    server.startServer()

    while (server.getRequestCount < requestLimit) {
      Thread.sleep(1000)
    }

    val workerInfo: List[WorkerMetadata] = getWorkerDetails()

    assert(workerInfo.size == requestLimit)

    try {
      val url = new URL("http://checkip.amazonaws.com")
      val source = Source.fromURL(url)
      val publicIpAddress = source.mkString.trim
      source.close()

      println(publicIpAddress + ":" + server.getPort)
      println(workerInfo.map(_.host).mkString(", "))
    } catch {
      case e: Exception => e.printStackTrace()
    }
    // TODO: start WorkerClient

  }
}
